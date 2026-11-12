// Economy module database boot on the shared PostgreSQL database.
//
// The economy tables live in the shared migration set (server/db/migrations/
// 002_economy.sql) and are converted from this module's original schema. What
// remains module-owned here is the catalogue seed: gifts and coin packs are
// server-owned and refreshed on every boot.

import { GIFTS, COIN_PACKS } from "./catalog.mjs";
import { openDatabase, migrate } from "../db/db.mjs";

export async function openEconomyDatabase(connectionString) {
  const db = openDatabase(connectionString);
  await migrate(db);
  await seedCatalogue(db);
  return db;
}

export async function seedCatalogue(db) {
  await db.transaction(async (tx) => {
    for (const [index, gift] of GIFTS.entries()) {
      await tx.run(
        `INSERT INTO gifts (id, name, description, cost, icon, tint, sort)
         VALUES ($1, $2, $3, $4, $5, $6, $7)
         ON CONFLICT (id) DO UPDATE SET
           name = EXCLUDED.name, description = EXCLUDED.description, cost = EXCLUDED.cost,
           icon = EXCLUDED.icon, tint = EXCLUDED.tint, sort = EXCLUDED.sort`,
        [gift.id, gift.name, gift.description, gift.cost, gift.icon, gift.tint, index],
      );
    }

    for (const [index, pack] of COIN_PACKS.entries()) {
      await tx.run(
        `INSERT INTO coin_packs (id, coins, bonus, price_minor, sort)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (id) DO UPDATE SET
           coins = EXCLUDED.coins, bonus = EXCLUDED.bonus,
           price_minor = EXCLUDED.price_minor, sort = EXCLUDED.sort`,
        [pack.id, pack.coins, pack.bonus, pack.priceMinor, index],
      );
    }
  });
}
