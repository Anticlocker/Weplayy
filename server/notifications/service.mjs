// Notifications: one row per recipient event.
//
// Rows are inserted by the owning feature (social, messaging, rooms) at the
// moment the event happens; this module only reads them and marks them read.
// There is no write API here on purpose — notification content belongs to the
// feature that understands the event.

const MAX_PAGE = 100;

export class NotificationsError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createNotificationsService(db) {
  const markOne = db.run.bind(
    db,
    "UPDATE notifications SET read = TRUE WHERE id = $1 AND user_id = $2 AND read = FALSE",
  );
  const markAll = db.run.bind(db, "UPDATE notifications SET read = TRUE WHERE user_id = $1 AND read = FALSE");

  async function list(userId, { limit, cursor, unreadOnly } = {}) {
    assertUser(userId, "userId");
    const take = clampLimit(limit);
    const after = parseCursor(cursor);
    const filter = unreadOnly ? " AND read = FALSE" : "";

    const rows = after
      ? await db.all(
          `SELECT * FROM notifications
           WHERE user_id = $1${filter} AND (created_at < $2 OR (created_at = $2 AND seq < $3))
           ORDER BY created_at DESC, seq DESC LIMIT $4`,
          [userId, after.createdAt, after.seq, take + 1],
        )
      : await db.all(
          `SELECT * FROM notifications WHERE user_id = $1${filter}
           ORDER BY created_at DESC, seq DESC LIMIT $2`,
          [userId, take + 1],
        );

    const page = rows.slice(0, take);
    const last = page[page.length - 1];
    return {
      items: page.map(toDto),
      nextCursor: rows.length > take && last ? `${last.created_at}:${last.seq}` : null,
    };
  }

  async function markRead(userId, notificationId) {
    assertUser(userId, "userId");
    if (typeof notificationId !== "string" || notificationId.length === 0) {
      throw new NotificationsError("invalid_notification", "A notification id is required.");
    }
    const result = await markOne(notificationId, userId);
    if (result.changes === 0) {
      const exists = await db.get("SELECT 1 FROM notifications WHERE id = $1 AND user_id = $2", [
        notificationId,
        userId,
      ]);
      if (!exists) throw new NotificationsError("notification_not_found", "Unknown notification.", 404);
    }
    return { ok: true };
  }

  async function markAllRead(userId) {
    assertUser(userId, "userId");
    const result = await markAll(userId);
    return { updated: result.changes };
  }

  return { list, markRead, markAllRead };
}

function toDto(row) {
  return {
    id: row.id,
    kind: row.kind,
    actorId: row.actor_id,
    roomId: row.room_id,
    body: row.body,
    read: !!row.read,
    createdAt: row.created_at,
  };
}

function assertUser(value, field) {
  if (typeof value !== "string" || value.length === 0 || value.length > 64) {
    throw new NotificationsError("invalid_user", `${field} is not a valid user id.`);
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
  const seq = Number(value.slice(split + 1));
  if (!Number.isInteger(createdAt) || !Number.isInteger(seq)) return null;
  return { createdAt, seq };
}
