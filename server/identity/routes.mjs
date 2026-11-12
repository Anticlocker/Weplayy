import { IdentityError } from "./service.mjs";

const BASE = "/identity";
const PUBLIC = new Set([`${BASE}/signup`, `${BASE}/signin`]);

// Pure dispatcher: the HTTP layer resolves `userId` through the bearer token
// and passes it in. signup/signin are the only public routes.
export async function handleIdentityRequest({ service, method, path, userId, body = null }) {
  try {
    const route = normalize(path);
    const verb = String(method ?? "").toUpperCase();

    if (!PUBLIC.has(route)) {
      if (typeof userId !== "string" || userId.length === 0) {
        throw new IdentityError("unauthenticated", "Authentication required.", 401);
      }
    }

    if (verb === "POST" && route === `${BASE}/signup`) {
      return { status: 201, body: await service.signUp(body ?? {}) };
    }
    if (verb === "POST" && route === `${BASE}/signin`) {
      return { status: 200, body: await service.signIn(body ?? {}) };
    }
    if (verb === "POST" && route === `${BASE}/signout`) {
      return { status: 200, body: await service.signOut(userId, body?.token) };
    }
    if (verb === "GET" && route === `${BASE}/me`) {
      return { status: 200, body: { user: await service.getProfile(userId) } };
    }
    if (verb === "PATCH" && route === `${BASE}/me`) {
      return { status: 200, body: { user: await service.updateProfile(userId, body ?? {}) } };
    }
    if (verb === "GET" && route === `${BASE}/settings`) {
      return { status: 200, body: { settings: await service.getSettings(userId) } };
    }
    if (verb === "PATCH" && route === `${BASE}/settings`) {
      return { status: 200, body: { settings: await service.updateSettings(userId, body ?? {}) } };
    }

    throw new IdentityError("not_found", "Unknown identity route.", 404);
  } catch (error) {
    if (error instanceof IdentityError) {
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
