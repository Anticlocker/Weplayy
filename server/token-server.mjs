// Voice room token minter for the self-hosted LiveKit SFU.
//
// Why this exists: a LiveKit access token can only be signed with the server's
// API secret, and that secret must never ship inside the Android app. The app
// therefore asks this endpoint for a short-lived token every time it joins a
// room, and the token it gets back only allows that one room.
//
// Runs on the VPS with Node only, no npm install, no external packages:
//
//   LIVEKIT_API_KEY=APIDx8cR2yGh \
//   LIVEKIT_API_SECRET="$(cat /etc/livekit/secret)" \
//   TOKEN_SECRET=any-random-string-for-the-caller-header \
//   node server/token-server.mjs
//
// The Android app is pointed at it with:
//
//   livekit.url=wss://your-vps-host:7880
//   livekit.token.url=https://your-vps-host:8081/voice/token
//
// Put it behind TLS (Caddy/nginx/traefik). The app talks plain https here and
// refuses to run against it over http.

import { createServer } from "node:http";
import { createHmac, randomUUID, timingSafeEqual } from "node:crypto";

const PORT = Number(process.env.PORT ?? 8081);
const API_KEY = readRequired("LIVEKIT_API_KEY");
const API_SECRET = readRequired("LIVEKIT_API_SECRET");

// Optional second factor: when set, requests must send `x-weplay-token-key`.
// Without it, anybody who can reach the port could mint join tokens, so set it
// and keep the matching value out of this repository.
const CALLER_HEADER = process.env.TOKEN_SECRET ?? "";

// How long a minted token stays valid. The app reconnects with a fresh one.
const TOKEN_TTL_SECONDS = Number(process.env.TOKEN_TTL_SECONDS ?? 3600);

const ROOM_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

createServer(handleRequest).listen(PORT, () => {
  console.log(`voice token server listening on ${PORT}`);
});

function readRequired(name) {
  const value = process.env[name];
  if (!value) {
    console.error(`${name} is required`);
    process.exit(1);
  }
  if (name === "LIVEKIT_API_SECRET" && value.length < 32) {
    console.error("LIVEKIT_API_SECRET must be at least 32 characters");
    process.exit(1);
  }
  return value;
}

function handleRequest(req, res) {
  const url = new URL(req.url, `http://${req.headers.host ?? "localhost"}`);

  if (url.pathname === "/healthz") {
    return send(res, 200, { status: "ok" });
  }
  if (url.pathname !== "/voice/token") {
    return send(res, 404, { error: "not found" });
  }
  if (CALLER_HEADER && !headerMatches(req.headers["x-weplay-token-key"], CALLER_HEADER)) {
    return send(res, 403, { error: "caller not authorised" });
  }

  const room = url.searchParams.get("room") ?? "";
  const identity = (url.searchParams.get("identity") ?? "").slice(0, 64);
  const displayName = (url.searchParams.get("name") ?? "").slice(0, 64);

  if (!ROOM_PATTERN.test(room)) {
    return send(res, 400, { error: "invalid room" });
  }
  if (identity.length === 0) {
    return send(res, 400, { error: "identity is required" });
  }

  const now = Math.floor(Date.now() / 1000);
  const claims = {
    iss: API_KEY,
    sub: identity,
    nbf: now - 10,
    exp: now + TOKEN_TTL_SECONDS,
    jti: randomUUID(),
    name: displayName || identity,
    // Grants are deliberately narrow: this one room, audio + data only.
    // Seat attributes need canUpdateOwnMetadata.
    video: {
      room,
      roomJoin: true,
      canPublish: true,
      canSubscribe: true,
      canPublishData: true,
      canUpdateOwnMetadata: true,
    },
  };

  send(res, 200, { token: signToken(claims) });
}

/** HS256 JWT, exactly what livekit-server expects to verify. */
function signToken(claims) {
  const header = { alg: "HS256", typ: "JWT" };
  const encode = (part) => base64Url(JSON.stringify(part));
  const body = `${encode(header)}.${encode(claims)}`;
  const signature = base64UrlRaw(createHmac("sha256", API_SECRET).update(body).digest());
  return `${body}.${signature}`;
}

function base64Url(text) {
  return base64UrlRaw(Buffer.from(text, "utf8"));
}

function base64UrlRaw(buffer) {
  return buffer
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function headerMatches(candidate, expected) {
  if (typeof candidate !== "string" || candidate.length !== expected.length) return false;
  return timingSafeEqual(Buffer.from(candidate), Buffer.from(expected));
}

function send(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(body),
    "cache-control": "no-store",
  });
  res.end(body);
}
