import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

const LOGIN_PATH = "/login";
const SESSION_COOKIE_NAMES = [
  "SESSION",
  "JSESSIONID",
  "__Secure-SESSION",
  "__Host-SESSION",
] as const;

export function middleware(request: NextRequest) {
  const session = SESSION_COOKIE_NAMES
    .map((cookieName) => request.cookies.get(cookieName)?.value)
    .find(Boolean);

  if (session) {
    return NextResponse.next();
  }

  const loginUrl = new URL(LOGIN_PATH, request.url);
  const returnTo = `${request.nextUrl.pathname}${request.nextUrl.search}`;
  loginUrl.searchParams.set("reason", "required");
  loginUrl.searchParams.set("redirect", returnTo);
  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: [
    "/((?!login|signup|logout|sp|_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)",
  ],
};
