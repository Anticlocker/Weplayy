import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { createNotificationsService } from "./service.mjs";
import { handleNotificationsRequest } from "./routes.mjs";

async function setup() {
  const db = await openTestDatabase("notifications");
  await clearData(db);
  const service = createNotificationsService(db);
  const user = "WP-tester1";
  await db.run(
    "INSERT INTO users (id, username, display_name, created_at, updated_at) VALUES ($1, $2, $3, $4, $5)",
    [user, "tester1", "Tester", Date.now(), Date.now()],
  );

  const insert = db.run.bind(
    db,
    "INSERT INTO notifications (id, user_id, kind, actor_id, body, read, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)",
  );
  // Distinct timestamps so ordering is deterministic.
  for (let i = 1; i <= 5; i += 1) {
    await insert(`n${i}`, user, "message", "WP-actor01", `note ${i}`, i === 5, 1000 * i);
  }
  return { db, service, user };
}

test("list returns newest first and paginates with a cursor", async () => {
  const { service, user } = await setup();

  const page1 = await service.list(user, { limit: 2 });
  assert.equal(page1.items.length, 2);
  assert.equal(page1.items[0].id, "n5");
  assert.equal(page1.items[0].read, true);
  assert.ok(page1.nextCursor);

  const page2 = await service.list(user, { limit: 2, cursor: page1.nextCursor });
  assert.equal(page2.items[0].id, "n3");

  const page3 = await service.list(user, { limit: 10, cursor: page2.nextCursor });
  assert.equal(page3.items.length, 1);
  assert.equal(page3.nextCursor, null);
});

test("unreadOnly filters read rows out", async () => {
  const { service, user } = await setup();
  const unread = await service.list(user, { unreadOnly: true });
  assert.deepEqual(
    unread.items.map((n) => n.id),
    ["n4", "n3", "n2", "n1"],
  );
});

test("markRead works per row, markAllRead sweeps the rest", async () => {
  const { service, user } = await setup();

  await service.markRead(user, "n1");
  let unread = await service.list(user, { unreadOnly: true });
  assert.deepEqual(
    unread.items.map((n) => n.id),
    ["n4", "n3", "n2"],
  );

  const swept = await service.markAllRead(user);
  assert.equal(swept.updated, 3);
  unread = await service.list(user, { unreadOnly: true });
  assert.equal(unread.items.length, 0);
});

test("a user cannot mark someone else's notification", async () => {
  const { service } = await setup();
  await assert.rejects(() => service.markRead("WP-other001", "n1"), (error) => {
    assert.equal(error.code, "notification_not_found");
    assert.equal(error.status, 404);
    return true;
  });
});

test("dispatcher requires auth and handles routing", async () => {
  const { service } = await setup();

  const denied = await handleNotificationsRequest({ service, method: "GET", path: "/notifications", userId: null });
  assert.equal(denied.status, 401);

  const ok = await handleNotificationsRequest({
    service,
    method: "GET",
    path: "/notifications",
    userId: "WP-tester1",
    query: { unreadOnly: "1" },
  });
  assert.equal(ok.status, 200);
  assert.equal(ok.body.items.length, 4);

  const marked = await handleNotificationsRequest({
    service,
    method: "POST",
    path: "/notifications/read",
    userId: "WP-tester1",
    body: { id: "n1" },
  });
  assert.equal(marked.status, 200);

  const unknown = await handleNotificationsRequest({
    service,
    method: "GET",
    path: "/notifications/nope",
    userId: "WP-tester1",
  });
  assert.equal(unknown.status, 404);
});
