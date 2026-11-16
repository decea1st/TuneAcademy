/**
 * The one way the frontend talks to the Spring Boot backend.
 *
 * - The browser sends the JSESSIONID login cookie automatically (same origin, so no CORS).
 * - Every non-GET request also sends the CSRF token: Spring stores it in the readable
 *   XSRF-TOKEN cookie, and we copy it into the X-XSRF-TOKEN header. Another website can't
 *   read our cookies, so it can't forge a request with the right header.
 */

/** Thrown for any non-2xx response. `message` is the server's explanation when it sent one. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

function readCookie(name: string): string | null {
  const entry = document.cookie.split("; ").find((c) => c.startsWith(`${name}=`));
  return entry ? decodeURIComponent(entry.slice(name.length + 1)) : null;
}

async function getCsrfToken(): Promise<string> {
  let token = readCookie(CSRF_COOKIE);
  if (!token) {
    // Asking Spring for the token makes it set the cookie.
    await fetch("/api/auth/csrf", { credentials: "same-origin" });
    token = readCookie(CSRF_COOKIE);
  }
  if (!token) throw new ApiError(0, "Couldn't start a secure session. Refresh and try again.");
  return token;
}

type ApiOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** Plain objects are sent as JSON; FormData (file uploads) is sent as-is. */
  body?: unknown;
};

export async function apiFetch<T>(
  path: string,
  { method = "GET", body }: ApiOptions = {},
): Promise<T> {
  const headers: Record<string, string> = {};
  let payload: BodyInit | undefined;

  if (body instanceof FormData) {
    payload = body; // the browser sets the multipart Content-Type itself
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  if (method !== "GET") headers[CSRF_HEADER] = await getCsrfToken();

  const res = await fetch(path, { method, headers, body: payload, credentials: "same-origin" });

  if (!res.ok) {
    // Spring sends errors as { title, status, detail }; show `detail` when there is one.
    const problem = (await res.json().catch(() => null)) as { detail?: string } | null;
    throw new ApiError(res.status, problem?.detail ?? `Request failed (${res.status}).`);
  }

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
