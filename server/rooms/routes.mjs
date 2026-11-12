import { RoomsError } from "./service.mjs";

const BASE = "/rooms";

// Pure dispatcher: the HTTP layer resolves `userId` through the bearer token
// and passes it in. Every rooms route requires a signed-in user.
export async function handleRoomsRequest({ service, method, path, userId, query = {}, body = null }) {
  try {
    if (typeof userId !== "string" || userId.length === 0) {
      throw new RoomsError("unauthenticated", "Authentication required.", 401);
    }

    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (verb === "POST" && route === BASE) {
      return { status: 201, body: await service.create(userId, body ?? {}) };
    }
    if (verb === "GET" && route === BASE) {
      return { status: 200, body: await service.listOpen({ limit: query.limit }) };
    }
    if (verb === "POST" && route === `${BASE}/close`) {
      return { status: 200, body: await service.close(body?.roomId, userId) };
    }

    throw new RoomsError("not_found", "Unknown rooms route.", 404);
  } catch (error) {
    if (error instanceof RoomsError) {
      return { status: error.status, body: { error: { code: error.code, message: error.message } } };
    }
    throw error;
  }
}

function normalize(path) {
  const value = String(path ?? "");
  if (value.length > 1 && value.endsWith("/")) return value.slice(0, -1);
  return value;
}
