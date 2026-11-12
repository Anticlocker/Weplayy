// Identity: accounts, password sign-in, device sessions, profile, settings.
//
// Passwords are hashed with scrypt (random 16-byte salt, 64-byte key) and are
// never stored or logged in any other form. Device sessions hold only the
// SHA-256 of the issued bearer token, so a database leak yields no usable
// tokens. The wallet balance deliberately lives in the economy module; a user
// with no wallet row simply has zero coins.

import { createHash, randomBytes, randomUUID, scrypt as scryptCb, timingSafeEqual } from "node:crypto";
import { promisify } from "node:util";

const scrypt = promisify(scryptCb);

const USERNAME = /^[a-z0-9_]{3,20}$/;
const PASSWORD_MIN = 8;
const NAME_MAX = 50;
const BIO_MAX = 280;
const USERNAME_CHANGE_COOLDOWN_MS = 30 * 24 * 60 * 60 * 1000;
const ID_ATTEMPTS = 5;

const BOOLEAN_SETTINGS = [
  "dmPermission",
  "invitePermission",
  "showOnline",
  "showGifts",
  "privateAccount",
  "dndEnabled",
  "notifyInvites",
  "notifyMessages",
  "notifyGifts",
  "notifyFollowers",
  "notifyAnnouncements",
];
const MINUTE_SETTINGS = ["dndStartMinute", "dndEndMinute"];

export class IdentityError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createIdentityService(db) {
  // --- accounts -----------------------------------------------------------

  async function signUp({ username, password, displayName }) {
    assertUsername(username);
    assertPassword(password);
    assertDisplayName(displayName);
    if (await db.get("SELECT 1 FROM users WHERE username = $1", [username])) {
      throw new IdentityError("username_taken", "That username is already in use.", 409);
    }

    const hash = await hashPassword(password);
    const now = Date.now();
    const id = await insertUserWithRetry(username, displayName.trim(), hash, now);
    await db.run("INSERT INTO user_settings (user_id) VALUES ($1)", [id]);

    const accessToken = await issueSession(id, "username", username, now);
    return { user: toUserDto(await getUser(id)), accessToken };
  }

  async function signIn({ username, password }) {
    if (typeof username !== "string" || typeof password !== "string") {
      throw new IdentityError("invalid_credentials", "Username or password is incorrect.", 401);
    }
    const row = await db.get("SELECT * FROM users WHERE username = $1", [username.trim().toLowerCase()]);
    if (!row || !row.password_hash || !(await verifyPassword(password, row.password_hash))) {
      throw new IdentityError("invalid_credentials", "Username or password is incorrect.", 401);
    }

    const accessToken = await issueSession(row.id, row.provider, row.username, Date.now());
    return { user: toUserDto(row), accessToken };
  }

  async function signOut(userId, token) {
    assertUser(userId, "userId");
    if (typeof token !== "string" || token.length === 0) {
      throw new IdentityError("invalid_token", "A bearer token is required.");
    }
    await db.run(
      "UPDATE device_sessions SET revoked_at = $1 WHERE token_hash = $2 AND user_id = $3 AND revoked_at IS NULL",
      [Date.now(), tokenHash(token), userId],
    );
    return { ok: true };
  }

  async function resolveToken(token) {
    if (typeof token !== "string" || token.length === 0) return null;
    const row = await db.get(
      "SELECT user_id FROM device_sessions WHERE token_hash = $1 AND revoked_at IS NULL",
      [tokenHash(token)],
    );
    return row?.user_id ?? null;
  }

  // --- profile ------------------------------------------------------------

  async function getProfile(userId) {
    assertUser(userId, "userId");
    const row = await getUser(userId);
    if (!row) throw new IdentityError("user_not_found", "Unknown user.", 404);
    return toUserDto(row);
  }

