# Shared database

One PostgreSQL database backs every WePlay service, self-hosted on the VPS.
This module is the only way services reach it: it opens the pool, applies
migrations, and hands back a small query facade.

## Rules

1. **One database, no exceptions.** Never give a service its own database —
   joins and transactional integrity across databases do not exist. Every
   service points `DATABASE_URL` at the same database.
2. **Never edit a shipped migration.** Add the next numbered `.sql` in
   `migrations/`. `schema_migrations` tracks what has been applied; each
   migration runs once, inside a transaction (PostgreSQL DDL is transactional).
3. **No live state.** Presence, voice-room seats, mic/speaking state, and
   typing indicators are ephemeral — they live in the running process or in
   LiveKit, never in this database.
4. **No media bytes.** Images, voice notes, and avatars go to object storage;
   only their URLs are stored here.
5. **Economy stays economy's.** The economy tables ship as migration `002`
   (converted from the economy module's original schema) and remain owned by
   the economy module: only it writes wallets, ledger, and transactions.

## Usage

```js
import { openDatabase, migrate } from "../db/db.mjs";

const db = openDatabase(process.env.DATABASE_URL);
await migrate(db);
```

The facade every service receives:

| Call | Behaviour |
|---|---|
| `get(sql, params)` | first row or `undefined` |
| `all(sql, params)` | array of rows |
| `run(sql, params)` | `{ changes, rows }` |
| `transaction(async (tx) => …)` | same calls on one client; commits on success, rolls back on throw |

Params accept both `get(sql, [a, b])` and `get(sql, a, b)` — the second form
lets services pre-bind SQL with `.bind` and pass scalars at call sites.
BIGINT and NUMERIC results are parsed as numbers (epoch milliseconds, identity
sequences, and coin sums all fit a JS number).

## What the migrations own

| Migration | Tables |
|---|---|
| `001_init.sql` | `users`, `user_settings`, `device_sessions`, `friendships`, `friend_requests`, `blocks`, `reports`, `chats`, `chat_members`, `messages`, `notifications`, `rooms`, `support_tickets` |
| `002_economy.sql` | `wallets`, `gifts`, `coin_packs`, `transactions`, `ledger` |

Conventions: `TEXT` ids with module prefixes, `BIGINT` epoch-millisecond
timestamps, `BOOLEAN` flags, integer coin amounts (never floats). `seq`
identity columns give keyset pagination a stable insertion-order tiebreak.
`chats.user_a_id` is the lexicographically smaller user id so a pair has
exactly one chat row.

## Requirements

Node >= 22, the `pg` driver (`server/package.json`), and a reachable
PostgreSQL. Connection comes from `DATABASE_URL`
(`postgresql://user:pass@host:5432/weplay`).

## Tests

```bash
TEST_DATABASE_URL=postgresql://weplay:weplay@127.0.0.1:54329/postgres node --test
```

Each module's tests create and use their own database
(`weplay_test_<module>`) against the same server, so files can run in
parallel. Missing databases are created on first use.

## Backups

Standard PostgreSQL tooling: `pg_dump` on a schedule, or managed snapshots
when the VPS provider offers them. Decide when the first real deployment
lands.
