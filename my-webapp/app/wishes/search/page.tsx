"use client";

import Image from "next/image";
import Link from "next/link";
import { FormEvent, useState } from "react";
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
    <div className="space-y-4">
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
  );
}
