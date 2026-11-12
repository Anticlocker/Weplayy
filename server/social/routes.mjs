import { SocialError } from "./service.mjs";

const BASE = "/social";

// Pure dispatcher: the HTTP layer resolves `userId` through the bearer token
// and passes it in. Every social route requires a signed-in user.
export async function handleSocialRequest({ service, method, path, userId, body = null }) {
  try {
    if (typeof userId !== "string" || userId.length === 0) {
      throw new SocialError("unauthenticated", "Authentication required.", 401);
    }

    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (verb === "POST" && route === `${BASE}/requests`) {
      const sent = await service.sendRequest(userId, body?.toUserId);
      return { status: sent.status === "accepted" ? 200 : 201, body: sent };
    }
    if (verb === "POST" && route === `${BASE}/requests/accept`) {
      return { status: 200, body: await service.acceptRequest(userId, body?.fromUserId) };
    }
    if (verb === "POST" && route === `${BASE}/requests/decline`) {
      return { status: 200, body: await service.declineRequest(userId, body?.fromUserId) };
    }
    if (verb === "GET" && route === `${BASE}/requests`) {
      return { status: 200, body: await service.listRequests(userId) };
    }
    if (verb === "GET" && route === `${BASE}/friends`) {
      return { status: 200, body: await service.listFriends(userId) };
    }
    if (verb === "POST" && route === `${BASE}/friends/remove`) {
      return { status: 200, body: await service.removeFriend(userId, body?.userId) };
    }
    if (verb === "POST" && route === `${BASE}/blocks`) {
      return { status: 200, body: await service.block(userId, body?.userId) };
    }
    if (verb === "POST" && route === `${BASE}/blocks/remove`) {
      return { status: 200, body: await service.unblock(userId, body?.userId) };
    }
    if (verb === "GET" && route === `${BASE}/blocks`) {
      return { status: 200, body: await service.listBlocks(userId) };
    }
    if (verb === "POST" && route === `${BASE}/reports`) {
      return { status: 201, body: await service.report(userId, body?.userId, body?.reason) };
    }

    throw new SocialError("not_found", "Unknown social route.", 404);
  } catch (error) {
    if (error instanceof SocialError) {
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
