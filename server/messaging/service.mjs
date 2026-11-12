// Direct messaging: chats, messages, delivery of read state.
//
// A chat exists exactly once per pair of users (the smaller id is stored in
// user_a_id). Money never moves here: a gift message references a completed
// gift transaction from the economy module, and its id is stored UNIQUELY on
// the message, so a client cannot conjure coins or replay gifts by posting
// chat messages directly.
//
// Recipient controls are honoured at send time: a block in either direction
// stops delivery, and `dmPermission: false` restricts new non-friend chats.

import { randomUUID } from "node:crypto";

const KINDS = new Set(["text", "gift", "image", "voice"]);
const BODY_MAX = 4000;
const URL_MAX = 500;
const PREVIEW_MAX = 80;
const MAX_PAGE = 100;

export class MessagingError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createMessagingService(db) {
  const userCard = db.get.bind(db, "SELECT id, username, display_name, avatar_url, level FROM users WHERE id = $1");
  const chatById = db.get.bind(db, "SELECT * FROM chats WHERE id = $1");
  const chatForPair = db.get.bind(db, "SELECT * FROM chats WHERE user_a_id = $1 AND user_b_id = $2");
  const memberRow = db.get.bind(db, "SELECT * FROM chat_members WHERE chat_id = $1 AND user_id = $2");
  const blockBetween = db.get.bind(
    db,
    "SELECT 1 FROM blocks WHERE (user_id = $1 AND blocked_id = $2) OR (user_id = $3 AND blocked_id = $4)",
  );
  const friendBetween = db.get.bind(db, "SELECT 1 FROM friendships WHERE user_id = $1 AND friend_id = $2");
  const dmPermissionOf = db.get.bind(db, "SELECT dm_permission FROM user_settings WHERE user_id = $1");
  const notifyMessagesOf = db.get.bind(db, "SELECT notify_messages FROM user_settings WHERE user_id = $1");
  const completedGiftTx = db.get.bind(
    db,
    `SELECT * FROM transactions
     WHERE id = $1 AND kind = 'gift' AND status = 'completed' AND from_user_id = $2 AND to_user_id = $3`,
  );
  const messageByGiftTx = db.get.bind(db, "SELECT id FROM messages WHERE gift_transaction_id = $1");
  const insertNotification = db.run.bind(
    db,
    "INSERT INTO notifications (id, user_id, kind, actor_id, body, created_at) VALUES ($1, $2, $3, $4, $5, $6)",
  );

  // --- chats --------------------------------------------------------------

  async function openChat(userId, otherUserId) {
    assertUser(userId, "userId");
    assertUser(otherUserId, "otherUserId");
    if (userId === otherUserId) {
      throw new MessagingError("cannot_chat_self", "You cannot open a chat with yourself.");
    }
    if (!(await userCard(otherUserId))) throw new MessagingError("invalid_user", "Unknown user.", 404);
    if (await blockBetween(userId, otherUserId, otherUserId, userId)) {
      throw new MessagingError("blocked", "This chat cannot be opened.", 403);
    }

    const [a, b] = [userId, otherUserId].sort();
    const existing = await chatForPair(a, b);
    if (existing) return { chat: chatDto(existing, userId), created: false };

    const id = `chat_${randomUUID()}`;
    const now = Date.now();
    try {
      await db.transaction(async (tx) => {
        await tx.run("INSERT INTO chats (id, user_a_id, user_b_id, created_at) VALUES ($1, $2, $3, $4)", [id, a, b, now]);
        await tx.run("INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2)", [id, a]);
        await tx.run("INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2)", [id, b]);
      });
    } catch (error) {
      if (error.code !== "23505") throw error;
      // A simultaneous open inserted the chat first: hand back the winner's
      // row instead of the local id, which was never committed.
      const winner = await chatForPair(a, b);
      if (!winner) throw error; // the conflict was not the pair constraint
      return { chat: chatDto(winner, userId), created: false };
    }
    return { chat: chatDto(await chatById(id), userId), created: true };
  }

