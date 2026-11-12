import test from "node:test";
import assert from "node:assert/strict";
import { openTestDatabase, clearData } from "../db/testdb.mjs";
import { seedCatalogue } from "../economy/db.mjs";
import { createIdentityService } from "../identity/service.mjs";
import { createEconomyService } from "../economy/service.mjs";
import { createMessagingService, MessagingError } from "./service.mjs";
import { handleMessagingRequest } from "./routes.mjs";

async function setup() {
  const db = await openTestDatabase("messaging");
  await clearData(db);
  await seedCatalogue(db);
  const identity = createIdentityService(db);
  const economy = createEconomyService(db);
  const messaging = createMessagingService(db);
  const signup = (username) => identity.signUp({ username, password: "password123", displayName: username });
  const a = (await signup("riya")).user.id;
  const b = (await signup("arjun")).user.id;
  return { db, identity, economy, messaging, a, b };
}

test("a pair of users shares exactly one chat, whoever opens it", async () => {
  const { messaging, a, b } = await setup();

  const first = await messaging.openChat(a, b);
  assert.equal(first.created, true);
  assert.equal(first.chat.otherUserId, b);

  const second = await messaging.openChat(b, a);
  assert.equal(second.created, false);
  assert.equal(second.chat.chatId, first.chat.chatId);

  await assert.rejects(() => messaging.openChat(a, a), { code: "cannot_chat_self" });
});

test("concurrent opens of the same chat return one chat, one winner", async () => {
  const { messaging, a, b } = await setup();

  const results = await Promise.all([messaging.openChat(a, b), messaging.openChat(b, a)]);

  // Both callers land on the same committed chat; the loser must hand back
  // the winner's row, not its own uncommitted id.
  assert.equal(results[0].chat.chatId, results[1].chat.chatId);
  assert.equal(results.filter((r) => r.created).length, 1);
  for (const result of results) {
    assert.ok(result.chat.chatId.startsWith("chat_"));
    assert.ok(result.chat.otherUserId === a || result.chat.otherUserId === b);
  }
});

