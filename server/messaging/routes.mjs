import { MessagingError } from "./service.mjs";

const BASE = "/messaging";
const CHAT_MESSAGES = new RegExp(`^${BASE}/chats/([^/]+)/?messages$`);
const CHAT_READ = new RegExp(`^${BASE}/chats/([^/]+)/?read$`);
const CHAT_PATCH = new RegExp(`^${BASE}/chats/([^/]+)$`);

// Pure dispatcher: the HTTP layer resolves `userId` through the bearer token
// and passes it in. Every messaging route requires a signed-in user.
export async function handleMessagingRequest({ service, method, path, userId, query = {}, body = null }) {
  try {
    if (typeof userId !== "string" || userId.length === 0) {
      throw new MessagingError("unauthenticated", "Authentication required.", 401);
    }

    // Accept both "/chats/<id>/messages" and "/chats/<id>/messages/".
    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (verb === "POST" && route === `${BASE}/chats`) {
      const opened = await service.openChat(userId, body?.userId);
      return { status: opened.created ? 201 : 200, body: opened };
    }
    if (verb === "GET" && route === `${BASE}/chats`) {
      return { status: 200, body: await service.listChats(userId, { limit: query.limit, cursor: query.cursor }) };
    }

    const messages = CHAT_MESSAGES.exec(route);
    if (verb === "GET" && messages) {
      return {
        status: 200,
        body: await service.listMessages(userId, messages[1], { limit: query.limit, cursor: query.cursor }),
      };
    }
    if (verb === "POST" && messages) {
      return { status: 201, body: await service.sendMessage(userId, { chatId: messages[1], ...body }) };
    }

    const read = CHAT_READ.exec(route);
    if (verb === "POST" && read) {
      return { status: 200, body: await service.markRead(userId, read[1], body?.at) };
    }

    const patch = CHAT_PATCH.exec(route);
    if (verb === "PATCH" && patch) {
      return { status: 200, body: await service.updateChat(userId, patch[1], body ?? {}) };
    }

    if (verb === "POST" && route === `${BASE}/messages/recall`) {
      return { status: 200, body: await service.recallMessage(userId, body?.messageId) };
    }

    throw new MessagingError("not_found", "Unknown messaging route.", 404);
  } catch (error) {
    if (error instanceof MessagingError) {
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
