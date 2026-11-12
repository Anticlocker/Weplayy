// Test-database helpers shared by every module's tests.
//
// Each module's tests use their own database (named after the module) so
// files can run in parallel without truncating each other's data. Missing
// databases are created on first use against the admin "postgres" database.

import pg from "pg";
import { openDatabase, migrate } from "./db.mjs";

const DATA_TABLES = `SELECT tablename FROM pg_tables
                     WHERE schemaname = 'public' AND tablename <> 'schema_migrations'`;

export async function openTestDatabase(module) {
  const adminUrl = adminDatabaseUrl();
  const url = adminUrl.replace(/\/[^/]+$/, `/${dbName(module)}`);

  try {
    const db = openDatabase(url);
    await migrate(db);
    return db;
  } catch (error) {
    if (error.code !== "3D000" && !/does not exist/i.test(String(error.message))) throw error;
    const admin = new pg.Pool({ connectionString: adminUrl });
    try {
      await admin.query(`CREATE DATABASE ${dbName(module)}`);
    } finally {
      await admin.end();
    }
    const db = openDatabase(url);
    await migrate(db);
    return db;
  }
}

// Wipe every data table between tests, keeping migrations applied.
export async function clearData(db) {
  const { rows } = await db.query(DATA_TABLES);
  const tables = rows.map((row) => `"${row.tablename}"`).join(", ");
  if (tables) await db.run(`TRUNCATE ${tables} RESTART IDENTITY CASCADE`);
}

// The base URL each module's own database name is derived from.
export function testDatabaseUrl(module) {
  return adminDatabaseUrl().replace(/\/[^/]+$/, `/${dbName(module)}`);
}

function adminDatabaseUrl() {
  const base = new URL(process.env.TEST_DATABASE_URL ?? "postgresql://weplay:weplay@127.0.0.1:54329/postgres");
  base.pathname = `/${base.pathname.split("/").filter(Boolean)[0] ?? "postgres"}`;
  return base.toString();
}

function dbName(module) {
  return `weplay_test_${module.replace(/[^a-z0-9_]/gi, "_")}`;
}
