# Economy service

Server-authoritative wallet, gifts, and top-up state. Balances are never accepted from the client.

## Boundaries

This module does not implement authentication. The HTTP layer must resolve the caller and pass
`userId` into the handlers. Missing `userId` returns `401 unauthenticated`.

It also does not talk to a payment provider. `confirmTopUp` must only be called after a webhook
signature has been verified by the provider adapter.

## Data model

- `wallets` — cached balance per user. `CHECK (balance >= 0)`.
- `ledger` — append-only deltas. One row per user per transaction, enforced by
  `UNIQUE (transaction_id, user_id)`.
- `transactions` — auditable state: `pending`, `paid`, `completed`, `failed`, `refunded`.
- `gifts` and `coin_packs` — server-owned catalogue, refreshed from `catalog.mjs` on boot.

The client never sends a price or a coin amount. It sends `giftId` or `packId`, and the server
looks up the value.

These tables ship in the shared migration set (`server/db/migrations/002_economy.sql`, converted
from this module's original schema when WePlay moved to PostgreSQL) and remain owned by this
module: only economy code writes wallets, ledger, and transactions.

## Endpoints

| Method | Path | Body | Result |
|---|---|---|---|
| GET | `/economy/wallet` | — | `{ userId, balance, updatedAt }` |
| GET | `/economy/gifts` | — | `{ items: [{ id, name, description, cost, icon, tint }] }` |
| GET | `/economy/packs` | — | `{ items: [{ id, coins, bonus, totalCoins, priceMinor, currency }] }` |
| GET | `/economy/transactions?limit=&cursor=` | — | `{ items: [...], nextCursor }` |
| POST | `/economy/gifts/send` | `{ toUserId, giftId, idempotencyKey }` | `201` / `200` replay |
| POST | `/economy/topup/intents` | `{ packId, idempotencyKey }` | `201` / `200` replay |

`priceMinor` is the smallest currency unit, so `9900` means ₹99.00.

## Errors

```json
{ "error": { "code": "insufficient_funds", "message": "Not enough coins for this gift." } }
```

| Code | HTTP | When |
|---|---|---|
| `unauthenticated` | 401 | No resolved user |
| `invalid_user` | 400 | Bad user id |
| `invalid_idempotency_key` | 400 | Key outside `[A-Za-z0-9:_-]{8,128}` |
| `gift_not_found` | 404 | Unknown or inactive gift |
| `pack_not_found` | 404 | Unknown or inactive pack |
| `cannot_gift_self` | 400 | `toUserId` equals the caller |
| `insufficient_funds` | 409 | Balance below gift cost |
| `invalid_transition` | 409 | Confirming or failing a closed transaction |
| `transaction_not_found` | 404 | Unknown transaction id |

## Invariants

1. `wallet.balance` always equals `SUM(ledger.delta)` for that user. `reconcile(userId)` asserts it.
2. A gift is one transaction: debit sender, credit receiver, both inside a single PostgreSQL
   transaction with both wallet rows locked `FOR UPDATE` in a deterministic order, so concurrent
   gifts queue instead of deadlocking or overdrawing.
3. `idempotencyKey` is unique. Repeating a request returns the original transaction and never
   moves coins twice.
4. A top-up only credits after `confirmTopUp`, which is reachable only from a verified webhook.
5. Coins are integers. No floating point anywhere in the money path.

## Tests

```bash
TEST_DATABASE_URL=postgresql://weplay:weplay@127.0.0.1:54329/postgres node --test
```

## Pending dependencies

- Auth middleware from the shared platform layer must populate `userId`.
- Payment provider is not chosen yet, so `payment.provider` stays `null` and intents remain
  `pending`. Do not report a top-up as successful before `confirmTopUp` runs.
- Refunds are modelled as a status but not implemented yet.
