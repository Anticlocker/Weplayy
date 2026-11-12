# WePlay server

Node services over one shared PostgreSQL database, self-hosted on the VPS.
The only npm dependency is the `pg` driver (`npm install` once in `server/`).
Requires Node >= 22.

## Modules

| Module | Owns |
|---|---|
| `db/` | Shared pool, migrations, query facade (see `db/README.md`) |
| `identity/` | Accounts, password sign-in, device sessions, profile, settings |
| `economy/` | Wallet, gifts, coin packs, top-up intents, ledger (see `economy/README.md`) |
| `social/` | Friend requests, friendships, blocks, reports |
| `messaging/` | Chats, messages, read state; gift messages ride completed economy transactions |
| `notifications/` | Per-user notification feed; rows are written by owning features |
| `rooms/` | Voice room metadata (title, host, capacity, open/closed) |
| `support/` | Bug reports from the app |
| `server.mjs` | One process mounting every module over plain node:http |
| `token-server.mjs` | Standalone LiveKit join-token minter (no database) |

## Running

```bash
# once: provision the database
createdb weplay   # or: CREATE DATABASE weplay;

# boot
cd server && npm install
DATABASE_URL=postgresql://weplay:SECRET@127.0.0.1:5432/weplay \
PORT=8080 node server/server.mjs
```

Migrations and the gift catalogue apply automatically on boot. Put TLS in
front (Caddy/nginx); the Android app refuses plain http.

Health check: `GET /healthz`.

## Auth

All routes except `POST /identity/signup` and `POST /identity/signin` require
`Authorization: Bearer <token>`. Tokens are issued by sign-in/up and stored
server-side as SHA-256 hashes; sign-out revokes them.

## Conventions

- JSON errors: `{ "error": { "code": "...", "message": "..." } }`
- Lists paginate with `?limit=&cursor=` and answer
  `{ items: [...], nextCursor }` (cursor is opaque; pass it back as-is).
- Ids are `TEXT` with module prefixes (`WP-`, `msg_`, `tx_`, `chat_`,
  `room_`, `ntf_`, `sup_`); timestamps are epoch milliseconds.
- Money and gift sending stay in `economy/`; messaging never moves coins.

## Tests

Tests need a PostgreSQL server; point `TEST_DATABASE_URL` at it (each module
creates its own `weplay_test_<module>` database automatically):

```bash
# quick local database, e.g. via Docker:
docker run -d --name weplay-pg-test -e POSTGRES_USER=weplay -e POSTGRES_PASSWORD=weplay \
  -p 127.0.0.1:54329:5432 postgres:17-alpine

cd server/db            && TEST_DATABASE_URL=postgresql://weplay:weplay@127.0.0.1:54329/postgres node --test
cd server/identity      && TEST_DATABASE_URL=... node --test
cd server/economy       && TEST_DATABASE_URL=... node --test
cd server/social        && TEST_DATABASE_URL=... node --test
cd server/messaging     && TEST_DATABASE_URL=... node --test
cd server/notifications && TEST_DATABASE_URL=... node --test
cd server/rooms         && TEST_DATABASE_URL=... node --test
cd server/support       && TEST_DATABASE_URL=... node --test
```

## Pending dependencies

- Media upload endpoint (chat images, voice notes, avatars) once object
  storage is provisioned; today clients pass a `mediaUrl` they already have.
- Payment provider webhook calling `confirmTopUp`/`failTopUp` after signature
  verification (see `economy/README.md`).
- LiveKit room lifecycle: opening/closing `rooms` rows when real rooms start
  and empty out (the token server is stateless today).
- Push delivery (FCM) for message and notification fan-out to killed apps.
