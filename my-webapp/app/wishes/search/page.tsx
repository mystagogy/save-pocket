"use client";

import Image from "next/image";
import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { ApiRequestError, searchWishProducts } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import { WishSearchItemResponse } from "@/lib/types";

function normalizeProductUrl(url: string): string {
  const decoded = url.replaceAll("&amp;", "&").trim();
  if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
    return decoded;
  }

  if (decoded.startsWith("//")) {
    return `https:${decoded}`;
  }

  return `https://${decoded}`;
}

function buildPrefillLink(item: WishSearchItemResponse): string {
  const params = new URLSearchParams({
    productUrl: item.url,
    productName: item.name,
  });

  if (item.imageUrl) {
    params.set("productImageUrl", item.imageUrl);
  }

  if (item.referencePrice !== null && item.referencePrice !== undefined) {
    params.set("referencePrice", String(item.referencePrice));
  }

  return `/wishes/new?${params.toString()}`;
}

export default function WishSearchPage() {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<WishSearchItemResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (window as typeof window & { __spWishSearchHydrated?: boolean }).__spWishSearchHydrated = true;
  }, []);

  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (!query.trim()) {
      setError("검색어를 입력해주세요.");
      return;
    }

    setLoading(true);

    try {
      const response = await searchWishProducts(query.trim());
      setItems(response);
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError("검색 중 오류가 발생했습니다.");
      }
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
    <div id="wish-search-react-root" className="space-y-4">
      <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-xl font-semibold">위시 상품 검색</h2>
          <Link
            href="/wishes/new"
            className="min-w-20 whitespace-nowrap rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-center text-sm font-semibold !text-white transition hover:bg-[#173da9] hover:!text-white visited:!text-white"
          >
            수동 등록
          </Link>
        </div>

        <form onSubmit={handleSearch} className="mt-5 flex flex-col gap-3 sm:flex-row">
          <input
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="예: 나이키 운동화"
            className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
          />
          <button
            type="submit"
            disabled={loading}
            className="min-w-20 whitespace-nowrap rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#173da9] disabled:cursor-not-allowed disabled:bg-[#8ca8ec]"
          >
            {loading ? "검색 중..." : "검색"}
          </button>
        </form>

        {error && (
          <p className="mt-4 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
            {error}
          </p>
        )}

        <div className="mt-4 border-t border-[#e5eaf4] pt-4" />
      </section>

      {items.length > 0 && (
        <section className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {items.map((item, index) => (
            <article
              key={`${item.url}-${index}`}
              className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5"
            >
              <div className="flex gap-4">
                {item.imageUrl ? (
                  <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-lg bg-[#eef2ff]">
                    <Image
                      src={item.imageUrl}
                      alt={item.name}
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
                  <p className="text-xs font-medium text-[#4b556d]">
                    {item.mallName || "몰 정보 없음"}
                  </p>
                  <h3 className="mt-1 line-clamp-2 text-lg font-semibold">{item.name}</h3>
                  <p className="mt-2 text-sm text-[#1f2a44]">
                    기준 가격: <span className="font-semibold">{formatCurrency(item.referencePrice)}</span>
                  </p>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <a
                  href={normalizeProductUrl(item.url)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="rounded-lg border border-[#bfcbec] bg-white px-3 py-2 text-sm font-medium text-[#1f2a44] transition hover:bg-[#f4f7ff]"
                >
                  사이트로 이동
                </a>
                <Link
                  href={buildPrefillLink(item)}
                  className="rounded-lg bg-[#1d4ed8] px-3 py-2 text-sm font-medium text-white transition hover:bg-[#173da9]"
                >
                  이 상품으로 등록
                </Link>
              </div>
            </article>
          ))}
        </section>
      )}
    </div>
    <div id="wish-search-fallback-root" className="hidden" />
    <script
      dangerouslySetInnerHTML={{
        __html: `
          (function () {
            function escapeHtml(text) {
              return String(text)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll('"', "&quot;");
            }

            function formatCurrency(value) {
              if (value === null || value === undefined) return "-";
              return Number(value).toLocaleString("ko-KR") + "원";
            }

            function buildPrefillLink(item) {
              var params = new URLSearchParams({
                productUrl: item.url || "",
                productName: item.name || ""
              });
              if (item.imageUrl) params.set("productImageUrl", item.imageUrl);
              if (item.referencePrice !== null && item.referencePrice !== undefined) {
                params.set("referencePrice", String(item.referencePrice));
              }
              return "/wishes/new?" + params.toString();
            }

            function renderBase(root, loading) {
              root.innerHTML =
                '<section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">' +
                '<div class="flex items-center justify-between gap-3">' +
                '<h2 class="text-xl font-semibold">위시 상품 검색</h2>' +
                '<a href="/wishes/new" class="min-w-20 whitespace-nowrap rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-center text-sm font-semibold !text-white">수동 등록</a>' +
                '</div>' +
                '<form id="wish-search-fallback-form" class="mt-5 flex flex-col gap-3 sm:flex-row">' +
                '<input id="wish-search-fallback-input" type="text" placeholder="예: 나이키 운동화" class="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none" />' +
                '<button id="wish-search-fallback-submit" type="submit" class="min-w-20 whitespace-nowrap rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-sm font-semibold text-white">' +
                (loading ? "검색 중..." : "검색") +
                '</button>' +
                '</form>' +
                '<p id="wish-search-fallback-error" class="mt-4 hidden rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]"></p>' +
                '</section>' +
                '<section id="wish-search-fallback-results" class="grid grid-cols-1 gap-4 md:grid-cols-2"></section>';
            }

            function showError(message) {
              var error = document.getElementById("wish-search-fallback-error");
              if (error instanceof HTMLElement) {
                error.textContent = message;
                error.classList.remove("hidden");
              }
            }

            function clearError() {
              var error = document.getElementById("wish-search-fallback-error");
              if (error instanceof HTMLElement) {
                error.textContent = "";
                error.classList.add("hidden");
              }
            }

            function renderResults(items) {
              var target = document.getElementById("wish-search-fallback-results");
              if (!(target instanceof HTMLElement)) return;
              if (!Array.isArray(items) || items.length === 0) {
                target.innerHTML = "";
                return;
              }

              target.innerHTML = items.map(function (item, index) {
                var href = buildPrefillLink(item);
                return (
                  '<article key="' + index + '" class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5">' +
                  '<p class="text-xs font-medium text-[#4b556d]">' + escapeHtml(item.mallName || "몰 정보 없음") + '</p>' +
                  '<h3 class="mt-1 line-clamp-2 text-lg font-semibold">' + escapeHtml(item.name || "") + '</h3>' +
                  '<p class="mt-2 text-sm text-[#1f2a44]">기준 가격: <span class="font-semibold">' + escapeHtml(formatCurrency(item.referencePrice)) + '</span></p>' +
                  '<div class="mt-4 flex flex-wrap gap-2">' +
                  '<a href="' + escapeHtml(item.url || "#") + '" target="_blank" rel="noopener noreferrer" class="rounded-lg border border-[#bfcbec] bg-white px-3 py-2 text-sm font-medium text-[#1f2a44]">사이트로 이동</a>' +
                  '<a href="' + href + '" class="rounded-lg bg-[#1d4ed8] px-3 py-2 text-sm font-medium text-white">이 상품으로 등록</a>' +
                  '</div>' +
                  '</article>'
                );
              }).join("");
            }

            function bindSearch(root) {
              var form = document.getElementById("wish-search-fallback-form");
              var input = document.getElementById("wish-search-fallback-input");
              if (!(form instanceof HTMLFormElement) || !(input instanceof HTMLInputElement)) return;

              form.addEventListener("submit", async function (event) {
                event.preventDefault();
                clearError();
                var query = input.value.trim();
                if (!query) {
                  showError("검색어를 입력해주세요.");
                  return;
                }

                renderBase(root, true);
                var newInput = document.getElementById("wish-search-fallback-input");
                if (newInput instanceof HTMLInputElement) {
                  newInput.value = query;
                }
                bindSearch(root);
                try {
                  var response = await fetch("/sp/wishes/search?query=" + encodeURIComponent(query), {
                    method: "GET",
                    headers: { Accept: "application/json" },
                    credentials: "include",
                    cache: "no-store"
                  });
                  var text = await response.text();
                  var payload = text ? JSON.parse(text) : null;
                  if (!response.ok || !payload || !payload.success || !Array.isArray(payload.data)) {
                    showError("검색 중 오류가 발생했습니다.");
                    return;
                  }
                  renderResults(payload.data);
                } catch (_) {
                  showError("네트워크 오류로 검색에 실패했습니다.");
                }
              });
            }

            function activateFallback() {
              if (window.__spWishSearchHydrated) return;
              var reactRoot = document.getElementById("wish-search-react-root");
              var fallbackRoot = document.getElementById("wish-search-fallback-root");
              if (!(reactRoot instanceof HTMLElement) || !(fallbackRoot instanceof HTMLElement)) return;

              reactRoot.classList.add("hidden");
              fallbackRoot.classList.remove("hidden");
              renderBase(fallbackRoot, false);
              bindSearch(fallbackRoot);
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
