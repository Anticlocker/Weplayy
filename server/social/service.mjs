// Social graph: friend requests, friendships, blocks, reports.
//
// Friendships are stored in both directions (one row per direction) so a
// "my friends" lookup is a single indexed query. A block removes friendships
// and requests in both directions, matching the client's behaviour: blocking
// someone unfriends them and cancels anything pending.

import { randomUUID } from "node:crypto";

const REASON_MAX = 500;

export class SocialError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createSocialService(db) {
  const userCard = db.get.bind(db, "SELECT id, username, display_name, avatar_url, level FROM users WHERE id = $1");
  const isFriend = db.get.bind(db, "SELECT 1 FROM friendships WHERE user_id = $1 AND friend_id = $2");
  const isBlocked = db.get.bind(
    db,
    "SELECT 1 FROM blocks WHERE (user_id = $1 AND blocked_id = $2) OR (user_id = $3 AND blocked_id = $4)",
  );
  const requestBetween = db.get.bind(
    db,
    `SELECT * FROM friend_requests
     WHERE (from_user_id = $1 AND to_user_id = $2) OR (from_user_id = $3 AND to_user_id = $4)`,
  );
  const insertNotification = db.run.bind(
    db,
    "INSERT INTO notifications (id, user_id, kind, actor_id, body, created_at) VALUES ($1, $2, $3, $4, $5, $6)",
  );

  async function sendRequest(fromUserId, toUserId) {
    assertUser(fromUserId, "fromUserId");
    assertUser(toUserId, "toUserId");
    if (fromUserId === toUserId) {
      throw new SocialError("cannot_add_self", "You cannot send a request to yourself.");
    }
    await requireUser(toUserId);
    if (await isFriend(fromUserId, toUserId)) {
      throw new SocialError("already_friends", "You are already friends.", 409);
    }
    if (await isBlocked(toUserId, fromUserId, fromUserId, toUserId)) {
      throw new SocialError("blocked", "This request cannot be sent.", 403);
    }

    const existing = await requestBetween(fromUserId, toUserId, toUserId, fromUserId);
    const now = Date.now();

    // A reciprocal pending request makes both users friends immediately.
    if (existing && existing.from_user_id === toUserId && existing.status === "pending") {
      await db.transaction(async (tx) => {
        await tx.run("UPDATE friend_requests SET status = 'accepted' WHERE from_user_id = $1 AND to_user_id = $2", [
          toUserId,
          fromUserId,
        ]);
        await tx.run("INSERT INTO friendships (user_id, friend_id, created_at) VALUES ($1, $2, $3)", [
          fromUserId,
          toUserId,
          now,
        ]);
        await tx.run("INSERT INTO friendships (user_id, friend_id, created_at) VALUES ($1, $2, $3)", [
          toUserId,
          fromUserId,
          now,
        ]);
      });
      await notify(fromUserId, "friend_accepted", toUserId, "accepted your friend request.");
      await notify(toUserId, "friend_accepted", fromUserId, "is now your friend.");
      return { status: "accepted", replay: false };
    }
    if (existing && existing.status === "pending") {
      throw new SocialError("request_exists", "A request is already pending.", 409);
    }
    if (existing && existing.status === "accepted") {
      throw new SocialError("already_friends", "You are already friends.", 409);
    }

    await db.run(
      "INSERT INTO friend_requests (from_user_id, to_user_id, status, created_at) VALUES ($1, $2, 'pending', $3)",
      [fromUserId, toUserId, now],
    );
    await notify(toUserId, "friend_request", fromUserId, "sent you a friend request.");
    return { status: "pending", replay: false };
  }

  async function acceptRequest(userId, fromUserId) {
    assertUser(userId, "userId");
    assertUser(fromUserId, "fromUserId");
    const request = await requestBetween(fromUserId, userId, userId, fromUserId);
    if (!request || request.status !== "pending" || request.to_user_id !== userId) {
      throw new SocialError("request_not_found", "No pending request from that user.", 404);
    }

    const now = Date.now();
    await db.transaction(async (tx) => {
      await tx.run("UPDATE friend_requests SET status = 'accepted' WHERE from_user_id = $1 AND to_user_id = $2", [
        fromUserId,
        userId,
      ]);
      await tx.run("INSERT INTO friendships (user_id, friend_id, created_at) VALUES ($1, $2, $3)", [
        userId,
        fromUserId,
        now,
      ]);
      await tx.run("INSERT INTO friendships (user_id, friend_id, created_at) VALUES ($1, $2, $3)", [
        fromUserId,
        userId,
        now,
      ]);
    });
    await notify(fromUserId, "friend_accepted", userId, "accepted your friend request.");
    return { status: "accepted", replay: false };
  }

