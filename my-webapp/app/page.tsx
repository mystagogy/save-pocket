"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import NotificationCenter from "@/components/NotificationCenter";
import { ApiRequestError, getMonthlySavings } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import { MonthlySavingsResponse } from "@/lib/types";

function formatSignedCurrency(value: number): string {
  const absolute = formatCurrency(Math.abs(value));
  if (value > 0) {
    return `+${absolute}`;
  }
  if (value < 0) {
    return `-${absolute}`;
  }
  return absolute;
}

export default function HomePage() {
  const [summary, setSummary] = useState<MonthlySavingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await getMonthlySavings();
        if (!cancelled) {
          setSummary(response);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiRequestError) {
            setError(err.message);
          } else {
            setError("메인 리포트를 불러오지 못했습니다.");
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-4 py-8 sm:px-6 lg:px-10">
      <header className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-semibold tracking-wide text-[#1d4ed8]">SAVE-POCKET</p>
            <h1 className="mt-1 text-2xl font-bold">작심삼일 긴축재정</h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              href="/wishes/search"
              prefetch={false}
              aria-label="위시 상품 검색"
              title="위시 상품 검색"
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <circle cx="11" cy="11" r="6.5" stroke="currentColor" strokeWidth="1.8" />
                <path d="M16 16L20 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
              </svg>
            </Link>
            <Link
              href="/wishes"
              prefetch={false}
              aria-label="위시 리스트"
              title="위시 리스트"
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <path d="M8 7H20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                <path d="M8 12H20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                <path d="M8 17H20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                <circle cx="4.5" cy="7" r="1.2" fill="currentColor" />
                <circle cx="4.5" cy="12" r="1.2" fill="currentColor" />
                <circle cx="4.5" cy="17" r="1.2" fill="currentColor" />
              </svg>
            </Link>
            <Link
              href="/profile"
              prefetch={false}
              aria-label="내 정보 설정"
              title="내 정보 설정"
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <circle cx="12" cy="8" r="3.2" stroke="currentColor" strokeWidth="1.8" />
                <path
                  d="M5 19C5.9 15.9 8.5 14.5 12 14.5C15.5 14.5 18.1 15.9 19 19"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                />
              </svg>
            </Link>
            <NotificationCenter />
            <form action="/logout" method="post">
              <button
                id="home-logout-button"
                type="submit"
                aria-label="로그아웃"
                title="로그아웃"
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#d6dcef] bg-white text-[#39445f] transition hover:bg-[#f5f7fc] disabled:cursor-not-allowed"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  aria-hidden="true"
                >
                  <path d="M14 7L19 12L14 17" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M19 12H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                  <path d="M9 5H6C5.45 5 5 5.45 5 6V18C5 18.55 5.45 19 6 19H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                </svg>
              </button>
            </form>
          </div>
        </div>
      </header>

      <section className="mt-5 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <div className="flex items-start justify-between gap-3">
          <h2 className="text-4xl font-semibold">이번 달 절약 리포트</h2>
          <Link
            href="/reports/monthly"
            prefetch={false}
            aria-label="상세 리포트로 이동"
            title="상세 리포트"
            className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              aria-hidden="true"
            >
              <path d="M5 19V11" stroke="currentColor" strokeWidth="1.8" />
              <path d="M12 19V5" stroke="currentColor" strokeWidth="1.8" />
              <path d="M19 19V14" stroke="currentColor" strokeWidth="1.8" />
            </svg>
          </Link>
        </div>
        <p className="mt-1 text-sm text-[#4b556d]">
          {summary ? `${summary.year}년 ${summary.month}월 기준` : "집계 기준 확인 중"}
        </p>

        {loading ? (
          <p className="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">불러오는 중...</p>
        ) : error ? (
          <p className="mt-5 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">{error}</p>
        ) : (
          <div className="mt-5 rounded-2xl bg-[#f8fafc] px-5 py-6 ring-1 ring-black/5">
            <p className="text-2xl text-[#4b556d]">이번 달 총합</p>
            <p
              className={`mt-2 text-6xl font-bold tracking-tight ${
                (summary?.netSavedAmount ?? 0) > 0
                  ? "text-[#dc2626]"
                  : (summary?.netSavedAmount ?? 0) < 0
                    ? "text-[#2563eb]"
                    : "text-[#1f2937]"
              }`}
            >
              {formatSignedCurrency(summary?.netSavedAmount ?? 0)}
            </p>
          </div>
        )}
      </section>
    </main>
  );
}
