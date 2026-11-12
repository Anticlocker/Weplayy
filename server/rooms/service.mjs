// Voice rooms: persistent metadata only.
//
// Live room state — seats, mic switches, speaking indicators, who is in the
// room right now — belongs to the running LiveKit room and is never stored
// here. Live participant counts therefore come from LiveKit, not from this
// module; the database only knows a room exists, who hosts it, and whether it
// is still open.

import { randomUUID } from "node:crypto";

const TITLE_MAX = 60;
const TAG_MAX = 20;
const PARTICIPANTS_CAP = 24;

export class RoomsError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export function createRoomsService(db) {
  const hostCard = db.get.bind(db, "SELECT id, username, display_name, avatar_url, level FROM users WHERE id = $1");

  async function create(hostId, { title, tag, isPrivate, maxParticipants } = {}) {
    assertUser(hostId, "hostId");
    if (typeof title !== "string" || title.trim().length === 0 || title.length > TITLE_MAX) {
      throw new RoomsError("invalid_title", `title must be 1-${TITLE_MAX} characters.`);
    }
    if (tag !== undefined && (typeof tag !== "string" || tag.length > TAG_MAX)) {
      throw new RoomsError("invalid_tag", `tag must be at most ${TAG_MAX} characters.`);
    }
    let capacity = PARTICIPANTS_CAP;
    if (maxParticipants !== undefined) {
      if (!Number.isInteger(maxParticipants) || maxParticipants < 1 || maxParticipants > PARTICIPANTS_CAP) {
        throw new RoomsError("invalid_capacity", `maxParticipants must be 1-${PARTICIPANTS_CAP}.`);
      }
      capacity = maxParticipants;
    }

    const id = `room_${randomUUID()}`;
    await db.run(
      `INSERT INTO rooms (id, title, host_id, tag, is_private, max_participants, created_at, closed_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, NULL)`,
      [id, title.trim(), hostId, tag ?? "", isPrivate === true, capacity, Date.now()],
    );
    return toDto(await db.get("SELECT * FROM rooms WHERE id = $1", [id]), await hostCard(hostId));
  }

  async function listOpen({ limit } = {}) {
    const take = clampLimit(limit);
    const rows = await db.all(
      `SELECT r.*, u.id AS host_id, u.username AS host_username, u.display_name AS host_display_name,
              u.avatar_url AS host_avatar_url, u.level AS host_level
       FROM rooms r JOIN users u ON u.id = r.host_id
       WHERE r.closed_at IS NULL
       ORDER BY r.created_at DESC, r.seq DESC LIMIT $1`,
      [take],
    );
    return { items: rows.map(toListDto) };
  }

  async function close(roomId, userId) {
    assertUser(userId, "userId");
    if (typeof roomId !== "string" || roomId.length === 0) {
      throw new RoomsError("invalid_room", "A room id is required.");
    }
    const room = await db.get("SELECT * FROM rooms WHERE id = $1", [roomId]);
    if (!room) throw new RoomsError("room_not_found", "Unknown room.", 404);
    if (room.host_id !== userId) {
      throw new RoomsError("not_allowed", "Only the host can close a room.", 403);
    }
    const result = await db.run("UPDATE rooms SET closed_at = $1 WHERE id = $2 AND closed_at IS NULL", [
      Date.now(),
      roomId,
    ]);
    return { room: toDto(await db.get("SELECT * FROM rooms WHERE id = $1", [roomId]), await hostCard(room.host_id)), replay: result.changes === 0 };
  }

  return { create, listOpen, close };
}

function toDto(row, host) {
  return {
    id: row.id,
    title: row.title,
    tag: row.tag,
    isPrivate: !!row.is_private,
    maxParticipants: row.max_participants,
    host: host
      ? {
          id: host.id,
          username: host.username,
          displayName: host.display_name,
          avatarUrl: host.avatar_url,
          level: host.level,
        }
      : { id: row.host_id },
    createdAt: row.created_at,
    closedAt: row.closed_at,
  };
}

function toListDto(row) {
  return {
    id: row.id,
    title: row.title,
    tag: row.tag,
    isPrivate: !!row.is_private,
    maxParticipants: row.max_participants,
    host: {
      id: row.host_id,
      username: row.host_username,
      displayName: row.host_display_name,
      avatarUrl: row.host_avatar_url,
      level: row.host_level,
    },
    createdAt: row.created_at,
  };
}

function assertUser(value, field) {
  if (typeof value !== "string" || value.length === 0 || value.length > 64) {
    throw new RoomsError("invalid_user", `${field} is not a valid user id.`);
  }
}

function clampLimit(value) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) return 20;
  return Math.min(parsed, 100);
}
