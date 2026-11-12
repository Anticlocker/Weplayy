import assert from "node:assert/strict";
import { test } from "node:test";
import { seedCatalogue } from "./db.mjs";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { createEconomyService, EconomyError } from "./service.mjs";

async function setup() {
  const db = await openTestDatabase("economy-service");
  await clearData(db);
  await seedCatalogue(db);
  return { db, service: createEconomyService(db) };
}

async function topUp(service, userId, packId = "coins_100", key = "topup-key-1") {
  const { transaction } = await service.createTopUpIntent({ userId, packId, idempotencyKey: key });
  return service.confirmTopUp({ transactionId: transaction.id, provider: "test", providerRef: `p_${transaction.id}` });
}

test("new wallet starts empty", async () => {
  const { service } = await setup();
  assert.equal((await service.getWallet("u1")).balance, 0);
});

test("confirmed top-up credits coins once", async () => {
  const { service } = await setup();
  const first = await topUp(service, "u1");
  assert.equal(first.transaction.status, "paid");
  assert.equal(first.balance, 100);

  const again = await service.confirmTopUp({ transactionId: first.transaction.id, provider: "test", providerRef: "dup" });
  assert.equal(again.replay, true);
  assert.equal(again.balance, 100);
});

test("failed top-up cannot be confirmed later", async () => {
  const { service } = await setup();
  const { transaction } = await service.createTopUpIntent({ userId: "u1", packId: "coins_100", idempotencyKey: "topup-key-2" });
  await service.failTopUp({ transactionId: transaction.id, reason: "declined" });
  await assert.rejects(
    () => service.confirmTopUp({ transactionId: transaction.id, provider: "test", providerRef: "x" }),
    (error) => error instanceof EconomyError && error.code === "invalid_transition",
  );
});

test("gift moves coins between wallets", async () => {
  const { service } = await setup();
  await topUp(service, "sender");
  const sent = await service.sendGift({
    fromUserId: "sender",
    toUserId: "receiver",
    giftId: "rose",
    idempotencyKey: "gift-key-1",
  });

  assert.equal(sent.transaction.status, "completed");
  assert.equal(sent.transaction.coins, 10);
  assert.equal(sent.balance, 90);
  assert.equal((await service.getWallet("receiver")).balance, 10);
});

test("gift without enough coins leaves both wallets untouched", async () => {
  const { service } = await setup();
  await assert.rejects(
    () => service.sendGift({ fromUserId: "sender", toUserId: "receiver", giftId: "crown", idempotencyKey: "gift-key-2" }),
    (error) => error instanceof EconomyError && error.code === "insufficient_funds",
  );
  assert.equal((await service.getWallet("sender")).balance, 0);
  assert.equal((await service.getWallet("receiver")).balance, 0);
  assert.equal((await service.listTransactions({ userId: "sender" })).items.length, 0);
});

test("repeated gift keeps the same transaction and does not debit twice", async () => {
  const { service } = await setup();
  await topUp(service, "sender", "coins_1200", "topup-key-3");

  const first = await service.sendGift({
    fromUserId: "sender",
    toUserId: "receiver",
    giftId: "box",
    idempotencyKey: "gift-key-3",
  });
  const second = await service.sendGift({
    fromUserId: "sender",
    toUserId: "receiver",
    giftId: "box",
    idempotencyKey: "gift-key-3",
  });

  assert.equal(second.replay, true);
  assert.equal(second.transaction.id, first.transaction.id);
  assert.equal((await service.getWallet("sender")).balance, first.balance);
  assert.equal((await service.getWallet("receiver")).balance, 50);
});

test("self gifting and unknown gifts are rejected", async () => {
  const { service } = await setup();
  await assert.rejects(
    () => service.sendGift({ fromUserId: "u1", toUserId: "u1", giftId: "rose", idempotencyKey: "gift-key-4" }),
    (error) => error.code === "cannot_gift_self",
  );
  await assert.rejects(
    () => service.sendGift({ fromUserId: "u1", toUserId: "u2", giftId: "nope", idempotencyKey: "gift-key-5" }),
    (error) => error.code === "gift_not_found",
  );
});

test("wallet balance always matches the ledger", async () => {
  const { service } = await setup();
  await topUp(service, "a", "coins_3000", "topup-key-4");
  await topUp(service, "b", "coins_500", "topup-key-5");
  await service.sendGift({ fromUserId: "a", toUserId: "b", giftId: "bolt", idempotencyKey: "gift-key-6" });

  assert.deepEqual(await service.reconcile("a"), {
    userId: "a",
    walletBalance: 3400,
    ledgerBalance: 3400,
    ok: true,
  });
  assert.equal((await service.reconcile("b")).ok, true);
});

test("transactions are listed for both sides with stable paging", async () => {
  const { service } = await setup();
  await topUp(service, "a", "coins_1200", "topup-key-6");
  await topUp(service, "b", "coins_1200", "topup-key-7");
  for (let i = 0; i < 3; i += 1) {
    await service.sendGift({ fromUserId: "a", toUserId: "b", giftId: "rose", idempotencyKey: `gift-page-${i}` });
  }

  const firstPage = await service.listTransactions({ userId: "a", limit: 2 });
  assert.equal(firstPage.items.length, 2);
  assert.ok(firstPage.nextCursor);

  const secondPage = await service.listTransactions({ userId: "a", limit: 2, cursor: firstPage.nextCursor });
  const ids = new Set([...firstPage.items, ...secondPage.items].map((item) => item.id));
  assert.equal(ids.size, firstPage.items.length + secondPage.items.length);
  assert.equal((await service.listTransactions({ userId: "b" })).items.length, 4);
});

test("concurrent gifts cannot overdraw a wallet", async () => {
  const { service } = await setup();
  await topUp(service, "broke", "coins_100", "topup-key-8");

  // Two 100-coin gifts race on the same wallet; only one may succeed.
  const attempts = await Promise.allSettled([
    service.sendGift({ fromUserId: "broke", toUserId: "other1", giftId: "bolt", idempotencyKey: "race-gift-1" }),
    service.sendGift({ fromUserId: "broke", toUserId: "other2", giftId: "bolt", idempotencyKey: "race-gift-2" }),
  ]);
  const fulfilled = attempts.filter((attempt) => attempt.status === "fulfilled");
  assert.equal(fulfilled.length, 1);
  assert.equal((await service.getWallet("broke")).balance, 0);
  assert.equal((await service.reconcile("broke")).ok, true);
});
