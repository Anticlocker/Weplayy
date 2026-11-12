import { SupportError } from "./service.mjs";

const BASE = "/support";

// Pure dispatcher: the HTTP layer resolves `userId` through the bearer token
// and passes it in. Every support route requires a signed-in user.
export async function handleSupportRequest({ service, method, path, userId, query = {}, body = null }) {
  try {
    if (typeof userId !== "string" || userId.length === 0) {
      throw new SupportError("unauthenticated", "Authentication required.", 401);
    }

    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (verb === "POST" && route === BASE) {
      return { status: 201, body: await service.create(userId, body ?? {}) };
    }
    if (verb === "GET" && route === BASE) {
      return { status: 200, body: await service.listMine(userId, { limit: query.limit }) };
    }

    throw new SupportError("not_found", "Unknown support route.", 404);
  } catch (error) {
    if (error instanceof SupportError) {
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
