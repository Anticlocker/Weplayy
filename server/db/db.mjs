// Shared PostgreSQL database for every WePlay service.
//
// One database backs all services, self-hosted on the VPS. Connections come
// from a small pool configured through DATABASE_URL (or the standard PG*
// variables). Schema changes are numbered .sql files in ./migrations, applied
// exactly once inside a transaction and tracked in schema_migrations. Never
// edit a migration that shipped; add the next number instead.
//
// Every service receives the facade returned by openDatabase():
//   get(sql, params)            -> first row or undefined
//   all(sql, params)            -> array of rows
//   run(sql, params)            -> { changes, rows }
//   transaction(async (tx) => …) -> same three calls on one client, committed
//                                   on success, rolled back on throw
//
// What deliberately lives elsewhere:
//   - live room state (seats, mic, speaking) — held by LiveKit, never persisted
//   - media bytes (images, voice notes, avatars) — object storage; only URLs

import { readFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import pg from "pg";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const MIGRATIONS = join(HERE, "migrations");

// pg hands back BIGINT and NUMERIC as strings to dodge precision loss, but
// epoch milliseconds, identity sequences, and coin sums all fit a JS number
// comfortably. Parse them as numbers so the API stays numeric.
pg.types.setTypeParser(20, Number); // int8
pg.types.setTypeParser(1700, Number); // numeric

export function openDatabase(connectionString = process.env.DATABASE_URL) {
  if (!connectionString) {
    throw new Error("DATABASE_URL (or an explicit connection string) is required");
  }
  const pool = new pg.Pool({ connectionString, max: 10 });

  // Accept both styles: get(sql, [a, b]) and get(sql, a, b). The second form
  // lets services pre-bind SQL with .bind and pass plain scalars at call sites.
  const argsOf = (rest) => (rest.length === 1 && Array.isArray(rest[0]) ? rest[0] : rest);

  const fromClient = (client) => ({
    query: (text, ...rest) => client.query(text, argsOf(rest)),
    get: async (text, ...rest) => (await client.query(text, argsOf(rest))).rows[0],
    all: async (text, ...rest) => (await client.query(text, argsOf(rest))).rows,
    run: async (text, ...rest) => {
      const result = await client.query(text, argsOf(rest));
      return { changes: result.rowCount, rows: result.rows };
    },
  });

  const db = {
    pool,
    ...fromClient(pool),
    transaction: async (work) => {
      const client = await pool.connect();
      try {
        await client.query("BEGIN");
        const out = await work(fromClient(client));
        await client.query("COMMIT");
        return out;
      } catch (error) {
        try {
          await client.query("ROLLBACK");
        } catch {
          // the client is doomed anyway; release below surfaces the real error
        }
        throw error;
      } finally {
        client.release();
      }
    },
    close: () => pool.end(),
  };
  return db;
}

export async function migrate(db) {
  await db.run(
    `CREATE TABLE IF NOT EXISTS schema_migrations (
       version    INTEGER PRIMARY KEY,
       applied_at BIGINT NOT NULL
     )`,
  );
  const row = await db.get("SELECT COALESCE(MAX(version), 0) AS version FROM schema_migrations");
  const current = Number(row?.version ?? 0);

  const files = readdirSync(MIGRATIONS)
    .filter((name) => name.endsWith(".sql"))
    .sort();

  for (let index = 0; index < files.length; index += 1) {
    const version = index + 1;
    if (version <= current) continue;
    const sql = readFileSync(join(MIGRATIONS, files[index]), "utf8");
    await db.transaction(async (tx) => {
      await tx.run(sql);
      await tx.run("INSERT INTO schema_migrations (version, applied_at) VALUES ($1, $2)", [
        version,
        Date.now(),
      ]);
    });
  }
}
