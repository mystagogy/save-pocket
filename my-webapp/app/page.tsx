"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
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
  const [needLogin, setNeedLogin] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      setNeedLogin(false);

      try {
        const response = await getMonthlySavings({ redirectOnUnauthorized: false });
        if (!cancelled) {
          setSummary(response);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiRequestError) {
            if (err.status === 401 || err.code === "UNAUTHORIZED") {
              setNeedLogin(true);
            } else {
              setError(err.message);
            }
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
              className="rounded-lg border border-[#bfcbec] bg-white px-3 py-2 text-sm font-medium"
            >
              위시 상품 검색
            </Link>
            <Link
              href="/wishes"
              className="rounded-lg border border-[#bfcbec] bg-white px-3 py-2 text-sm font-medium"
            >
              위시 리스트
            </Link>
          </div>
        </div>
      </header>

      <section className="mt-5 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <div className="flex items-start justify-between gap-3">
          <h2 className="text-4xl font-semibold">이번 달 절약 리포트</h2>
          <Link
            href="/reports/monthly"
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
          {summary?.year ?? new Date().getFullYear()}년 {summary?.month ?? new Date().getMonth() + 1}월 기준
        </p>

        {loading ? (
          <p className="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">불러오는 중...</p>
        ) : needLogin ? (
          <p className="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">
            로그인 후 메인 리포트를 확인할 수 있습니다.{" "}
            <Link href="/login" className="font-semibold text-[#1d4ed8] underline">
              로그인으로 이동
            </Link>
          </p>
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
