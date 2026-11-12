import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { createIdentityService } from "../identity/service.mjs";
import { createSupportService, SupportError } from "./service.mjs";
import { handleSupportRequest } from "./routes.mjs";

async function setup() {
  const db = await openTestDatabase("support");
  await clearData(db);
  const identity = createIdentityService(db);
  const support = createSupportService(db);
  const a = (await identity.signUp({ username: "user1", password: "password123", displayName: "One" })).user.id;
  const b = (await identity.signUp({ username: "user2", password: "password123", displayName: "Two" })).user.id;
  return { support, a, b };
}

test("create a ticket with and without a screenshot", async () => {
  const { support, a } = await setup();

  const plain = await support.create(a, { category: "bug", message: "Voice cuts out on 4G." });
  assert.ok(plain.id.startsWith("sup_"));
  assert.equal(plain.category, "bug");
  assert.equal(plain.imageUrl, null);

  const withImage = await support.create(a, {
    category: "crash",
    message: "App closed itself.",
    imageUrl: "https://cdn.example.com/shot.png",
  });
  assert.equal(withImage.imageUrl, "https://cdn.example.com/shot.png");

  await assert.rejects(() => support.create(a, { category: "", message: "x" }), { code: "invalid_category" });
  await assert.rejects(() => support.create(a, { category: "bug", message: "" }), { code: "invalid_message" });
  await assert.rejects(() => support.create(a, { category: "bug", message: "x", imageUrl: 42 }), {
    code: "invalid_image",
  });
});

test("listing shows only the caller's tickets, newest first", async () => {
  const { support, a, b } = await setup();

  await support.create(a, { category: "bug", message: "first" });
  await support.create(a, { category: "bug", message: "second" });
  await support.create(b, { category: "idea", message: "someone else" });

  const mine = (await support.listMine(a)).items;
  assert.equal(mine.length, 2);
  assert.equal(mine[0].message, "second");
  assert.ok(mine.every((t) => t.message !== "someone else"));
});

test("dispatcher requires auth and routes cleanly", async () => {
  const { support, a } = await setup();

  const denied = await handleSupportRequest({ service: support, method: "GET", path: "/support", userId: null });
  assert.equal(denied.status, 401);

  const created = await handleSupportRequest({
    service: support,
    method: "POST",
    path: "/support",
    userId: a,
    body: { category: "bug", message: "via dispatcher" },
  });
  assert.equal(created.status, 201);

  const listed = await handleSupportRequest({ service: support, method: "GET", path: "/support", userId: a });
  assert.equal(listed.status, 200);
  assert.equal(listed.body.items.length, 1);

  const unknown = await handleSupportRequest({ service: support, method: "GET", path: "/support/nope", userId: a });
  assert.equal(unknown.status, 404);
});