  async function declineRequest(userId, fromUserId) {
    assertUser(userId, "userId");
    assertUser(fromUserId, "fromUserId");
    const request = await requestBetween(fromUserId, userId, userId, fromUserId);
    if (!request || request.status !== "pending" || request.to_user_id !== userId) {
      throw new SocialError("request_not_found", "No pending request from that user.", 404);
    }
    await db.run("UPDATE friend_requests SET status = 'declined' WHERE from_user_id = $1 AND to_user_id = $2", [
      fromUserId,
      userId,
    ]);
    return { status: "declined", replay: false };
  }

  async function listRequests(userId) {
    assertUser(userId, "userId");
    const incoming = await db.all(
      `SELECT u.id, u.username, u.display_name, u.avatar_url, u.level, r.created_at
       FROM friend_requests r JOIN users u ON u.id = r.from_user_id
       WHERE r.to_user_id = $1 AND r.status = 'pending' ORDER BY r.created_at DESC`,
      [userId],
    );
    const outgoing = await db.all(
      `SELECT u.id, u.username, u.display_name, u.avatar_url, u.level, r.created_at
       FROM friend_requests r JOIN users u ON u.id = r.to_user_id
       WHERE r.from_user_id = $1 AND r.status = 'pending' ORDER BY r.created_at DESC`,
      [userId],
    );
    return { incoming: incoming.map(withTime), outgoing: outgoing.map(withTime) };
  }

  async function listFriends(userId) {
    assertUser(userId, "userId");
    const rows = await db.all(
      `SELECT u.id, u.username, u.display_name, u.avatar_url, u.level, f.created_at
       FROM friendships f JOIN users u ON u.id = f.friend_id
       WHERE f.user_id = $1 ORDER BY f.created_at DESC`,
      [userId],
    );
    return { items: rows.map(withTime) };
  }

  async function removeFriend(userId, friendId) {
    assertUser(userId, "userId");
    assertUser(friendId, "friendId");
    await db.run(
      "DELETE FROM friendships WHERE (user_id = $1 AND friend_id = $2) OR (user_id = $3 AND friend_id = $4)",
      [userId, friendId, friendId, userId],
    );
    return { ok: true };
  }

  async function block(userId, targetId) {
    assertUser(userId, "userId");
    assertUser(targetId, "targetId");
    if (userId === targetId) throw new SocialError("cannot_block_self", "You cannot block yourself.");
    await requireUser(targetId);

    await db.transaction(async (tx) => {
      await tx.run("INSERT INTO blocks (user_id, blocked_id, created_at) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING", [
        userId,
        targetId,
        Date.now(),
      ]);
      await tx.run(
        "DELETE FROM friendships WHERE (user_id = $1 AND friend_id = $2) OR (user_id = $3 AND friend_id = $4)",
        [userId, targetId, targetId, userId],
      );
      await tx.run(
        `DELETE FROM friend_requests
         WHERE (from_user_id = $1 AND to_user_id = $2) OR (from_user_id = $3 AND to_user_id = $4)`,
        [userId, targetId, targetId, userId],
      );
    });
    return { ok: true };
  }

  async function unblock(userId, targetId) {
    assertUser(userId, "userId");
    assertUser(targetId, "targetId");
    await db.run("DELETE FROM blocks WHERE user_id = $1 AND blocked_id = $2", [userId, targetId]);
    return { ok: true };
  }

  async function listBlocks(userId) {
    assertUser(userId, "userId");
    const rows = await db.all(
      `SELECT u.id, u.username, u.display_name, u.avatar_url, u.level, b.created_at
       FROM blocks b JOIN users u ON u.id = b.blocked_id
       WHERE b.user_id = $1 ORDER BY b.created_at DESC`,
      [userId],
    );
    return { items: rows.map(withTime) };
  }

  async function report(userId, targetId, reason) {
    assertUser(userId, "userId");
    assertUser(targetId, "targetId");
    if (typeof reason !== "string" || reason.trim().length === 0 || reason.length > REASON_MAX) {
      throw new SocialError("invalid_reason", `reason must be 1-${REASON_MAX} characters.`);
    }
    await db.run("INSERT INTO reports (reporter_id, target_id, reason, created_at) VALUES ($1, $2, $3, $4)", [
      userId,
      targetId,
      reason.trim(),
      Date.now(),
    ]);
    return { ok: true };
  }

  async function requireUser(userId) {
    if (!(await userCard(userId))) throw new SocialError("invalid_user", "Unknown user.", 404);
  }

  function notify(notifiedUserId, kind, actorId, body) {
    return insertNotification(`ntf_${randomUUID()}`, notifiedUserId, kind, actorId, body, Date.now());
  }

  return {
    sendRequest,
    acceptRequest,
    declineRequest,
    listRequests,
    listFriends,
    removeFriend,
    block,
    unblock,
    listBlocks,
    report,
  };
}

function withTime(row) {
  return {
    id: row.id,
    username: row.username,
    displayName: row.display_name,
    avatarUrl: row.avatar_url,
    level: row.level,
    createdAt: row.created_at,
  };
}

function assertUser(value, field) {
  if (typeof value !== "string" || value.length === 0 || value.length > 64) {
    throw new SocialError("invalid_user", `${field} is not a valid user id.`);
  }
}
