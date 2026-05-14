"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
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

export default function MonthlyReportPage() {
  const [summary, setSummary] = useState<MonthlySavingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (window as typeof window & { __spMonthlyHydrated?: boolean }).__spMonthlyHydrated = true;
  }, []);

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
  }, []);

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
        <div>
          <h2 className="text-xl font-semibold">이번 달 절약 리포트</h2>
          <p className="mt-1 text-sm text-[#4b556d]">
            {summary ? `${summary.year}년 ${summary.month}월 기준` : "집계 기준 확인 중"}
          </p>
        </div>

        <div id="monthly-react-dynamic">
          {loading ? (
            <p className="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">불러오는 중...</p>
          ) : error ? (
            <p className="mt-5 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">{error}</p>
          ) : (
            <>
              <div className="mt-5 rounded-2xl bg-[#f8fafc] px-5 py-6 ring-1 ring-black/5">
                <p className="text-sm text-[#4b556d]">이번 달 총합</p>
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
                    이번 달 상세 내역이 없습니다.
                  </p>
                )}
              </section>
            </>
          )}
        </div>

        <div id="monthly-fallback-root" className="hidden" />
      </section>

      <script
        dangerouslySetInnerHTML={{
          __html: `
            (function () {
              function formatCurrency(value) {
                if (value === null || value === undefined) return "-";
                return Number(value).toLocaleString("ko-KR") + "원";
              }

              function formatSignedCurrency(value) {
                var n = Number(value || 0);
                var abs = formatCurrency(Math.abs(n));
                if (n > 0) return "+" + abs;
                if (n < 0) return "-" + abs;
                return abs;
              }

              function escapeHtml(text) {
                return String(text)
                  .replaceAll("&", "&amp;")
                  .replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;")
                  .replaceAll('"', "&quot;");
              }

              async function activateFallback() {
                if (window.__spMonthlyHydrated) return;

                var reactDynamic = document.getElementById("monthly-react-dynamic");
                var root = document.getElementById("monthly-fallback-root");
                if (!(reactDynamic instanceof HTMLElement) || !(root instanceof HTMLElement)) return;

                reactDynamic.classList.add("hidden");
                root.classList.remove("hidden");
                root.innerHTML = '<p class="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">모바일 호환 모드에서 불러오는 중...</p>';

                try {
                  var response = await fetch("/sp/reports/monthly", {
                    method: "GET",
                    headers: { Accept: "application/json" },
                    credentials: "include",
                    cache: "no-store"
                  });
                  var text = await response.text();
                  var payload = text ? JSON.parse(text) : null;
                  if (!response.ok || !payload || !payload.success || !payload.data) {
                    root.innerHTML = '<p class="mt-5 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">월 리포트를 불러오지 못했습니다.</p>';
                    return;
                  }

                  var data = payload.data;
                  var amount = Number(data.netSavedAmount || 0);
                  var amountClass = amount > 0 ? "text-[#dc2626]" : amount < 0 ? "text-[#2563eb]" : "text-[#1f2937]";

                  var details = Array.isArray(data.details) ? data.details : [];
                  var detailHtml = "";
                  if (details.length > 0) {
                    detailHtml =
                      '<ul class="mt-3 divide-y divide-[#e5eaf4] rounded-xl border border-[#e5eaf4] bg-white">' +
                      details.map(function (item) {
                        var signed = Number(item.signedAmount || 0);
                        var amountColor = signed > 0 ? "text-[#dc2626]" : "text-[#2563eb]";
                        var label = item.status === "EXPIRED" ? "만료(+)" : "구매(-)";
                        return (
                          '<li>' +
                          '<a href="/wishes/' + item.wishId + '" class="flex items-center justify-between gap-4 px-4 py-3">' +
                          '<div class="min-w-0">' +
                          '<p class="truncate text-sm font-medium text-[#1f2a44]">' + escapeHtml(item.wishName) + '</p>' +
                          '<p class="mt-0.5 text-xs text-[#6b7280]">' + label + '</p>' +
                          '</div>' +
                          '<p class="shrink-0 text-sm font-semibold ' + amountColor + '">' + formatSignedCurrency(signed) + '</p>' +
                          '</a>' +
                          '</li>'
                        );
                      }).join("") +
                      '</ul>';
                  } else {
                    detailHtml = '<p class="mt-3 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">이번 달 상세 내역이 없습니다.</p>';
                  }

                  root.innerHTML =
                    '<div class="mt-5 rounded-2xl bg-[#f8fafc] px-5 py-6 ring-1 ring-black/5">' +
                    '<p class="text-sm text-[#4b556d]">이번 달 총합</p>' +
                    '<p class="mt-2 text-4xl font-bold tracking-tight ' + amountClass + '">' + formatSignedCurrency(amount) + '</p>' +
                    '</div>' +
                    '<section class="mt-6">' +
                    '<h3 class="text-base font-semibold">상세 내역</h3>' +
                    detailHtml +
                    '</section>';
                } catch (_) {
                  root.innerHTML = '<p class="mt-5 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">네트워크 오류로 월 리포트를 불러오지 못했습니다.</p>';
                }
              }

              if (document.readyState === "loading") {
                document.addEventListener("DOMContentLoaded", function () {
                  setTimeout(activateFallback, 600);
                });
              } else {
                setTimeout(activateFallback, 600);
              }
            })();
          `,
        }}
      />
    </main>
  );
}
