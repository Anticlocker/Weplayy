import { NotificationsError } from "./service.mjs";

const BASE = "/notifications";

// Pure dispatcher: the HTTP layer resolves `userId` through the bearer token
// and passes it in. Every notifications route requires a signed-in user.
export async function handleNotificationsRequest({ service, method, path, userId, query = {}, body = null }) {
  try {
    if (typeof userId !== "string" || userId.length === 0) {
      throw new NotificationsError("unauthenticated", "Authentication required.", 401);
    }

    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (verb === "GET" && route === BASE) {
      const unreadOnly = query.unreadOnly === "1" || query.unreadOnly === "true";
      return {
        status: 200,
        body: await service.list(userId, { limit: query.limit, cursor: query.cursor, unreadOnly }),
      };
    }
    if (verb === "POST" && route === `${BASE}/read`) {
      return { status: 200, body: await service.markRead(userId, body?.id) };
    }
    if (verb === "POST" && route === `${BASE}/read-all`) {
      return { status: 200, body: await service.markAllRead(userId) };
    }

    throw new NotificationsError("not_found", "Unknown notifications route.", 404);
  } catch (error) {
    if (error instanceof NotificationsError) {
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
