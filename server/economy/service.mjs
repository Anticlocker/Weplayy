// Server-authoritative wallet, gifts, and top-up state. Balances are never
// accepted from the client; the client sends giftId or packId and the server
// looks up the value.
//
// Money moves inside single transactions. Both wallet rows are locked with
// SELECT ... FOR UPDATE in a deterministic (sorted) order before a transfer,
// so two concurrent gifts cannot interleave or deadlock. The CHECK (balance
// >= 0) constraint is the last line of defence against any overdraft.

import { randomUUID } from "node:crypto";

const USER_ID = /^[A-Za-z0-9_-]{1,64}$/;
const IDEMPOTENCY_KEY = /^[A-Za-z0-9:_-]{8,128}$/;
const MAX_PAGE = 100;

export class EconomyError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createEconomyService(db) {
  const walletOf = db.get.bind(db, "SELECT user_id, balance, updated_at FROM wallets WHERE user_id = $1");
  const txById = db.get.bind(db, "SELECT * FROM transactions WHERE id = $1");
  const txByKey = db.get.bind(db, "SELECT * FROM transactions WHERE idempotency_key = $1");
  const giftById = db.get.bind(
    db,
    "SELECT id, name, description, cost, icon, tint FROM gifts WHERE id = $1 AND active = TRUE",
  );
  const packById = db.get.bind(
    db,
    "SELECT id, coins, bonus, price_minor, currency FROM coin_packs WHERE id = $1 AND active = TRUE",
  );
  const ledgerSum = db.get.bind(db, "SELECT COALESCE(SUM(delta), 0) AS total FROM ledger WHERE user_id = $1");

  async function balanceOf(userId) {
    const row = await walletOf(userId);
    return row?.balance ?? 0;
  }

  function ensureWallet(tx, userId, now) {
    return tx.run(
      "INSERT INTO wallets (user_id, balance, updated_at) VALUES ($1, 0, $2) ON CONFLICT (user_id) DO NOTHING",
      [userId, now],
    );
  }

  function applyDelta(tx, userId, delta, kind, transactionId, now) {
    return Promise.all([
      tx.run("UPDATE wallets SET balance = balance + $1, updated_at = $2 WHERE user_id = $3", [
        delta,
        now,
        userId,
      ]),
      tx.run("INSERT INTO ledger (user_id, delta, kind, transaction_id, created_at) VALUES ($1, $2, $3, $4, $5)", [
        userId,
        delta,
        kind,
        transactionId,
        now,
      ]),
    ]);
  }

  function insertTransaction(tx, fields) {
    return tx.run(
      `INSERT INTO transactions
         (id, kind, status, from_user_id, to_user_id, gift_id, pack_id, coins,
          price_minor, currency, provider, provider_ref, idempotency_key, failure_reason, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)`,
      [
        fields.id,
        fields.kind,
        fields.status,
        fields.fromUserId,
        fields.toUserId,
        fields.giftId,
        fields.packId,
        fields.coins,
        fields.priceMinor,
        fields.currency,
        fields.provider,
        fields.providerRef,
        fields.idempotencyKey,
        fields.failureReason,
        fields.createdAt,
        fields.updatedAt,
      ],
    );
  }

  async function getWallet(userId) {
    assertUser(userId, "userId");
    const row = await walletOf(userId);
    return { userId, balance: row?.balance ?? 0, updatedAt: row?.updated_at ?? 0 };
  }

  async function listGifts() {
    const rows = await db.all(
      "SELECT id, name, description, cost, icon, tint FROM gifts WHERE active = TRUE ORDER BY sort, id",
    );
    return rows.map((gift) => ({ ...gift }));
  }

  async function listCoinPacks() {
    const rows = await db.all(
      "SELECT id, coins, bonus, price_minor, currency FROM coin_packs WHERE active = TRUE ORDER BY sort, id",
    );
    return rows.map((pack) => ({
      id: pack.id,
      coins: pack.coins,
      bonus: pack.bonus,
      totalCoins: pack.coins + pack.bonus,
      priceMinor: pack.price_minor,
      currency: pack.currency,
    }));
  }