  async function listChats(userId, { limit, cursor } = {}) {
    assertUser(userId, "userId");
    const take = clampLimit(limit);
    const after = parseCursor(cursor);

    const base = `
      FROM chats c
      JOIN chat_members me ON me.chat_id = c.id AND me.user_id = $1
      LEFT JOIN messages lm ON lm.id = (
        SELECT id FROM messages WHERE chat_id = c.id ORDER BY created_at DESC, seq DESC LIMIT 1
      )`;
    const activity = "COALESCE(lm.created_at, c.created_at)";

    const rows = after
      ? await db.all(
          `SELECT c.id, c.user_a_id, c.user_b_id, c.created_at,
                  me.pinned, me.muted, me.last_read_at,
                  (SELECT COUNT(*) FROM messages x
                    WHERE x.chat_id = c.id AND x.sender_id <> $2 AND x.created_at > me.last_read_at) AS unread,
                  lm.kind AS last_kind, lm.body AS last_body, lm.sender_id AS last_sender,
                  lm.recalled AS last_recalled, lm.created_at AS last_ts
           ${base}
           WHERE (${activity} < $3 OR (${activity} = $3 AND c.id < $4))
           ORDER BY ${activity} DESC, c.id DESC LIMIT $5`,
          [userId, userId, after.createdAt, after.id, take + 1],
        )
      : await db.all(
          `SELECT c.id, c.user_a_id, c.user_b_id, c.created_at,
                  me.pinned, me.muted, me.last_read_at,
                  (SELECT COUNT(*) FROM messages x
                    WHERE x.chat_id = c.id AND x.sender_id <> $2 AND x.created_at > me.last_read_at) AS unread,
                  lm.kind AS last_kind, lm.body AS last_body, lm.sender_id AS last_sender,
                  lm.recalled AS last_recalled, lm.created_at AS last_ts
           ${base}
           ORDER BY ${activity} DESC, c.id DESC LIMIT $3`,
          [userId, userId, take + 1],
        );

    const page = rows.slice(0, take);
    const last = page[page.length - 1];
    const items = [];
    for (const row of page) {
      items.push(chatListItem(row, await userCard(otherUserId(row, userId))));
    }
    return {
      items,
      nextCursor: rows.length > take && last ? `${lastActivity(last)}:${last.id}` : null,
    };
  }

  async function updateChat(userId, chatId, patch) {
    const member = await requireMember(userId, chatId);
    const pinned = patch.pinned === undefined ? member.pinned : flag(patch.pinned, "pinned");
    const muted = patch.muted === undefined ? member.muted : flag(patch.muted, "muted");
    await db.run("UPDATE chat_members SET pinned = $1, muted = $2 WHERE chat_id = $3 AND user_id = $4", [
      pinned,
      muted,
      chatId,
      userId,
    ]);
    const updated = await memberRow(chatId, userId);
    return { chatId, pinned: !!updated.pinned, muted: !!updated.muted, lastReadAt: updated.last_read_at };
  }

  // --- messages -----------------------------------------------------------

