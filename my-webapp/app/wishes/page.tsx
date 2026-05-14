"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { ApiRequestError, getWishes } from "@/lib/api";
import { formatCurrency, formatDateTime, statusToLabel } from "@/lib/format";
import { WishStatus, WishSummaryResponse } from "@/lib/types";

type WishFilter = "ALL" | WishStatus;

const filters: { value: WishFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "WAITING", label: "보류 중" },
  { value: "EXPIRED", label: "만료" },
  { value: "PURCHASED", label: "구매" },
  { value: "DELETED", label: "삭제" },
];

export default function WishListPage() {
  const [filter, setFilter] = useState<WishFilter>("WAITING");
  const [items, setItems] = useState<WishSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const statusForRequest = useMemo(
    () => (filter === "ALL" ? undefined : filter),
    [filter],
  );

  useEffect(() => {
    (window as typeof window & { __spWishesHydrated?: boolean }).__spWishesHydrated = true;
  }, []);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await getWishes(statusForRequest);
        if (!cancelled) {
          setItems(response);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiRequestError) {
            setError(err.message);
          } else {
            setError("목록을 불러오지 못했습니다.");
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
  }, [statusForRequest]);

  return (
    <>
      <div id="wish-react-root" className="space-y-4">
        <div className="flex flex-wrap gap-2 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-black/5">
          {filters.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => setFilter(item.value)}
              className={`rounded-lg px-3 py-1.5 text-sm font-medium transition ${
                filter === item.value
                  ? "bg-[#1d4ed8] text-white"
                  : "bg-[#edf2ff] text-[#2d3b62] hover:bg-[#dfe8ff]"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>

        {error && (
          <div className="rounded-2xl bg-[#ffe9e9] px-4 py-3 text-sm text-[#b71d1d]">
            {error}
            {error.includes("인증") && (
              <span>
                {" "}
                <Link href="/login" className="font-semibold underline">
                  로그인으로 이동
                </Link>
              </span>
            )}
          </div>
        )}

        {loading ? (
          <div className="rounded-2xl bg-white px-4 py-8 text-center text-sm text-[#4b556d] shadow-sm ring-1 ring-black/5">
            불러오는 중...
          </div>
        ) : items.length === 0 ? (
          <div className="rounded-2xl bg-white px-4 py-8 text-center text-sm text-[#4b556d] shadow-sm ring-1 ring-black/5">
            조건에 맞는 위시가 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
            {items.map((wish) => (
              <Link
                key={wish.id}
                href={`/wishes/${wish.id}`}
                className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 transition hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="flex gap-4">
                  {wish.imageUrl ? (
                    <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-lg bg-[#eef2ff]">
                      <Image
                        src={wish.imageUrl}
                        alt={wish.name}
                        fill
                        sizes="96px"
                        className="object-cover"
                      />
                    </div>
                  ) : (
                    <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-lg bg-[#eef2ff] text-xs text-[#4b556d]">
                      이미지 없음
                    </div>
                  )}

                  <div className="min-w-0 flex-1">
                    <h2 className="line-clamp-2 text-lg font-semibold">{wish.name}</h2>
                    <span className="mt-2 inline-block rounded-full bg-[#edf2ff] px-2.5 py-1 text-xs font-medium text-[#2d3b62]">
                      {statusToLabel(wish.status)}
                    </span>
                    <dl className="mt-4 space-y-2 text-sm text-[#4b556d]">
                      <div className="flex justify-between gap-4">
                        <dt>기준 금액</dt>
                        <dd className="font-medium text-[#1d2433]">
                          {formatCurrency(wish.effectivePrice)}
                        </dd>
                      </div>
                      <div className="flex justify-between gap-4">
                        <dt>만료 예정</dt>
                        <dd className="font-medium text-[#1d2433]">
                          {formatDateTime(wish.expireAt)}
                        </dd>
                      </div>
                    </dl>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      <div id="wish-fallback-root" className="hidden" />
      <script
        dangerouslySetInnerHTML={{
          __html: `
            (function () {
              function formatCurrency(value) {
                if (value === null || value === undefined) return "-";
                return Number(value).toLocaleString("ko-KR") + "원";
              }

              function formatDateTime(value) {
                if (!value) return "-";
                var date = new Date(value);
                if (Number.isNaN(date.getTime())) return value;
                return new Intl.DateTimeFormat("ko-KR", {
                  year: "numeric",
                  month: "2-digit",
                  day: "2-digit",
                  hour: "2-digit",
                  minute: "2-digit",
                  hour12: false
                }).format(date);
              }

              function statusToLabel(status) {
                var map = {
                  WAITING: "보류 중",
                  EXPIRED: "만료",
                  PURCHASED: "구매 완료",
                  DELETED: "삭제됨"
                };
                return map[status] || status;
              }

              function escapeHtml(text) {
                return String(text)
                  .replaceAll("&", "&amp;")
                  .replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;")
                  .replaceAll('"', "&quot;");
              }

              function buildControl(activeStatus) {
                var statuses = [
                  { value: "ALL", label: "전체" },
                  { value: "WAITING", label: "보류 중" },
                  { value: "EXPIRED", label: "만료" },
                  { value: "PURCHASED", label: "구매" },
                  { value: "DELETED", label: "삭제" }
                ];

                var buttons = statuses.map(function (item) {
                  var activeClass = item.value === activeStatus
                    ? "bg-[#1d4ed8] text-white"
                    : "bg-[#edf2ff] text-[#2d3b62]";
                  return '<button data-status="' + item.value + '" class="rounded-lg px-3 py-1.5 text-sm font-medium ' + activeClass + '">' + item.label + '</button>';
                }).join("");

                return (
                  '<div class="flex flex-wrap gap-2 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-black/5">' +
                  '<div class="flex flex-wrap gap-2">' + buttons + '</div>' +
                  '</div>'
                );
              }

              function buildCards(items) {
                if (!Array.isArray(items) || items.length === 0) {
                  return '<div class="rounded-2xl bg-white px-4 py-8 text-center text-sm text-[#4b556d] shadow-sm ring-1 ring-black/5">조건에 맞는 위시가 없습니다.</div>';
                }

                var cards = items.map(function (wish) {
                  return (
                    '<a href="/wishes/' + wish.id + '" class="block rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5">' +
                    '<h2 class="line-clamp-2 text-lg font-semibold">' + escapeHtml(wish.name) + '</h2>' +
                    '<span class="mt-2 inline-block rounded-full bg-[#edf2ff] px-2.5 py-1 text-xs font-medium text-[#2d3b62]">' +
                    escapeHtml(statusToLabel(wish.status)) +
                    '</span>' +
                    '<dl class="mt-4 space-y-2 text-sm text-[#4b556d]">' +
                    '<div class="flex justify-between gap-4"><dt>기준 금액</dt><dd class="font-medium text-[#1d2433]">' +
                    escapeHtml(formatCurrency(wish.effectivePrice)) +
                    '</dd></div>' +
                    '<div class="flex justify-between gap-4"><dt>만료 예정</dt><dd class="font-medium text-[#1d2433]">' +
                    escapeHtml(formatDateTime(wish.expireAt)) +
                    '</dd></div>' +
                    '</dl>' +
                    '</a>'
                  );
                }).join("");

                return '<div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">' + cards + '</div>';
              }

              async function loadStatus(root, status) {
                root.innerHTML = buildControl(status) +
                  '<div class="rounded-2xl bg-white px-4 py-8 text-center text-sm text-[#4b556d] shadow-sm ring-1 ring-black/5">불러오는 중...</div>';

                var query = status === "ALL" ? "" : "?status=" + encodeURIComponent(status);
                try {
                  var response = await fetch("/sp/wishes" + query, {
                    method: "GET",
                    headers: { Accept: "application/json" },
                    credentials: "include",
                    cache: "no-store"
                  });
                  var text = await response.text();
                  var payload = text ? JSON.parse(text) : null;
                  if (!response.ok || !payload || !payload.success || !Array.isArray(payload.data)) {
                    root.innerHTML = buildControl(status) +
                      '<div class="rounded-2xl bg-[#ffe9e9] px-4 py-3 text-sm text-[#b71d1d]">목록을 불러오지 못했습니다.</div>';
                    bindEvents(root, status);
                    return;
                  }
                  root.innerHTML = buildControl(status) + buildCards(payload.data);
                  bindEvents(root, status);
                } catch (_) {
                  root.innerHTML = buildControl(status) +
                    '<div class="rounded-2xl bg-[#ffe9e9] px-4 py-3 text-sm text-[#b71d1d]">네트워크 오류로 목록을 불러오지 못했습니다.</div>';
                  bindEvents(root, status);
                }
              }

              function bindEvents(root, currentStatus) {
                root.querySelectorAll("button[data-status]").forEach(function (button) {
                  button.addEventListener("click", function () {
                    var nextStatus = button.getAttribute("data-status") || "WAITING";
                    void loadStatus(root, nextStatus);
                  });
                });

              }

              function activateFallback() {
                if (window.__spWishesHydrated) return;

                var reactRoot = document.getElementById("wish-react-root");
                var fallbackRoot = document.getElementById("wish-fallback-root");
                if (!(reactRoot instanceof HTMLElement) || !(fallbackRoot instanceof HTMLElement)) return;

                reactRoot.classList.add("hidden");
                fallbackRoot.classList.remove("hidden");
                fallbackRoot.innerHTML = '<div class="rounded-2xl bg-[#fff8db] px-4 py-3 text-sm text-[#7a4a00]">모바일 호환 모드로 표시 중입니다.</div>';
                void loadStatus(fallbackRoot, "WAITING");
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
    </>
  );
}
