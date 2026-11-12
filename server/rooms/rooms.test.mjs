import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { createIdentityService } from "../identity/service.mjs";
import { createRoomsService, RoomsError } from "./service.mjs";
import { handleRoomsRequest } from "./routes.mjs";

async function setup() {
  const db = await openTestDatabase("rooms");
  await clearData(db);
  const identity = createIdentityService(db);
  const rooms = createRoomsService(db);
  const a = (await identity.signUp({ username: "host1", password: "password123", displayName: "Host" })).user.id;
  const b = (await identity.signUp({ username: "guest1", password: "password123", displayName: "Guest" })).user.id;
  return { db, rooms, a, b };
}

test("create a room with defaults and explicit capacity", async () => {
  const { rooms, a } = await setup();

  const room = await rooms.create(a, { title: "Late night chill" });
  assert.ok(room.id.startsWith("room_"));
  assert.equal(room.title, "Late night chill");
  assert.equal(room.maxParticipants, 24);
  assert.equal(room.isPrivate, false);
  assert.equal(room.host.id, a);
  assert.equal(room.closedAt, null);

  const small = await rooms.create(a, { title: "Clean", tag: "music", isPrivate: true, maxParticipants: 8 });
  assert.equal(small.maxParticipants, 8);
  assert.equal(small.isPrivate, true);
});

test("create validates title and capacity", async () => {
  const { rooms, a } = await setup();

  await assert.rejects(() => rooms.create(a, { title: "" }), { code: "invalid_title" });
  await assert.rejects(() => rooms.create(a, { title: "ok", maxParticipants: 99 }), { code: "invalid_capacity" });
  await assert.rejects(() => rooms.create(a, { title: "ok", tag: "x".repeat(25) }), { code: "invalid_tag" });
});

test("listOpen shows open rooms and hides closed ones", async () => {
  const { rooms, a, b } = await setup();

  const first = await rooms.create(a, { title: "Room 1" });
  await rooms.create(b, { title: "Room 2" });

  let open = (await rooms.listOpen()).items;
  assert.equal(open.length, 2);
  assert.equal(open[0].title, "Room 2");
  assert.equal(open[1].host.id, a);

  await rooms.close(first.id, a);
  open = (await rooms.listOpen()).items;
  assert.equal(open.length, 1);
  assert.equal(open[0].title, "Room 2");
});

test("only the host can close, and closing twice is a replay", async () => {
  const { rooms, a, b } = await setup();
  const room = await rooms.create(a, { title: "Mine" });

  await assert.rejects(() => rooms.close(room.id, b), (error) => {
    assert.ok(error instanceof RoomsError);
    assert.equal(error.code, "not_allowed");
    assert.equal(error.status, 403);
    return true;
  });

  const first = await rooms.close(room.id, a);
  assert.equal(first.replay, false);
  assert.ok(first.room.closedAt);

  const second = await rooms.close(room.id, a);
  assert.equal(second.replay, true);

  await assert.rejects(() => rooms.close("room_nope", a), { code: "room_not_found", status: 404 });
});

test("dispatcher requires auth and routes cleanly", async () => {
  const { rooms, a } = await setup();

  const denied = await handleRoomsRequest({ service: rooms, method: "GET", path: "/rooms", userId: null });
  assert.equal(denied.status, 401);

  const created = await handleRoomsRequest({
    service: rooms,
    method: "POST",
    path: "/rooms",
    userId: a,
    body: { title: "Dispatcher room" },
  });
  assert.equal(created.status, 201);

  const listed = await handleRoomsRequest({ service: rooms, method: "GET", path: "/rooms", userId: a });
  assert.equal(listed.status, 200);
  assert.equal(listed.body.items.length, 1);

  const unknown = await handleRoomsRequest({ service: rooms, method: "GET", path: "/rooms/nope", userId: a });
  assert.equal(unknown.status, 404);
});
