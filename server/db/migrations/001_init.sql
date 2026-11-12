-- WePlay platform schema, migration 001.
--
-- Convention: ids are TEXT, timestamps are BIGINT epoch milliseconds,
-- booleans are BOOLEAN, money-adjacent amounts are INTEGER and never float.
-- `seq` columns are identity sequences used as a stable insertion-order
-- tiebreak for keyset pagination (created_at alone collides within a ms).
--
-- Ownership boundaries in this database:
--   this migration owns  -> identity, settings, sessions, social graph,
--                           direct messages, notifications, room metadata, support
--   migration 002 owns   -> the economy tables, converted from the economy
--                           module's schema (wallets, gifts, coin_packs,
--                           transactions, ledger)
--
-- Deliberately absent: live/ephemeral state. Presence, voice seats, mic and
-- speaking state belong to the running room (LiveKit), and message media bytes
-- belong to object storage — only their URLs are referenced here.

-- ---------------------------------------------------------------------------
-- Identity and profile
-- ---------------------------------------------------------------------------

CREATE TABLE users (
  id                  TEXT PRIMARY KEY,
  username            TEXT NOT NULL UNIQUE,
  username_changed_at BIGINT NOT NULL DEFAULT 0,
  display_name        TEXT NOT NULL,
  password_hash       TEXT,
  has_password        BOOLEAN NOT NULL DEFAULT FALSE,
  provider            TEXT NOT NULL DEFAULT 'username',
  phone               TEXT UNIQUE,
  email               TEXT UNIQUE,
  phone_verified      BOOLEAN NOT NULL DEFAULT FALSE,
  email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
  level               INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1),
  bio                 TEXT NOT NULL DEFAULT '',
  gender              TEXT NOT NULL DEFAULT '',
  dob                 BIGINT,
  avatar_url          TEXT,
  created_at          BIGINT NOT NULL,
  updated_at          BIGINT NOT NULL
);

-- The wallet balance is NOT a column here: it is economy's wallets.balance,
-- derived from the append-only ledger and reconciled there.

