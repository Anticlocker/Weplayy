// WePlay API server: one Node process exposing every feature module.
//
// Mounts the identity, economy, social, messaging, notifications, rooms, and
// support dispatchers over plain node:http. Bearer tokens are resolved once
// here and passed down as `userId`; the dispatchers stay pure and testable.
// PostgreSQL is the single shared database; the only npm dependency is the
// `pg` driver.
//
// Run (VPS):
//   DATABASE_URL=postgresql://weplay:SECRET@127.0.0.1:5432/weplay \
//   PORT=8080 node server/server.mjs
//
// Put TLS in front (Caddy/nginx) — the app talks https only.

import { createServer } from "node:http";
import { openEconomyDatabase } from "./economy/db.mjs";
import { createIdentityService } from "./identity/service.mjs";
import { handleIdentityRequest } from "./identity/routes.mjs";
import { createEconomyService } from "./economy/service.mjs";
import { handleEconomyRequest } from "./economy/routes.mjs";
import { createSocialService } from "./social/service.mjs";
import { handleSocialRequest } from "./social/routes.mjs";
import { createMessagingService } from "./messaging/service.mjs";
import { handleMessagingRequest } from "./messaging/routes.mjs";
import { createNotificationsService } from "./notifications/service.mjs";
import { handleNotificationsRequest } from "./notifications/routes.mjs";
import { createRoomsService } from "./rooms/service.mjs";
import { handleRoomsRequest } from "./rooms/routes.mjs";
import { createSupportService } from "./support/service.mjs";
import { handleSupportRequest } from "./support/routes.mjs";

const PORT = Number(process.env.PORT ?? 8080);
const DATABASE_URL = process.env.DATABASE_URL;
const BODY_LIMIT = 1024 * 1024;

if (!DATABASE_URL) {
  console.error("DATABASE_URL is required, e.g. postgresql://weplay:SECRET@127.0.0.1:5432/weplay");
  process.exit(1);
}

// Economy's boot opener also applies the shared migrations and refreshes the
// gift/coin-pack catalogue, so one call brings the whole database up to date.
const db = await openEconomyDatabase(DATABASE_URL);

const services = {
  "/identity": createIdentityService(db),
  "/economy": createEconomyService(db),
  "/social": createSocialService(db),
  "/messaging": createMessagingService(db),
  "/notifications": createNotificationsService(db),
  "/rooms": createRoomsService(db),
  "/support": createSupportService(db),
};

const handlers = {
  "/identity": handleIdentityRequest,
  "/economy": handleEconomyRequest,
  "/social": handleSocialRequest,
  "/messaging": handleMessagingRequest,
  "/notifications": handleNotificationsRequest,
  "/rooms": handleRoomsRequest,
  "/support": handleSupportRequest,
};

// The only routes reachable without a bearer token.
const PUBLIC_PATHS = new Set(["/identity/signup", "/identity/signin"]);

createServer(async (req, res) => {
  try {
    const url = new URL(req.url ?? "/", `http://${req.headers.host ?? "localhost"}`);
    const path = normalize(url.pathname);

    if (path === "/healthz") return json(res, 200, { ok: true });

    const prefix = Object.keys(services).find((p) => path === p || path.startsWith(`${p}/`));
    if (!prefix) {
      return json(res, 404, { error: { code: "not_found", message: "Unknown route." } });
    }

    let userId = null;
    if (!PUBLIC_PATHS.has(path)) {
      userId = await services["/identity"].resolveToken(bearer(req));
      if (!userId) {
        return json(res, 401, { error: { code: "unauthenticated", message: "Authentication required." } });
      }
    }

    const outcome = await handlers[prefix]({
      service: services[prefix],
      method: req.method,
      path,
      userId,
      query: Object.fromEntries(url.searchParams),
      body: await readBody(req),
    });
    return json(res, outcome.status, outcome.body);
  } catch (error) {
    if (error && typeof error.status === "number" && typeof error.code === "string") {
      return json(res, error.status, { error: { code: error.code, message: error.message } });
    }
    console.error("unhandled request error:", error);
    return json(res, 500, { error: { code: "internal", message: "Something went wrong." } });
  }
}).listen(PORT, () => {
  console.log(`weplay api listening on ${PORT}`);
});

function normalize(pathname) {
  const trimmed = pathname.replace(/\/+$/, "");
  return trimmed.length > 0 ? trimmed : "/";
}

function bearer(req) {
  const header = req.headers.authorization ?? "";
  return header.startsWith("Bearer ") ? header.slice(7) : "";
}

async function readBody(req) {
  if (req.method === "GET" || req.method === "HEAD") return null;
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > BODY_LIMIT) {
      throw requestError("payload_too_large", "Request body is too large.", 413);
    }
    chunks.push(chunk);
  }
  if (chunks.length === 0) return null;
  try {
    const parsed = JSON.parse(Buffer.concat(chunks).toString("utf8"));
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    throw requestError("invalid_body", "Request body must be JSON.", 400);
  }
}

function requestError(code, message, status) {
  const error = new Error(message);
  error.code = code;
  error.status = status;
  return error;
}

function json(res, status, body) {
  res.writeHead(status, { "content-type": "application/json" });
  res.end(JSON.stringify(body));
}