  async function sendGift({ fromUserId, toUserId, giftId, idempotencyKey }) {
    assertUser(fromUserId, "fromUserId");
    assertUser(toUserId, "toUserId");
    assertKey(idempotencyKey);
    if (fromUserId === toUserId) {
      throw new EconomyError("cannot_gift_self", "You cannot send a gift to yourself.");
    }

    const replay = await txByKey(idempotencyKey);
    if (replay) {
      return { transaction: toDto(replay), balance: await balanceOf(fromUserId), replay: true };
    }

    const gift = await giftById(giftId);
    if (!gift) throw new EconomyError("gift_not_found", "Unknown gift.", 404);

    const id = `tx_${randomUUID()}`;
    const now = Date.now();
    await db.transaction(async (tx) => {
      await ensureWallet(tx, fromUserId, now);
      await ensureWallet(tx, toUserId, now);
      // Lock both wallets in a deterministic order so concurrent gifts queue
      // instead of deadlocking.
      const [first, second] = [fromUserId, toUserId].sort();
      await tx.get("SELECT balance FROM wallets WHERE user_id = $1 FOR UPDATE", [first]);
      await tx.get("SELECT balance FROM wallets WHERE user_id = $1 FOR UPDATE", [second]);
      const balance = (await tx.get("SELECT balance FROM wallets WHERE user_id = $1", [fromUserId])).balance;
      if (balance < gift.cost) {
        throw new EconomyError("insufficient_funds", "Not enough coins for this gift.", 409);
      }
      await insertTransaction(tx, {
        id,
        kind: "gift",
        status: "completed",
        fromUserId,
        toUserId,
        giftId: gift.id,
        packId: null,
        coins: gift.cost,
        priceMinor: null,
        currency: null,
        provider: null,
        providerRef: null,
        idempotencyKey,
        failureReason: null,
        createdAt: now,
        updatedAt: now,
      });
      await applyDelta(tx, fromUserId, -gift.cost, "gift_sent", id, now);
      await applyDelta(tx, toUserId, gift.cost, "gift_received", id, now);
    });

    return { transaction: toDto(await txById(id)), balance: await balanceOf(fromUserId), replay: false };
  }

  async function createTopUpIntent({ userId, packId, idempotencyKey }) {
    assertUser(userId, "userId");
    assertKey(idempotencyKey);

    const replay = await txByKey(idempotencyKey);
    if (replay) {
      return { transaction: toDto(replay), payment: paymentState(replay), replay: true };
    }

    const pack = await packById(packId);
    if (!pack) throw new EconomyError("pack_not_found", "Unknown coin pack.", 404);

    const id = `tx_${randomUUID()}`;
    const now = Date.now();
    await insertTransaction(db, {
      id,
      kind: "topup",
      status: "pending",
      fromUserId: userId,
      toUserId: null,
      giftId: null,
      packId: pack.id,
      coins: pack.coins + pack.bonus,
      priceMinor: pack.price_minor,
      currency: pack.currency,
      provider: null,
      providerRef: null,
      idempotencyKey,
      failureReason: null,
      createdAt: now,
      updatedAt: now,
    });
    const transaction = await txById(id);
    return { transaction: toDto(transaction), payment: paymentState(transaction), replay: false };
  }

  // Call only after the provider webhook signature was verified.
  async function confirmTopUp({ transactionId, provider, providerRef }) {
    const existing = await txById(transactionId);
    if (!existing) throw new EconomyError("transaction_not_found", "Unknown transaction.", 404);
    if (existing.kind !== "topup") {
      throw new EconomyError("invalid_transition", "Only top-up transactions can be confirmed.", 409);
    }
    if (existing.status === "paid") {
      return { transaction: toDto(existing), balance: await balanceOf(existing.from_user_id), replay: true };
    }
    if (existing.status !== "pending") {
      throw new EconomyError("invalid_transition", `Cannot confirm a ${existing.status} transaction.`, 409);
    }

    const now = Date.now();
    await db.transaction(async (tx) => {
      await ensureWallet(tx, existing.from_user_id, now);
      await tx.get("SELECT balance FROM wallets WHERE user_id = $1 FOR UPDATE", [existing.from_user_id]);
      const claimed = await tx.run(
        `UPDATE transactions SET status = 'paid',
           provider = COALESCE($1, provider), provider_ref = COALESCE($2, provider_ref),
           failure_reason = NULL, updated_at = $3
         WHERE id = $4 AND status = 'pending'`,
        [provider ?? null, providerRef ?? null, now, existing.id],
      );
      if (claimed.changes === 0) {
        throw new EconomyError("invalid_transition", "Transaction already settled.", 409);
      }
      await applyDelta(tx, existing.from_user_id, existing.coins, "topup", existing.id, now);
    });

    return { transaction: toDto(await txById(existing.id)), balance: await balanceOf(existing.from_user_id), replay: false };
  }

