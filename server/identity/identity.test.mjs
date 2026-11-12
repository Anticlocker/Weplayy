import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { createIdentityService, IdentityError } from "./service.mjs";
import { handleIdentityRequest } from "./routes.mjs";

async function setup() {
  const db = await openTestDatabase("identity");
  await clearData(db);
  const service = createIdentityService(db);
  return { db, service };
}

const SIGNUP = { username: "sundram", password: "password123", displayName: "Sundram" };

test("signup creates the account, settings row, and a working session", async () => {
  const { db, service } = await setup();
  const { user, accessToken } = await service.signUp(SIGNUP);

  assert.equal(user.username, "sundram");
  assert.equal(user.displayName, "Sundram");
  assert.equal(user.hasPassword, true);
  assert.ok(user.id.startsWith("WP-"));
  assert.ok(accessToken.length > 20);
  assert.equal(await service.resolveToken(accessToken), user.id);
  assert.equal((await service.getSettings(user.id)).dmPermission, true);
  assert.ok(await db.get("SELECT 1 FROM user_settings WHERE user_id = $1", [user.id]));
});

test("signup rejects duplicate usernames and bad input", async () => {
  const { service } = await setup();
  await service.signUp(SIGNUP);
  await assert.rejects(() => service.signUp(SIGNUP), (error) => {
    assert.ok(error instanceof IdentityError);
    assert.equal(error.code, "username_taken");
    assert.equal(error.status, 409);
    return true;
  });
  await assert.rejects(() => service.signUp({ ...SIGNUP, username: "Bad Name" }), { code: "invalid_username" });
  await assert.rejects(() => service.signUp({ ...SIGNUP, password: "short" }), { code: "invalid_password" });
});

test("signin verifies the password and rejects wrong ones", async () => {
  const { service } = await setup();
  const { user } = await service.signUp(SIGNUP);

  const ok = await service.signIn({ username: "sundram", password: "password123" });
  assert.equal(ok.user.id, user.id);
  assert.ok(ok.accessToken.length > 20);

  await assert.rejects(() => service.signIn({ username: "sundram", password: "wrong-password" }), {
    code: "invalid_credentials",
    status: 401,
  });
  await assert.rejects(() => service.signIn({ username: "nobody", password: "password123" }), {
    code: "invalid_credentials",
  });
});

test("signout revokes the session token", async () => {
  const { service } = await setup();
  const { user, accessToken } = await service.signUp(SIGNUP);
  assert.equal(await service.resolveToken(accessToken), user.id);

  await service.signOut(user.id, accessToken);
  assert.equal(await service.resolveToken(accessToken), null);
});

test("profile updates respect the whitelist and the username cooldown", async () => {
  const { service } = await setup();
  const { user } = await service.signUp(SIGNUP);

  const updated = await service.updateProfile(user.id, {
    displayName: "Sundram Y",
    bio: "builder",
    gender: "male",
    passwordHash: "attack", // not a real column path; must be ignored
  });
  assert.equal(updated.displayName, "Sundram Y");
  assert.equal(updated.bio, "builder");
  assert.equal(updated.gender, "male");

  const renamed = await service.updateProfile(user.id, { username: "sundramy" });
  assert.equal(renamed.username, "sundramy");

  await assert.rejects(
    () => service.updateProfile(user.id, { username: "sundramz" }),
    (error) => {
      assert.ok(error instanceof IdentityError);
      assert.equal(error.code, "username_cooldown");
      return true;
    },
  );
});

test("settings update validates keys and values", async () => {
  const { service } = await setup();
  const { user } = await service.signUp(SIGNUP);

  const settings = await service.updateSettings(user.id, {
    privateAccount: true,
    dndEnabled: true,
    dndStartMinute: 1380,
    dndEndMinute: 420,
  });
  assert.equal(settings.privateAccount, true);
  assert.equal(settings.dndStartMinute, 1380);

  await assert.rejects(() => service.updateSettings(user.id, { theme: "dark" }), { code: "invalid_setting" });
  await assert.rejects(() => service.updateSettings(user.id, { dndStartMinute: 9999 }), { code: "invalid_setting" });
  await assert.rejects(() => service.updateSettings(user.id, { privateAccount: "yes" }), { code: "invalid_setting" });
});

test("dispatcher routes with and without auth", async () => {
  const { service } = await setup();

  const signup = await handleIdentityRequest({ service, method: "POST", path: "/identity/signup", body: SIGNUP });
  assert.equal(signup.status, 201);
  const { user, accessToken } = signup.body;

  const denied = await handleIdentityRequest({ service, method: "GET", path: "/identity/me", userId: null });
  assert.equal(denied.status, 401);

  const me = await handleIdentityRequest({ service, method: "GET", path: "/identity/me", userId: user.id });
  assert.equal(me.status, 200);
  assert.equal(me.body.user.id, user.id);

  const out = await handleIdentityRequest({
    service, method: "POST", path: "/identity/signout", userId: user.id, body: { token: accessToken },
  });
  assert.equal(out.status, 200);

  const unknown = await handleIdentityRequest({ service, method: "GET", path: "/identity/nope", userId: user.id });
  assert.equal(unknown.status, 404);
});
