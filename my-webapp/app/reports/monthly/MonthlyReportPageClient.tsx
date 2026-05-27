"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { ApiRequestError, getMonthlySavings } from "@/lib/api";
import { formatCurrency, formatDateTime } from "@/lib/format";
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

function parsePositiveInteger(value: string | null): number | null {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return null;
  }
  return parsed;
}

export default function MonthlyReportPageClient() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const today = new Date();
  const currentYear = today.getFullYear();
  const currentMonth = today.getMonth() + 1;

  const queryYearParam = parsePositiveInteger(searchParams.get("year"));
  const queryMonthParam = parsePositiveInteger(searchParams.get("month"));
  const hasValidQuery = queryYearParam !== null && queryMonthParam !== null && queryMonthParam <= 12;
  const initialYear = hasValidQuery ? queryYearParam : currentYear;
  const initialMonth = hasValidQuery ? queryMonthParam : currentMonth;

  const [summary, setSummary] = useState<MonthlySavingsResponse | null>(null);
  const [selectedYear, setSelectedYear] = useState(initialYear);
  const [selectedMonth, setSelectedMonth] = useState(initialMonth);
  const [queryYear, setQueryYear] = useState(initialYear);
  const [queryMonth, setQueryMonth] = useState(initialMonth);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const yearOptions = useMemo(() => {
    const years = Array.from({ length: 7 }, (_, index) => currentYear - index);
    if (!years.includes(selectedYear)) {
      years.push(selectedYear);
    }
    return Array.from(new Set(years)).sort((a, b) => b - a);
  }, [currentYear, selectedYear]);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await getMonthlySavings({ year: queryYear, month: queryMonth });
        if (!cancelled) {
          setSummary(response);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiRequestError) {
            setError(err.message);
          } else {
            setError("월 리포트를 불러오지 못했습니다.");
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
  }, [queryYear, queryMonth]);

  const handlePeriodSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const params = new URLSearchParams({
      year: String(selectedYear),
      month: String(selectedMonth),
    });
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
    setQueryYear(selectedYear);
    setQueryMonth(selectedMonth);
  };

  const detailsForDisplay =
    summary && summary.details && summary.details.length > 0
      ? summary.details
      : summary
        ? [
            {
              wishId: -1,
              wishName: "만료 합계",
              status: "EXPIRED" as const,
              occurredAt: "",
              signedAmount: summary.expiredAmount,
            },
            {
              wishId: -2,
              wishName: "구매 합계",
              status: "PURCHASED" as const,
              occurredAt: "",
              signedAmount: -summary.purchasedAmount,
            },
          ].filter((item) => item.signedAmount !== 0)
        : [];

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
              href="/"
              aria-label="메인 화면으로 이동"
              title="메인 화면"
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <path
                  d="M4 10.5L12 4L20 10.5V19C20 19.55 19.55 20 19 20H5C4.45 20 4 19.55 4 19V10.5Z"
                  stroke="currentColor"
                  strokeWidth="1.8"
                />
                <path d="M9 20V13H15V20" stroke="currentColor" strokeWidth="1.8" />
              </svg>
            </Link>
            <Link
              href="/wishes/search"
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
          </div>
        </div>
      </header>

      <section className="mt-5 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <form onSubmit={handlePeriodSearch}>
            <label htmlFor="monthly-report-year" className="text-sm font-medium text-[#1f2a44]">
              연/월 조회
            </label>
            <div className="mt-2 flex items-center gap-2">
              <select
                id="monthly-report-year"
                value={selectedYear}
                onChange={(event) => setSelectedYear(Number(event.target.value))}
                className="h-9 rounded-lg border border-[#bfcbec] bg-white px-2 text-sm text-[#1f2a44]"
              >
                {yearOptions.map((year) => (
                  <option key={year} value={year}>
                    {year}년
                  </option>
                ))}
              </select>
              <select
                id="monthly-report-month"
                value={selectedMonth}
                onChange={(event) => setSelectedMonth(Number(event.target.value))}
                className="h-9 rounded-lg border border-[#bfcbec] bg-white px-2 text-sm text-[#1f2a44]"
              >
                {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                  <option key={month} value={month}>
                    {month}월
                  </option>
                ))}
              </select>
              <button
                type="submit"
                className="inline-flex h-9 items-center rounded-lg border border-[#bfcbec] bg-[#f4f7ff] px-3 text-sm font-medium text-[#1f2a44] transition hover:bg-[#e9efff]"
              >
                조회
              </button>
            </div>
          </form>
          <div className="text-right">
            <h2 className="text-xl font-semibold">월간 절약 리포트</h2>
            <p className="mt-1 text-sm text-[#4b556d]">
              {summary ? `${summary.year}년 ${summary.month}월 기준` : "집계 기준 확인 중"}
            </p>
          </div>
        </div>

        <div id="monthly-react-dynamic">
          {loading ? (
            <p className="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">불러오는 중...</p>
          ) : error ? (
            <p className="mt-5 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">{error}</p>
          ) : (
            <>
              <div className="mt-5 rounded-2xl bg-[#f8fafc] px-5 py-6 ring-1 ring-black/5">
                <p className="text-sm text-[#4b556d]">선택한 달 총합</p>
                <p
                  className={`mt-2 text-4xl font-bold tracking-tight ${
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

              <section className="mt-6">
                <h3 className="text-base font-semibold">상세 내역</h3>
                {detailsForDisplay.length > 0 ? (
                  <ul className="mt-3 divide-y divide-[#e5eaf4] rounded-xl border border-[#e5eaf4] bg-white">
                    {detailsForDisplay.map((item, index) => {
                      const isPositive = item.signedAmount > 0;
                      const amountClass = isPositive ? "text-[#dc2626]" : "text-[#2563eb]";
                      const canNavigateWishDetail = item.wishId > 0;
                      return (
                        <li
                          key={`${item.status}-${item.wishId}-${item.occurredAt}-${index}`}
                          className={canNavigateWishDetail ? "cursor-pointer" : ""}
                        >
                          {canNavigateWishDetail ? (
                            <Link
                              href={`/wishes/${item.wishId}`}
                              className="flex items-center justify-between gap-4 px-4 py-3 transition hover:bg-[#f8fbff]"
                            >
                              <div className="min-w-0">
                                <p className="truncate text-sm font-medium text-[#1f2a44]">{item.wishName}</p>
                                <p className="mt-0.5 text-xs text-[#6b7280]">
                                  {item.status === "EXPIRED" ? "만료(+)" : "구매(-)"} ·{" "}
                                  {formatDateTime(item.occurredAt)}
                                </p>
                              </div>
                              <p className={`shrink-0 text-sm font-semibold ${amountClass}`}>
                                {formatSignedCurrency(item.signedAmount)}
                              </p>
                            </Link>
                          ) : (
                            <div className="flex items-center justify-between gap-4 px-4 py-3">
                              <div className="min-w-0">
                                <p className="truncate text-sm font-medium text-[#1f2a44]">{item.wishName}</p>
                                <p className="mt-0.5 text-xs text-[#6b7280]">
                                  {item.status === "EXPIRED" ? "만료(+)" : "구매(-)"} ·{" "}
                                  {formatDateTime(item.occurredAt)}
                                </p>
                              </div>
                              <p className={`shrink-0 text-sm font-semibold ${amountClass}`}>
                                {formatSignedCurrency(item.signedAmount)}
                              </p>
                            </div>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  <p className="mt-3 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">
                    선택한 달 상세 내역이 없습니다.
                  </p>
                )}
              </section>
            </>
          )}
        </div>
      </section>
    </main>
  );
}