CREATE TABLE user_settings (
  user_id              TEXT PRIMARY KEY REFERENCES users(id),
  dm_permission        BOOLEAN NOT NULL DEFAULT TRUE,
  invite_permission    BOOLEAN NOT NULL DEFAULT TRUE,
  show_online          BOOLEAN NOT NULL DEFAULT TRUE,
  show_gifts           BOOLEAN NOT NULL DEFAULT TRUE,
  private_account      BOOLEAN NOT NULL DEFAULT FALSE,
  dnd_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
  dnd_start_minute     INTEGER NOT NULL DEFAULT 0 CHECK (dnd_start_minute BETWEEN 0 AND 1439),
  dnd_end_minute       INTEGER NOT NULL DEFAULT 0 CHECK (dnd_end_minute BETWEEN 0 AND 1439),
  notify_invites       BOOLEAN NOT NULL DEFAULT TRUE,
  notify_messages      BOOLEAN NOT NULL DEFAULT TRUE,
  notify_gifts         BOOLEAN NOT NULL DEFAULT TRUE,
  notify_followers     BOOLEAN NOT NULL DEFAULT TRUE,
  notify_announcements BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------------
-- Sessions (server-issued; the client caches only what it is handed)
-- ---------------------------------------------------------------------------

CREATE TABLE device_sessions (
  id           TEXT PRIMARY KEY,
  user_id      TEXT NOT NULL REFERENCES users(id),
  token_hash   TEXT NOT NULL UNIQUE,
  provider     TEXT NOT NULL,
  identifier   TEXT NOT NULL,
  created_at   BIGINT NOT NULL,
  last_seen_at BIGINT NOT NULL,
  revoked_at   BIGINT
);

CREATE INDEX idx_sessions_user ON device_sessions (user_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Social graph
-- ---------------------------------------------------------------------------

CREATE TABLE friendships (
  user_id    TEXT NOT NULL REFERENCES users(id),
  friend_id  TEXT NOT NULL REFERENCES users(id),
  created_at BIGINT NOT NULL,
  PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE friend_requests (
  from_user_id TEXT NOT NULL REFERENCES users(id),
  to_user_id   TEXT NOT NULL REFERENCES users(id),
  status       TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
  created_at   BIGINT NOT NULL,
  PRIMARY KEY (from_user_id, to_user_id)
);

CREATE TABLE blocks (
  user_id    TEXT NOT NULL REFERENCES users(id),
  blocked_id TEXT NOT NULL REFERENCES users(id),
  created_at BIGINT NOT NULL,
  PRIMARY KEY (user_id, blocked_id)
);

CREATE TABLE reports (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reporter_id TEXT NOT NULL REFERENCES users(id),
  target_id   TEXT NOT NULL,
  reason      TEXT NOT NULL,
  created_at  BIGINT NOT NULL
);

-- ---------------------------------------------------------------------------
-- Direct messaging
-- ---------------------------------------------------------------------------

-- user_a_id is the lexicographically smaller of the two ids, so a pair of
-- users has exactly one chat row no matter who opened it first.
CREATE TABLE chats (
  id         TEXT PRIMARY KEY,
  user_a_id  TEXT NOT NULL REFERENCES users(id),
  user_b_id  TEXT NOT NULL REFERENCES users(id),
  created_at BIGINT NOT NULL,
  UNIQUE (user_a_id, user_b_id),
  CHECK (user_a_id < user_b_id)
);

CREATE TABLE chat_members (
  chat_id      TEXT NOT NULL REFERENCES chats(id),
  user_id      TEXT NOT NULL REFERENCES users(id),
  pinned       BOOLEAN NOT NULL DEFAULT FALSE,
  muted        BOOLEAN NOT NULL DEFAULT FALSE,
  last_read_at BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (chat_id, user_id)
);

-- gift_id resolves against the economy gift catalogue; it carries no foreign
-- key because the catalogue is owned by the economy module in this database.
-- gift_transaction_id is the completed economy transaction the gift message
-- rides on; UNIQUE means one transaction can produce at most one message.
CREATE TABLE messages (
  id                  TEXT PRIMARY KEY,
  chat_id             TEXT NOT NULL REFERENCES chats(id),
  sender_id           TEXT NOT NULL REFERENCES users(id),
  kind                TEXT NOT NULL CHECK (kind IN ('text', 'gift', 'image', 'voice')),
  body                TEXT NOT NULL DEFAULT '',
  gift_id             TEXT,
  gift_transaction_id TEXT UNIQUE,
  gift_cost           INTEGER NOT NULL DEFAULT 0 CHECK (gift_cost >= 0),
  media_url           TEXT,
  duration_ms         INTEGER NOT NULL DEFAULT 0 CHECK (duration_ms >= 0),
  recalled            BOOLEAN NOT NULL DEFAULT FALSE,
  seq                 BIGINT GENERATED ALWAYS AS IDENTITY,
  created_at          BIGINT NOT NULL
);

CREATE INDEX idx_messages_chat ON messages (chat_id, created_at DESC, seq DESC);

-- ---------------------------------------------------------------------------
-- Notifications (one row per recipient event)
-- ---------------------------------------------------------------------------

CREATE TABLE notifications (
  id         TEXT PRIMARY KEY,
  user_id    TEXT NOT NULL REFERENCES users(id),
  kind       TEXT NOT NULL CHECK (kind IN ('friend_request', 'friend_accepted', 'message', 'follow', 'gift', 'room_invite')),
  actor_id   TEXT,
  room_id    TEXT,
  body       TEXT NOT NULL,
  read       BOOLEAN NOT NULL DEFAULT FALSE,
  seq        BIGINT GENERATED ALWAYS AS IDENTITY,
  created_at BIGINT NOT NULL
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC, seq DESC);

-- ---------------------------------------------------------------------------
-- Voice rooms: persistent metadata only.
-- Seats, mic state, and speaking indicators are live room state and are never
-- stored; room chat rides the LiveKit data channel and is not retained here.
-- ---------------------------------------------------------------------------

CREATE TABLE rooms (
  id               TEXT PRIMARY KEY,
  title            TEXT NOT NULL,
  host_id          TEXT NOT NULL REFERENCES users(id),
  tag              TEXT NOT NULL DEFAULT '',
  is_private       BOOLEAN NOT NULL DEFAULT FALSE,
  max_participants INTEGER NOT NULL DEFAULT 24 CHECK (max_participants > 0),
  seq              BIGINT GENERATED ALWAYS AS IDENTITY,
  created_at       BIGINT NOT NULL,
  closed_at        BIGINT
);

CREATE INDEX idx_rooms_open ON rooms (closed_at, created_at DESC, seq DESC);

-- ---------------------------------------------------------------------------
-- Support
-- ---------------------------------------------------------------------------

CREATE TABLE support_tickets (
  id         TEXT PRIMARY KEY,
  user_id    TEXT NOT NULL REFERENCES users(id),
  category   TEXT NOT NULL,
  message    TEXT NOT NULL,
  image_url  TEXT,
  seq        BIGINT GENERATED ALWAYS AS IDENTITY,
  created_at BIGINT NOT NULL
);
