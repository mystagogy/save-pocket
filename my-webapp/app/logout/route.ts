import { NextResponse } from "next/server";

const backendOrigin = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

async function doLogout(request: Request) {
  try {
    const cookie = request.headers.get("cookie");
    await fetch(`${backendOrigin}/auth/logout`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        ...(cookie ? { Cookie: cookie } : {}),
      },
      cache: "no-store",
    });
  } catch {
    // Ignore network errors and redirect to login anyway.
  }

  const response = NextResponse.redirect(new URL("/login", request.url), 303);
  response.cookies.delete("SESSION");
  response.cookies.delete("JSESSIONID");
  return response;
}

export async function GET(request: Request) {
  return doLogout(request);
}

export async function POST(request: Request) {
  return doLogout(request);
}
