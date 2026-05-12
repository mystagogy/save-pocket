"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { ApiRequestError, signup } from "@/lib/api";

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await signup({ email, nickname, password });
      router.push("/login");
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError("회원가입 중 알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col px-4 py-10 sm:px-6">
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-black/5 sm:p-8">
        <h1 className="text-2xl font-bold">회원가입</h1>
        <p className="mt-2 text-sm text-[#4b556d]">
          가입 후 로그인하면 위시 관리 화면으로 이동할 수 있습니다.
        </p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <label className="block">
            <span className="mb-1 block text-sm font-medium">닉네임</span>
            <input
              type="text"
              required
              value={nickname}
              onChange={(event) => setNickname(event.target.value)}
              className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
              placeholder="절약러"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium">이메일</span>
            <input
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
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
              placeholder="영문/숫자/특수문자 포함 8자 이상"
            />
          </label>

          {error && (
            <p className="rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#173da9] disabled:cursor-not-allowed disabled:bg-[#8ca8ec]"
          >
            {submitting ? "가입 중..." : "회원가입"}
          </button>
        </form>

        <p className="mt-5 text-sm text-[#4b556d]">
          이미 계정이 있나요?{" "}
          <Link className="font-semibold text-[#1d4ed8]" href="/login">
            로그인
          </Link>
        </p>
      </section>
    </main>
  );
}