  async function sendMessage(userId, { chatId, kind, body, mediaUrl, durationMs, giftTransactionId }) {
    assertUser(userId, "userId");
    if (!KINDS.has(kind)) throw new MessagingError("invalid_kind", "kind must be text, gift, image, or voice.");

    const member = await requireMember(userId, chatId);
    const chat = await chatById(chatId);
    const otherId = otherUserId(chat, userId);
    if (await blockBetween(userId, otherId, otherId, userId)) {
      throw new MessagingError("blocked", "This message cannot be sent.", 403);
    }

    let text = "";
    let giftId = null;
    let giftCost = 0;
    let url = null;
    let duration = 0;

    if (kind === "text") {
      if (typeof body !== "string" || body.trim().length === 0 || body.length > BODY_MAX) {
        throw new MessagingError("invalid_body", `body must be 1-${BODY_MAX} characters.`);
      }
      text = body;
    } else if (kind === "gift") {
      if (typeof giftTransactionId !== "string" || giftTransactionId.length === 0) {
        throw new MessagingError("invalid_gift_transaction", "A completed gift transaction id is required.");
      }
      const tx = await completedGiftTx(giftTransactionId, userId, otherId);
      if (!tx) {
        throw new MessagingError("invalid_gift_transaction", "No completed gift to that user matches this id.", 409);
      }
      if (await messageByGiftTx(giftTransactionId)) {
        throw new MessagingError("invalid_gift_transaction", "This gift transaction was already sent as a message.", 409);
      }
      giftId = tx.gift_id;
      giftCost = tx.coins;
    } else {
      if (typeof mediaUrl !== "string" || mediaUrl.length === 0 || mediaUrl.length > URL_MAX) {
        throw new MessagingError("invalid_media", "mediaUrl is required for image and voice messages.");
      }
      url = mediaUrl;
      if (kind === "voice") {
        if (!Number.isInteger(durationMs) || durationMs < 0) {
          throw new MessagingError("invalid_duration", "durationMs must be a non-negative integer.");
        }
        duration = durationMs;
      }
    }

    // Recipient controls: friends always pass, strangers need dm_permission.
    if (!(await friendBetween(userId, otherId)) && !(await dmPermissionOf(otherId))?.dm_permission) {
      throw new MessagingError("dm_not_allowed", "This user only accepts messages from friends.", 403);
    }

    const id = `msg_${randomUUID()}`;
    const now = Date.now();
    await db.transaction(async (tx) => {
      await tx.run(
        `INSERT INTO messages (id, chat_id, sender_id, kind, body, gift_id, gift_transaction_id, gift_cost, media_url, duration_ms, recalled, created_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, FALSE, $11)`,
        [id, chatId, userId, kind, text, giftId, kind === "gift" ? giftTransactionId : null, giftCost, url, duration, now],
      );
      await tx.run(
        "UPDATE chat_members SET last_read_at = $1 WHERE chat_id = $2 AND user_id = $3 AND last_read_at < $1",
        [now, chatId, userId],
      );
      if ((await notifyMessagesOf(otherId))?.notify_messages !== false) {
        await tx.run(
          "INSERT INTO notifications (id, user_id, kind, actor_id, body, created_at) VALUES ($1, $2, $3, $4, $5, $6)",
          [`ntf_${randomUUID()}`, otherId, "message", userId, preview(kind, text), now],
        );
      }
    });
    return toMessageDto(await db.get("SELECT * FROM messages WHERE id = $1", [id]));
  }

  async function listMessages(userId, chatId, { limit, cursor } = {}) {
    await requireMember(userId, chatId);
    const take = clampLimit(limit);
    const after = parseNumericCursor(cursor);

    // seq breaks ties by insertion order, so messages sent within the same
    // millisecond page deterministically.
    const rows = after
      ? await db.all(
          `SELECT * FROM messages
           WHERE chat_id = $1 AND (created_at < $2 OR (created_at = $2 AND seq < $3))
           ORDER BY created_at DESC, seq DESC LIMIT $4`,
          [chatId, after.createdAt, after.seq, take + 1],
        )
      : await db.all(
          "SELECT * FROM messages WHERE chat_id = $1 ORDER BY created_at DESC, seq DESC LIMIT $2",
          [chatId, take + 1],
        );

    const page = rows.slice(0, take);
    const last = page[page.length - 1];
    return {
      items: page.map(toMessageDto),
      nextCursor: rows.length > take && last ? `${last.created_at}:${last.seq}` : null,
    };
  }