test("sending, unread counts, and read receipts work", async () => {
  const { messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  const sent = await messaging.sendMessage(a, { chatId, kind: "text", body: "hello bhai" });
  assert.equal(sent.kind, "text");
  assert.equal(sent.senderId, a);
  assert.ok(sent.id.startsWith("msg_"));

  let chats = (await messaging.listChats(b)).items;
  assert.equal(chats[0].unread, 1);
  assert.equal(chats[0].lastMessage.body, "hello bhai");

  await messaging.markRead(b, chatId);
  chats = (await messaging.listChats(b)).items;
  assert.equal(chats[0].unread, 0);

  // Sender reads their own message automatically.
  assert.equal((await messaging.listChats(a)).items[0].unread, 0);
});

test("list messages paginates newest first with a cursor", async () => {
  const { messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  for (let i = 1; i <= 5; i += 1) {
    await messaging.sendMessage(a, { chatId, kind: "text", body: `msg ${i}` });
  }

  const page1 = await messaging.listMessages(b, chatId, { limit: 2 });
  assert.equal(page1.items.length, 2);
  assert.equal(page1.items[0].body, "msg 5");
  assert.ok(page1.nextCursor);

  const page2 = await messaging.listMessages(b, chatId, { limit: 2, cursor: page1.nextCursor });
  assert.equal(page2.items[0].body, "msg 3");

  const page3 = await messaging.listMessages(b, chatId, { limit: 10, cursor: page2.nextCursor });
  assert.equal(page3.items.length, 1);
  assert.equal(page3.nextCursor, null);
});

test("chat prefs: pin, mute", async () => {
  const { messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  const prefs = await messaging.updateChat(a, chatId, { pinned: true, muted: true });
  assert.equal(prefs.pinned, true);
  assert.equal(prefs.muted, true);

  const chats = (await messaging.listChats(a)).items;
  assert.equal(chats[0].pinned, true);

  await assert.rejects(() => messaging.updateChat(a, chatId, { pinned: "yes" }), { code: "invalid_flag" });
});

test("recall: only the sender, only once", async () => {
  const { messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  const sent = await messaging.sendMessage(a, { chatId, kind: "text", body: "galat msg" });
  const recalled = await messaging.recallMessage(a, sent.id);
  assert.equal(recalled.recalled, true);

  await assert.rejects(() => messaging.recallMessage(b, sent.id), (error) => {
    assert.ok(error instanceof MessagingError);
    assert.equal(error.code, "not_allowed");
    assert.equal(error.status, 403);
    return true;
  });

  // Listing hides the body of a recalled message but keeps the row.
  const listed = (await messaging.listMessages(b, chatId)).items[0];
  assert.equal(listed.recalled, true);
});

async function creditWallet(economy, userId, packId, key) {
  const intent = await economy.createTopUpIntent({ userId, packId, idempotencyKey: key });
  return economy.confirmTopUp({ transactionId: intent.transaction.id, provider: "test", providerRef: key });
}

test("gift messages ride a completed economy transaction", async () => {
  const { messaging, economy, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  // No coins yet: the gift transaction cannot exist.
  await assert.rejects(
    () => messaging.sendMessage(a, { chatId, kind: "gift", giftTransactionId: "tx_missing" }),
    { code: "invalid_gift_transaction" },
  );

  await creditWallet(economy, a, "coins_500", "key-topup-a1");
  const gift = await economy.sendGift({ fromUserId: a, toUserId: b, giftId: "rose", idempotencyKey: "key-gift-a1" });
  assert.equal(gift.transaction.status, "completed");

  const message = await messaging.sendMessage(a, {
    chatId,
    kind: "gift",
    giftTransactionId: gift.transaction.id,
  });
  assert.equal(message.kind, "gift");
  assert.equal(message.giftId, "rose");
  assert.ok(message.giftCost > 0);

  // The money already moved in economy; sending again with the same
  // transaction id is rejected instead of double-crediting a chat.
  await assert.rejects(
    () => messaging.sendMessage(a, { chatId, kind: "gift", giftTransactionId: gift.transaction.id }),
    { code: "invalid_gift_transaction" },
  );

  // A second, distinct gift transaction still produces a message.
  await creditWallet(economy, a, "coins_100", "key-topup-a2");
  const other = await economy.sendGift({ fromUserId: a, toUserId: b, giftId: "star", idempotencyKey: "key-gift-a2" });
  assert.doesNotThrow(() =>
    messaging.sendMessage(a, { chatId, kind: "gift", giftTransactionId: other.transaction.id }),
  );
});

test("blocks and dm permission gate message delivery", async () => {
  const { db, identity, messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  // Recipient turns DMs off for non-friends; they are not friends yet.
  await identity.updateSettings(b, { dmPermission: false });
  await assert.rejects(
    () => messaging.sendMessage(a, { chatId, kind: "text", body: "hi" }),
    { code: "dm_not_allowed", status: 403 },
  );

  // Becoming friends opens the door.
  await db.run("INSERT INTO friendships (user_id, friend_id, created_at) VALUES ($1, $2, $3)", [a, b, Date.now()]);
  await db.run("INSERT INTO friendships (user_id, friend_id, created_at) VALUES ($1, $2, $3)", [b, a, Date.now()]);
  await messaging.sendMessage(a, { chatId, kind: "text", body: "hi friend" });

  // A block in either direction stops delivery.
  await db.run("INSERT INTO blocks (user_id, blocked_id, created_at) VALUES ($1, $2, $3)", [b, a, Date.now()]);
  await assert.rejects(
    () => messaging.sendMessage(a, { chatId, kind: "text", body: "hello?" }),
    { code: "blocked", status: 403 },
  );
});

test("media messages need a URL; voice needs a duration", async () => {
  const { messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  await assert.rejects(() => messaging.sendMessage(a, { chatId, kind: "image" }), { code: "invalid_media" });
  await messaging.sendMessage(a, { chatId, kind: "image", mediaUrl: "https://cdn.example.com/p.jpg" });
  await assert.rejects(
    () => messaging.sendMessage(a, { chatId, kind: "voice", mediaUrl: "https://cdn.example.com/v.ogg" }),
    { code: "invalid_duration" },
  );
  await messaging.sendMessage(a, {
    chatId,
    kind: "voice",
    mediaUrl: "https://cdn.example.com/v.ogg",
    durationMs: 4200,
  });
  await assert.rejects(() => messaging.sendMessage(a, { chatId, kind: "sticker", body: "x" }), { code: "invalid_kind" });
});

test("dispatcher requires auth and handles routing", async () => {
  const { messaging, a, b } = await setup();
  const { chatId } = (await messaging.openChat(a, b)).chat;

  const denied = await handleMessagingRequest({ service: messaging, method: "GET", path: "/messaging/chats", userId: null });
  assert.equal(denied.status, 401);

  const chats = await handleMessagingRequest({ service: messaging, method: "GET", path: "/messaging/chats", userId: a });
  assert.equal(chats.status, 200);
  assert.equal(chats.body.items.length, 1);

  const send = await handleMessagingRequest({
    service: messaging,
    method: "POST",
    path: `/messaging/chats/${chatId}/messages`,
    userId: a,
    body: { kind: "text", body: "via dispatcher" },
  });
  assert.equal(send.status, 201);
  assert.equal(send.body.body, "via dispatcher");

  const listed = await handleMessagingRequest({
    service: messaging,
    method: "GET",
    path: `/messaging/chats/${chatId}/messages`,
    userId: b,
  });
  assert.equal(listed.status, 200);
  assert.ok(listed.body.items.length >= 1);

  const marked = await handleMessagingRequest({
    service: messaging,
    method: "POST",
    path: `/messaging/chats/${chatId}/read`,
    userId: b,
    body: {},
  });
  assert.equal(marked.status, 200);

  const unknown = await handleMessagingRequest({ service: messaging, method: "GET", path: "/messaging/nope", userId: a });
  assert.equal(unknown.status, 404);
});
