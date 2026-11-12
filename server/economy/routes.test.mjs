import assert from "node:assert/strict";
import { test } from "node:test";
import { seedCatalogue } from "./db.mjs";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { handleEconomyRequest } from "./routes.mjs";
import { createEconomyService } from "./service.mjs";

async function setup() {
  const db = await openTestDatabase("economy-routes");
  await clearData(db);
  await seedCatalogue(db);
  const service = createEconomyService(db);
  return async (request) => handleEconomyRequest({ service, ...request });
}

test("wallet requires an authenticated user", async () => {
  const call = await setup();
  const response = await call({ method: "GET", path: "/economy/wallet" });
  assert.equal(response.status, 401);
  assert.equal(response.body.error.code, "unauthenticated");
});

test("unknown route returns the error envelope", async () => {
  const call = await setup();
  const response = await call({ method: "GET", path: "/economy/nope", userId: "u1" });
  assert.equal(response.status, 404);
  assert.equal(response.body.error.code, "not_found");
});

test("gift endpoint returns 201 then 200 on replay", async () => {
  const call = await setup();
  await call({
    method: "POST",
    path: "/economy/topup/intents",
    userId: "a",
    body: { packId: "coins_100", idempotencyKey: "route-topup-1" },
  });

  const wallet = await call({ method: "GET", path: "/economy/wallet", userId: "a" });
  assert.equal(wallet.body.balance, 0);

  const packs = await call({ method: "GET", path: "/economy/packs", userId: "a" });
  assert.equal(packs.body.items.length, 6);

  const gifts = await call({ method: "GET", path: "/economy/gifts", userId: "a" });
  assert.equal(gifts.body.items[0].id, "rose");

  const tx = await call({
    method: "POST",
    path: "/economy/topup/intents",
    userId: "a",
    body: { packId: "coins_100", idempotencyKey: "route-topup-1" },
  });
  assert.equal(tx.status, 200);
  assert.equal(tx.body.replay, true);
});

test("insufficient funds maps to 409 with a stable code", async () => {
  const call = await setup();
  const response = await call({
    method: "POST",
    path: "/economy/gifts/send",
    userId: "a",
    body: { toUserId: "b", giftId: "crown", idempotencyKey: "route-gift-1" },
  });
  assert.equal(response.status, 409);
  assert.equal(response.body.error.code, "insufficient_funds");
});

test("trailing slashes are ignored", async () => {
  const call = await setup();
  const response = await call({ method: "GET", path: "/economy/wallet/", userId: "u1" });
  assert.equal(response.status, 200);
});