  async function markRead(userId, chatId, atMs) {
    await requireMember(userId, chatId);
    const requested = Number.isInteger(atMs) && atMs > 0 ? atMs : Date.now();
    const at = Math.min(requested, Date.now());
    await db.run(
      "UPDATE chat_members SET last_read_at = $1 WHERE chat_id = $2 AND user_id = $3 AND last_read_at < $1",
      [at, chatId, userId],
    );
    return { chatId, lastReadAt: (await memberRow(chatId, userId)).last_read_at };
  }

  async function recallMessage(userId, messageId) {
    assertUser(userId, "userId");
    const message = await db.get("SELECT * FROM messages WHERE id = $1", [messageId]);
    if (!message) throw new MessagingError("message_not_found", "Unknown message.", 404);
    if (message.sender_id !== userId) {
      throw new MessagingError("not_allowed", "Only the sender can recall a message.", 403);
    }
    if (!message.recalled) {
      await db.run("UPDATE messages SET recalled = TRUE WHERE id = $1", [messageId]);
    }
    return toMessageDto(await db.get("SELECT * FROM messages WHERE id = $1", [messageId]));
  }

  // --- internals ----------------------------------------------------------

  async function requireMember(userId, chatId) {
    assertUser(userId, "userId");
    const member = await memberRow(chatId, userId);
    if (!member) throw new MessagingError("chat_not_found", "Unknown chat.", 404);
    return member;
  }

  return {
    openChat,
    listChats,
    updateChat,
    sendMessage,
    listMessages,
    markRead,
    recallMessage,
  };
}

function otherUserId(chat, userId) {
  return chat.user_a_id === userId ? chat.user_b_id : chat.user_a_id;
}

function chatDto(chat, userId) {
  const other = otherUserId(chat, userId);
  return { chatId: chat.id, otherUserId: other, createdAt: chat.created_at };
}

function chatListItem(row, other) {
  return {
    chatId: row.id,
    other: {
      id: other.id,
      username: other.username,
      displayName: other.display_name,
      avatarUrl: other.avatar_url,
      level: other.level,
    },
    pinned: !!row.pinned,
    muted: !!row.muted,
    unread: Number(row.unread),
    lastReadAt: row.last_read_at,
    lastMessage: row.last_ts
      ? {
          kind: row.last_kind,
          body: row.last_recalled ? "" : row.last_body,
          senderId: row.last_sender,
          recalled: !!row.last_recalled,
          createdAt: row.last_ts,
        }
      : null,
    createdAt: row.created_at,
  };
}

function lastActivity(row) {
  return row.last_ts ?? row.created_at;
}

function preview(kind, body) {
  if (kind === "text") return body.slice(0, PREVIEW_MAX);
  if (kind === "image") return "sent a photo.";
  if (kind === "voice") return "sent a voice note.";
  return "sent you a gift.";
}

function toMessageDto(row) {
  return {
    id: row.id,
    chatId: row.chat_id,
    senderId: row.sender_id,
    kind: row.kind,
    body: row.body,
    giftId: row.gift_id,
    giftTransactionId: row.gift_transaction_id,
    giftCost: row.gift_cost,
    mediaUrl: row.media_url,
    durationMs: row.duration_ms,
    recalled: !!row.recalled,
    createdAt: row.created_at,
  };
}

function flag(value, name) {
  if (typeof value !== "boolean") throw new MessagingError("invalid_flag", `${name} must be true or false.`);
  return value;
}

function assertUser(value, field) {
  if (typeof value !== "string" || value.length === 0 || value.length > 64) {
    throw new MessagingError("invalid_user", `${field} is not a valid user id.`);
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

function parseNumericCursor(value) {
  if (typeof value !== "string" || !value.includes(":")) return null;
  const split = value.lastIndexOf(":");
  const createdAt = Number(value.slice(0, split));
  const seq = Number(value.slice(split + 1));
  if (!Number.isInteger(createdAt) || !Number.isInteger(seq)) return null;
  return { createdAt, seq };
}
