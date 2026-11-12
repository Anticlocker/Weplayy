import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "./testdb.mjs";
import { migrate } from "./db.mjs";

const CORE_TABLES = [
  "users",
  "user_settings",
  "device_sessions",
  "friendships",
  "friend_requests",
  "blocks",
  "reports",
  "chats",
  "chat_members",
  "messages",
  "notifications",
  "rooms",
  "support_tickets",
];

const ECONOMY_TABLES = ["wallets", "gifts", "coin_packs", "transactions", "ledger"];

async function tables(db) {
  const { rows } = await db.query(
    `SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename`,
  );
  return rows.map((row) => row.tablename);
}

async function insertUser(db, id, username) {
  await db.run(
    "INSERT INTO users (id, username, display_name, created_at, updated_at) VALUES ($1, $2, $3, $4, $5)",
    [id, username, username, Date.now(), Date.now()],
  );
}

async function insertChat(db, id, a, b) {
  await db.run(
    "INSERT INTO chats (id, user_a_id, user_b_id, created_at) VALUES ($1, $2, $3, $4)",
    [id, a, b, Date.now()],
  );
}

test("migrations create the platform and economy tables exactly once", async () => {
  const db = await openTestDatabase("db");
  await clearData(db);

  const present = await tables(db);
  for (const name of CORE_TABLES) assert.ok(present.includes(name), `missing table: ${name}`);
  for (const name of ECONOMY_TABLES) assert.ok(present.includes(name), `missing table: ${name}`);

  const row = await db.get("SELECT COALESCE(MAX(version), 0) AS version FROM schema_migrations");
  assert.equal(Number(row.version), 2);

  // Re-running is a no-op: no duplicate application, version unchanged.
  await migrate(db);
  const again = await db.get("SELECT COUNT(*) AS n FROM schema_migrations");
  assert.equal(Number(again.n), 2);
});

test("foreign keys are enforced", async () => {
  const db = await openTestDatabase("db");
  await clearData(db);
  await insertUser(db, "u1", "u1");
  await insertUser(db, "u2", "u2");
  await insertChat(db, "c1", "u1", "u2");
  await assert.rejects(() =>
    db.run(
      "INSERT INTO messages (id, chat_id, sender_id, kind, body, created_at) VALUES ($1, $2, $3, $4, $5, $6)",
      ["m1", "missing-chat", "u1", "text", "hi", Date.now()],
    ),
  );
});

test("usernames and phone/email identities are unique", async () => {
  const db = await openTestDatabase("db");
  await clearData(db);
  await insertUser(db, "u1", "dup");
  await assert.rejects(() => insertUser(db, "u2", "dup"));
  await db.run(
    "INSERT INTO users (id, username, display_name, phone, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6)",
    ["u3", "u3", "u3", "9999999999", Date.now(), Date.now()],
  );
  await assert.rejects(() =>
    db.run(
      "INSERT INTO users (id, username, display_name, phone, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6)",
      ["u4", "u4", "u4", "9999999999", Date.now(), Date.now()],
    ),
  );
});

test("a chat exists once per ordered pair of users", async () => {
  const db = await openTestDatabase("db");
  await clearData(db);
  await insertUser(db, "u1", "u1");
  await insertUser(db, "u2", "u2");
  await insertChat(db, "c1", "u1", "u2");
  // Reversed pair must fail: the smaller id always comes first.
  await assert.rejects(() => insertChat(db, "c2", "u2", "u1"));
  // Same pair again must fail on the unique constraint.
  await assert.rejects(() => insertChat(db, "c3", "u1", "u2"));
});

test("message kind and gift-transaction uniqueness are validated", async () => {
  const db = await openTestDatabase("db");
  await clearData(db);
  await insertUser(db, "u1", "u1");
  await insertUser(db, "u2", "u2");
  await insertChat(db, "c1", "u1", "u2");
  const insert = db.run.bind(
    db,
    `INSERT INTO messages (id, chat_id, sender_id, kind, body, gift_transaction_id, created_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
  );
  await assert.rejects(() => insert(["m1", "c1", "u1", "sticker", "hi", null, Date.now()]));
  await db.run(
    "INSERT INTO messages (id, chat_id, sender_id, kind, body, gift_transaction_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)",
    ["m2", "c1", "u1", "text", "hi", null, Date.now()],
  );
  await db.run(
    "INSERT INTO messages (id, chat_id, sender_id, kind, body, gift_transaction_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)",
    ["m3", "c1", "u1", "gift", "Sent Rose", "tx_1", Date.now()],
  );
  await assert.rejects(() =>
    db.run(
      "INSERT INTO messages (id, chat_id, sender_id, kind, body, gift_transaction_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)",
      ["m4", "c1", "u1", "gift", "Sent Rose", "tx_1", Date.now()],
    ),
  );
});

test("transactions commit together and roll back on failure", async () => {
  const db = await openTestDatabase("db");
  await clearData(db);

  await db.transaction(async (tx) => {
    await tx.run("INSERT INTO users (id, username, display_name, created_at, updated_at) VALUES ($1, $2, $3, $4, $5)", [
      "t1",
      "t1",
      "T1",
      Date.now(),
      Date.now(),
    ]);
    await tx.run("INSERT INTO user_settings (user_id) VALUES ($1)", ["t1"]);
  });
  const ok = await db.get("SELECT COUNT(*) AS n FROM user_settings WHERE user_id = 't1'");
  assert.equal(Number(ok.n), 1);

  await assert.rejects(() =>
    db.transaction(async (tx) => {
      await tx.run("INSERT INTO users (id, username, display_name, created_at, updated_at) VALUES ($1, $2, $3, $4, $5)", [
        "t2",
        "t2",
        "T2",
        Date.now(),
        Date.now(),
      ]);
      throw new Error("boom");
    }),
  );
  const rolled = await db.get("SELECT COUNT(*) AS n FROM users WHERE id = 't2'");
  assert.equal(Number(rolled.n), 0);
});