  async function failTopUp({ transactionId, reason }) {
    const existing = await txById(transactionId);
    if (!existing) throw new EconomyError("transaction_not_found", "Unknown transaction.", 404);
    if (existing.kind !== "topup") {
      throw new EconomyError("invalid_transition", "Only top-up transactions can fail.", 409);
    }
    if (existing.status === "failed") return { transaction: toDto(existing), replay: true };
    if (existing.status !== "pending") {
      throw new EconomyError("invalid_transition", `Cannot fail a ${existing.status} transaction.`, 409);
    }

    const updated = await db.run(
      `UPDATE transactions SET status = 'failed', failure_reason = $1, updated_at = $2
       WHERE id = $3 AND status = 'pending'`,
      [String(reason ?? "provider_declined").slice(0, 120), Date.now(), existing.id],
    );
    if (updated.changes === 0) {
      throw new EconomyError("invalid_transition", "Transaction already settled.", 409);
    }
    return { transaction: toDto(await txById(existing.id)), replay: false };
  }

  async function listTransactions({ userId, limit, cursor }) {
    assertUser(userId, "userId");
    const take = clampLimit(limit);
    const after = parseCursor(cursor);

    const rows = after
      ? await db.all(
          `SELECT * FROM transactions
           WHERE (from_user_id = $1 OR to_user_id = $1)
             AND (created_at < $2 OR (created_at = $2 AND id < $3))
           ORDER BY created_at DESC, id DESC LIMIT $4`,
          [userId, after.createdAt, after.id, take + 1],
        )
      : await db.all(
          `SELECT * FROM transactions
           WHERE (from_user_id = $1 OR to_user_id = $1)
           ORDER BY created_at DESC, id DESC LIMIT $2`,
          [userId, take + 1],
        );

    const page = rows.slice(0, take);
    const last = page[page.length - 1];
    return {
      items: page.map(toDto),
      nextCursor: rows.length > take && last ? `${last.created_at}:${last.id}` : null,
    };
  }

  async function reconcile(userId) {
    assertUser(userId, "userId");
    const wallet = await balanceOf(userId);
    const ledger = (await ledgerSum(userId)).total;
    return { userId, walletBalance: wallet, ledgerBalance: ledger, ok: wallet === ledger };
  }

  return {
    getWallet,
    listGifts,
    listCoinPacks,
    sendGift,
    createTopUpIntent,
    confirmTopUp,
    failTopUp,
    listTransactions,
    reconcile,
  };
}

function toDto(row) {
  return {
    id: row.id,
    kind: row.kind,
    status: row.status,
    fromUserId: row.from_user_id,
    toUserId: row.to_user_id,
    giftId: row.gift_id,
    packId: row.pack_id,
    coins: row.coins,
    priceMinor: row.price_minor,
    currency: row.currency,
    provider: row.provider,
    providerRef: row.provider_ref,
    failureReason: row.failure_reason,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function paymentState(row) {
  return {
    status: row.status,
    provider: row.provider,
    providerRef: row.provider_ref,
    priceMinor: row.price_minor,
    currency: row.currency,
  };
}

function assertUser(value, field) {
  if (typeof value !== "string" || !USER_ID.test(value)) {
    throw new EconomyError("invalid_user", `${field} is not a valid user id.`);
  }
}

function assertKey(value) {
  if (typeof value !== "string" || !IDEMPOTENCY_KEY.test(value)) {
    throw new EconomyError("invalid_idempotency_key", "idempotencyKey must be 8-128 chars of A-Z a-z 0-9 : _ -");
  }
}

function clampLimit(value) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) return 20;
  return Math.min(parsed, MAX_PAGE);
}

function parseCursor(value) {
  if (typeof value !== "string" || !value.includes(":")) return null;
  const split = value.lastIndexOf(":");
  const createdAt = Number(value.slice(0, split));
  const id = value.slice(split + 1);
  if (!Number.isInteger(createdAt) || id.length === 0) return null;
  return { createdAt, id };
}
