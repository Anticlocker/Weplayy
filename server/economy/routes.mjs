import { EconomyError } from "./service.mjs";

const BASE = "/economy";

// Pure dispatcher: the HTTP layer resolves `userId` through shared auth and passes it in.
export async function handleEconomyRequest({ service, method, path, userId, query = {}, body = null }) {
  try {
    if (typeof userId !== "string" || userId.length === 0) {
      throw new EconomyError("unauthenticated", "Authentication required.", 401);
    }

    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (verb === "GET" && route === `${BASE}/wallet`) {
      return { status: 200, body: await service.getWallet(userId) };
    }
    if (verb === "GET" && route === `${BASE}/gifts`) {
      return { status: 200, body: { items: await service.listGifts() } };
    }
    if (verb === "GET" && route === `${BASE}/packs`) {
      return { status: 200, body: { items: await service.listCoinPacks() } };
    }
    if (verb === "GET" && route === `${BASE}/transactions`) {
      const page = await service.listTransactions({
        userId,
        limit: query.limit,
        cursor: query.cursor,
      });
      return { status: 200, body: page };
    }
    if (verb === "POST" && route === `${BASE}/gifts/send`) {
      const created = await service.sendGift({
        fromUserId: userId,
        toUserId: body?.toUserId,
        giftId: body?.giftId,
        idempotencyKey: body?.idempotencyKey,
      });
      return { status: created.replay ? 200 : 201, body: created };
    }
    if (verb === "POST" && route === `${BASE}/topup/intents`) {
      const created = await service.createTopUpIntent({
        userId,
        packId: body?.packId,
        idempotencyKey: body?.idempotencyKey,
      });
      return { status: created.replay ? 200 : 201, body: created };
    }

    throw new EconomyError("not_found", "Unknown economy route.", 404);
  } catch (error) {
    if (error instanceof EconomyError) {
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
