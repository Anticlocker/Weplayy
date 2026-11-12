// Support: bug reports filed from the app.
//
// Screenshots ride in object storage; only the URL is stored here. Reports go
// to the team, so there is deliberately no endpoint to read other users'
// tickets.

import { randomUUID } from "node:crypto";

const CATEGORY_MAX = 40;
const MESSAGE_MAX = 4000;
const URL_MAX = 500;
const MAX_PAGE = 100;

export class SupportError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createSupportService(db) {
  async function create(userId, { category, message, imageUrl } = {}) {
    assertUser(userId, "userId");
    if (typeof category !== "string" || category.trim().length === 0 || category.length > CATEGORY_MAX) {
      throw new SupportError("invalid_category", `category must be 1-${CATEGORY_MAX} characters.`);
    }
    if (typeof message !== "string" || message.trim().length === 0 || message.length > MESSAGE_MAX) {
      throw new SupportError("invalid_message", `message must be 1-${MESSAGE_MAX} characters.`);
    }
    if (imageUrl !== undefined && imageUrl !== null) {
      if (typeof imageUrl !== "string" || imageUrl.length > URL_MAX) {
        throw new SupportError("invalid_image", "imageUrl must be a URL string or null.");
      }
    }

    const id = `sup_${randomUUID()}`;
    await db.run(
      "INSERT INTO support_tickets (id, user_id, category, message, image_url, created_at) VALUES ($1, $2, $3, $4, $5, $6)",
      [id, userId, category.trim(), message, imageUrl ?? null, Date.now()],
    );
    return toDto(await db.get("SELECT * FROM support_tickets WHERE id = $1", [id]));
  }

  async function listMine(userId, { limit } = {}) {
    assertUser(userId, "userId");
    const parsed = Number(limit);
    const take = Number.isInteger(parsed) && parsed >= 1 ? Math.min(parsed, MAX_PAGE) : 20;
    const rows = await db.all(
      "SELECT * FROM support_tickets WHERE user_id = $1 ORDER BY created_at DESC, seq DESC LIMIT $2",
      [userId, take],
    );
    return { items: rows.map(toDto) };
  }

  return { create, listMine };
}

function toDto(row) {
  return {
    id: row.id,
    category: row.category,
    message: row.message,
    imageUrl: row.image_url,
    createdAt: row.created_at,
  };
}

function assertUser(value, field) {
  if (typeof value !== "string" || value.length === 0 || value.length > 64) {
    throw new SupportError("invalid_user", `${field} is not a valid user id.`);
  }
}
