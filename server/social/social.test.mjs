import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { createIdentityService } from "../identity/service.mjs";
import { createSocialService, SocialError } from "./service.mjs";
import { handleSocialRequest } from "./routes.mjs";

async function setup() {
  const db = await openTestDatabase("social");
  await clearData(db);
  const identity = createIdentityService(db);
  const social = createSocialService(db);
  const signup = (username) => identity.signUp({ username, password: "password123", displayName: username });
  return { db, identity, social, signup };
}

test("a reciprocal pending request makes both users friends", async () => {
  const { db, social, signup } = await setup();
  const a = (await signup("ayesha")).user.id;
  const b = (await signup("bilal")).user.id;

  const sent = await social.sendRequest(a, b);
  assert.equal(sent.status, "pending");

  const received = await social.sendRequest(b, a);
  assert.equal(received.status, "accepted");

  const friendsOfA = (await social.listFriends(a)).items.map((f) => f.id);
  assert.deepEqual(friendsOfA, [b]);
  assert.ok((await social.listFriends(b)).items.some((f) => f.id === a));

  // Both sides were notified about the acceptance.
  const notes = await db.all("SELECT kind FROM notifications WHERE user_id IN ($1, $2)", [a, b]);
  assert.ok(notes.some((n) => n.kind === "friend_accepted"));
});

test("request, accept, and friend list flow", async () => {
  const { db, social, signup } = await setup();
  const a = (await signup("aa1")).user.id;
  const b = (await signup("bb1")).user.id;

  await social.sendRequest(a, b);
  assert.ok((await social.listRequests(b)).incoming.some((u) => u.id === a));
  assert.ok((await social.listRequests(a)).outgoing.some((u) => u.id === b));

  await social.acceptRequest(b, a);
  assert.equal((await social.listFriends(a)).items.length, 1);

  // The pending request notification landed for the recipient.
  const notes = await db.all("SELECT kind FROM notifications WHERE user_id = $1", [b]);
  assert.ok(notes.some((n) => n.kind === "friend_request"));
});

test("decline leaves the request declined and users unfriended", async () => {
  const { social, signup } = await setup();
  const a = (await signup("aa2")).user.id;
  const b = (await signup("bb2")).user.id;

  await social.sendRequest(a, b);
  const declined = await social.declineRequest(b, a);
  assert.equal(declined.status, "declined");
  assert.equal((await social.listFriends(a)).items.length, 0);
  assert.equal((await social.listRequests(b)).incoming.length, 0);
});

test("duplicate and self requests are rejected", async () => {
  const { social, signup } = await setup();
  const a = (await signup("aa3")).user.id;
  const b = (await signup("bb3")).user.id;

  await social.sendRequest(a, b);
  await assert.rejects(() => social.sendRequest(a, b), (error) => {
    assert.ok(error instanceof SocialError);
    assert.equal(error.code, "request_exists");
    return true;
  });
  await assert.rejects(() => social.sendRequest(a, a), { code: "cannot_add_self" });
});

test("blocking removes friendships and requests both ways", async () => {
  const { social, signup } = await setup();
  const a = (await signup("aa4")).user.id;
  const b = (await signup("bb4")).user.id;

  await social.sendRequest(a, b);
  await social.acceptRequest(b, a);
  assert.equal((await social.listFriends(a)).items.length, 1);

  await social.block(a, b);
  assert.equal((await social.listFriends(a)).items.length, 0);
  assert.equal((await social.listFriends(b)).items.length, 0);
  assert.ok((await social.listBlocks(a)).items.some((u) => u.id === b));

  await assert.rejects(() => social.sendRequest(b, a), (error) => {
    assert.equal(error.code, "blocked");
    assert.equal(error.status, 403);
    return true;
  });

  await social.unblock(a, b);
  assert.equal((await social.listBlocks(a)).items.length, 0);
});

test("reports store the reporter, target, and reason", async () => {
  const { db, social, signup } = await setup();
  const a = (await signup("aa5")).user.id;
  const b = (await signup("bb5")).user.id;

  await social.report(a, b, "spam messages");
  const row = await db.get("SELECT reporter_id, target_id, reason FROM reports");
  assert.equal(row.reporter_id, a);
  assert.equal(row.target_id, b);
  assert.equal(row.reason, "spam messages");

  await assert.rejects(() => social.report(a, b, ""), { code: "invalid_reason" });
});

test("dispatcher requires auth and routes cleanly", async () => {
  const { social, signup } = await setup();
  const a = (await signup("aa6")).user;

  const denied = await handleSocialRequest({ service: social, method: "GET", path: "/social/friends", userId: null });
  assert.equal(denied.status, 401);

  const ok = await handleSocialRequest({ service: social, method: "GET", path: "/social/friends", userId: a.id });
  assert.equal(ok.status, 200);
  assert.deepEqual(ok.body.items, []);

  const unknown = await handleSocialRequest({ service: social, method: "GET", path: "/social/nope", userId: a.id });
  assert.equal(unknown.status, 404);
});
