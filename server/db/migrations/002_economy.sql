-- Economy tables, migration 002.
--
-- Converted from the economy module's own SQLite schema when WePlay moved to
-- PostgreSQL; the economy module owns these tables and their invariants:
--   1. wallet.balance always equals SUM(ledger.delta) per user (reconcile).
--   2. A gift debits the sender and credits the receiver in one transaction.
--   3. idempotency_key is unique; replays never move coins twice.
--   4. A top-up credits only after confirmTopUp, behind a verified webhook.
--   5. Coins are integers; no floating point anywhere in the money path.

CREATE TABLE wallets (
  user_id    TEXT PRIMARY KEY,
  balance    INTEGER NOT NULL DEFAULT 0 CHECK (balance >= 0),
  updated_at BIGINT NOT NULL
);

CREATE TABLE gifts (
  id          TEXT PRIMARY KEY,
  name        TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  cost        INTEGER NOT NULL CHECK (cost > 0),
  icon        TEXT NOT NULL DEFAULT '',
  tint        TEXT NOT NULL DEFAULT '',
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  sort        INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE coin_packs (
  id          TEXT PRIMARY KEY,
  coins       INTEGER NOT NULL CHECK (coins > 0),
  bonus       INTEGER NOT NULL DEFAULT 0 CHECK (bonus >= 0),
  price_minor INTEGER NOT NULL CHECK (price_minor > 0),
  currency    TEXT NOT NULL DEFAULT 'INR',
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  sort        INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE transactions (
  id              TEXT PRIMARY KEY,
  kind            TEXT NOT NULL CHECK (kind IN ('gift', 'topup')),
  status          TEXT NOT NULL CHECK (status IN ('pending', 'paid', 'completed', 'failed', 'refunded')),
  from_user_id    TEXT,
  to_user_id      TEXT,
  gift_id         TEXT,
  pack_id         TEXT,
  coins           INTEGER NOT NULL CHECK (coins > 0),
  price_minor     INTEGER,
  currency        TEXT,
  provider        TEXT,
  provider_ref    TEXT,
  idempotency_key TEXT UNIQUE,
  failure_reason  TEXT,
  created_at      BIGINT NOT NULL,
  updated_at      BIGINT NOT NULL
);

CREATE TABLE ledger (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id        TEXT NOT NULL,
  delta          INTEGER NOT NULL CHECK (delta <> 0),
  kind           TEXT NOT NULL CHECK (kind IN ('topup', 'gift_sent', 'gift_received')),
  transaction_id TEXT,
  created_at     BIGINT NOT NULL,
  UNIQUE (transaction_id, user_id)
);

CREATE INDEX idx_ledger_user ON ledger (user_id, id DESC);
CREATE INDEX idx_tx_from ON transactions (from_user_id, created_at DESC);
CREATE INDEX idx_tx_to ON transactions (to_user_id, created_at DESC);