  async function updateProfile(userId, patch) {
    assertUser(userId, "userId");
    const current = await getUser(userId);
    if (!current) throw new IdentityError("user_not_found", "Unknown user.", 404);

    let n = 0;
    const next = () => `$${++n}`;
    const sets = [];
    const values = [];

    if (patch.username !== undefined && patch.username !== current.username) {
      assertUsername(patch.username);
      if (await db.get("SELECT 1 FROM users WHERE username = $1 AND id <> $2", [patch.username, userId])) {
        throw new IdentityError("username_taken", "That username is already in use.", 409);
      }
      if (current.username_changed_at && Date.now() - current.username_changed_at < USERNAME_CHANGE_COOLDOWN_MS) {
        throw new IdentityError("username_cooldown", "Username can be changed once every 30 days.", 409);
      }
      sets.push(`username = ${next()}`, `username_changed_at = ${next()}`);
      values.push(patch.username, Date.now());
    }
    if (patch.displayName !== undefined) {
      assertDisplayName(patch.displayName);
      sets.push(`display_name = ${next()}`);
      values.push(patch.displayName.trim());
    }
    if (patch.bio !== undefined) {
      if (typeof patch.bio !== "string" || patch.bio.length > BIO_MAX) {
        throw new IdentityError("invalid_bio", `bio must be at most ${BIO_MAX} characters.`);
      }
      sets.push(`bio = ${next()}`);
      values.push(patch.bio);
    }
    if (patch.gender !== undefined) {
      if (typeof patch.gender !== "string" || patch.gender.length > 20) {
        throw new IdentityError("invalid_gender", "gender must be a short string.");
      }
      sets.push(`gender = ${next()}`);
      values.push(patch.gender);
    }
    if (patch.dob !== undefined) {
      if (!Number.isInteger(patch.dob) || patch.dob <= 0) {
        throw new IdentityError("invalid_dob", "dob must be an epoch-millisecond integer.");
      }
      sets.push(`dob = ${next()}`);
      values.push(patch.dob);
    }
    if (patch.avatarUrl !== undefined) {
      if (patch.avatarUrl !== null && (typeof patch.avatarUrl !== "string" || patch.avatarUrl.length > 500)) {
        throw new IdentityError("invalid_avatar", "avatarUrl must be a URL string or null.");
      }
      sets.push(`avatar_url = ${next()}`);
      values.push(patch.avatarUrl);
    }

    if (sets.length > 0) {
      sets.push(`updated_at = ${next()}`);
      values.push(Date.now());
      values.push(userId);
      await db.run(`UPDATE users SET ${sets.join(", ")} WHERE id = ${next()}`, values);
    }
    return toUserDto(await getUser(userId));
  }

  // --- settings -----------------------------------------------------------

  async function getSettings(userId) {
    assertUser(userId, "userId");
    const row = await db.get("SELECT * FROM user_settings WHERE user_id = $1", [userId]);
    return toSettingsDto(row ?? { user_id: userId });
  }

  async function updateSettings(userId, patch) {
    assertUser(userId, "userId");
    if (!(await db.get("SELECT 1 FROM user_settings WHERE user_id = $1", [userId]))) {
      await db.run("INSERT INTO user_settings (user_id) VALUES ($1)", [userId]);
    }

    for (const key of Object.keys(patch)) {
      if (!BOOLEAN_SETTINGS.includes(key) && !MINUTE_SETTINGS.includes(key)) {
        throw new IdentityError("invalid_setting", "Unknown setting key.");
      }
    }

    let n = 0;
    const next = () => `$${++n}`;
    const sets = [];
    const values = [];
    for (const key of BOOLEAN_SETTINGS) {
      if (patch[key] === undefined) continue;
      if (typeof patch[key] !== "boolean") {
        throw new IdentityError("invalid_setting", `${key} must be true or false.`);
      }
      sets.push(`${snake(key)} = ${next()}`);
      values.push(patch[key]);
    }
    for (const key of MINUTE_SETTINGS) {
      if (patch[key] === undefined) continue;
      if (!Number.isInteger(patch[key]) || patch[key] < 0 || patch[key] > 1439) {
        throw new IdentityError("invalid_setting", `${key} must be a minute of the day (0-1439).`);
      }
      sets.push(`${snake(key)} = ${next()}`);
      values.push(patch[key]);
    }

    if (sets.length > 0) {
      values.push(userId);
      await db.run(`UPDATE user_settings SET ${sets.join(", ")} WHERE user_id = ${next()}`, values);
    }
    return toSettingsDto(await db.get("SELECT * FROM user_settings WHERE user_id = $1", [userId]));
  }

