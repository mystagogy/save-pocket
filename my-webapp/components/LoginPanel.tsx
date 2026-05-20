"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useState } from "react";
import { ApiRequestError, login } from "@/lib/api";

export default function LoginPanel() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const reason = searchParams.get("reason");
  const redirectParam = searchParams.get("redirect");
  const redirectTarget = sanitizeRedirectPath(redirectParam);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await login({ email, password });
      router.push(redirectTarget);
      router.refresh();
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError("로그인 중 알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-xl flex-1 flex-col justify-center px-4 py-10 sm:px-6">
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-black/5 sm:p-8">
        <div className="text-center">
          <div className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-[#1d4ed8] text-white">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              className="h-7 w-7"
              aria-hidden="true"
            >
              <path
                d="M3 8.5C3 7.12 4.12 6 5.5 6H18.5C19.88 6 21 7.12 21 8.5V15.5C21 16.88 19.88 18 18.5 18H5.5C4.12 18 3 16.88 3 15.5V8.5Z"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <path
                d="M14.5 12C14.5 11.17 15.17 10.5 16 10.5H21V13.5H16C15.17 13.5 14.5 12.83 14.5 12Z"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <circle cx="16.4" cy="12" r="0.9" fill="currentColor" />
            </svg>
          </div>
          <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">
            작심삼일 긴축재정
          </h1>
        </div>

        <form id="login-form" className="mt-8 space-y-4" onSubmit={handleSubmit}>
          {reason === "expired" && (
            <p className="rounded-xl bg-[#fff5d6] px-3 py-2 text-sm text-[#7a4a00]">
              세션이 만료되었습니다. 다시 로그인해주세요.
            </p>
          )}
          {reason === "required" && (
            <p className="rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#1f2a44]">
              로그인이 필요한 페이지입니다. 로그인 후 이용해주세요.
            </p>
          )}
          <label className="block">
            <span className="mb-1 block text-sm font-medium">이메일</span>
            <input
              id="login-email"
              name="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
              placeholder="user@example.com"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium">비밀번호</span>
            <input
              id="login-password"
              name="password"
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
              placeholder="비밀번호"
            />
          </label>

          {error && (
            <p id="login-error" className="rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
              {error}
            </p>
          )}

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <button
              id="login-submit"
              type="submit"
              disabled={submitting}
              className="rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#173da9] disabled:cursor-not-allowed disabled:bg-[#8ca8ec]"
            >
              {submitting ? "로그인 중..." : "로그인"}
            </button>
            <Link
              href="/signup"
              className="rounded-xl border border-[#bfcbec] bg-white px-4 py-2.5 text-center text-sm font-semibold text-[#1f2a44] transition hover:bg-[#f4f7ff]"
            >
              회원가입
            </Link>
          </div>
        </form>
      </section>
    </main>
  );
}

function sanitizeRedirectPath(path: string | null): string {
  if (!path || !path.startsWith("/") || path.startsWith("//")) {
    return "/";
  }
  if (path === "/?") {
    return "/";
  }
  return path;
}