  // --- internals ----------------------------------------------------------

  async function getUser(userId) {
    return db.get("SELECT * FROM users WHERE id = $1", [userId]);
  }

  async function insertUserWithRetry(username, displayName, hash, now) {
    let lastError;
    for (let attempt = 0; attempt < ID_ATTEMPTS; attempt += 1) {
      const id = newUserId();
      try {
        await db.run(
          `INSERT INTO users (id, username, display_name, password_hash, has_password, provider, created_at, updated_at)
           VALUES ($1, $2, $3, $4, TRUE, $5, $6, $7)`,
          [id, username, displayName, hash, "username", now, now],
        );
        return id;
      } catch (error) {
        lastError = error;
      }
    }
    throw lastError;
  }

  async function issueSession(userId, provider, identifier, now) {
    const token = randomBytes(32).toString("base64url");
    await db.run(
      `INSERT INTO device_sessions (id, user_id, token_hash, provider, identifier, created_at, last_seen_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [`sess_${randomUUID()}`, userId, tokenHash(token), provider, identifier, now, now],
    );
    return token;
  }

  return {
    signUp,
    signIn,
    signOut,
    resolveToken,
    getProfile,
    updateProfile,
    getSettings,
    updateSettings,
  };
}

async function hashPassword(password) {
  const salt = randomBytes(16);
  const derived = await scrypt(password, salt, 64);
  return `scrypt$${salt.toString("base64")}$${derived.toString("base64")}`;
}

async function verifyPassword(password, stored) {
  const parts = String(stored).split("$");
  if (parts.length !== 3 || parts[0] !== "scrypt") return false;
  const salt = Buffer.from(parts[1], "base64");
  const expected = Buffer.from(parts[2], "base64");
  const derived = await scrypt(password, salt, expected.length);
  return derived.length === expected.length && timingSafeEqual(derived, expected);
}

function tokenHash(token) {
  return createHash("sha256").update(token).digest("hex");
}

function newUserId() {
  // "WP-" plus 6 base-36 characters keeps the product id look while giving a
  // large enough space that insert collisions are rare and retryable.
  const chars = Array.from(randomBytes(6), (byte) => (byte % 36).toString(36)).join("");
  return `WP-${chars}`;
}

function toUserDto(row) {
  return {
    id: row.id,
    username: row.username,
    displayName: row.display_name,
    avatarUrl: row.avatar_url,
    provider: row.provider,
    phone: row.phone,
    email: row.email,
    phoneVerified: !!row.phone_verified,
    emailVerified: !!row.email_verified,
    level: row.level,
    bio: row.bio,
    gender: row.gender,
    dob: row.dob,
    hasPassword: !!row.password_hash,
    usernameChangedAt: row.username_changed_at,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function toSettingsDto(row) {
  const dto = { userId: row.user_id };
  for (const key of BOOLEAN_SETTINGS) dto[key] = row[snake(key)] ?? true;
  for (const key of MINUTE_SETTINGS) dto[key] = row[snake(key)] ?? 0;
  return dto;
}

function assertUser(value, field) {
  if (typeof value !== "string" || value.length === 0 || value.length > 64) {
    throw new IdentityError("invalid_user", `${field} is not a valid user id.`);
  }
}

function assertUsername(value) {
  if (typeof value !== "string" || !USERNAME.test(value)) {
    throw new IdentityError("invalid_username", "username must be 3-20 chars of a-z, 0-9, _.");
  }
}

function assertPassword(value) {
  if (typeof value !== "string" || value.length < PASSWORD_MIN) {
    throw new IdentityError("invalid_password", `password must be at least ${PASSWORD_MIN} characters.`);
  }
}

function assertDisplayName(value) {
  if (typeof value !== "string" || value.trim().length === 0 || value.trim().length > NAME_MAX) {
    throw new IdentityError("invalid_display_name", `displayName must be 1-${NAME_MAX} characters.`);
  }
}

function snake(key) {
  return key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
}
