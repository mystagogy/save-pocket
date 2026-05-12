"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { ApiRequestError, getWishes, logout } from "@/lib/api";
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
  const router = useRouter();
  const [filter, setFilter] = useState<WishFilter>("ALL");
  const [items, setItems] = useState<WishSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);

  const statusForRequest = useMemo(
    () => (filter === "ALL" ? undefined : filter),
    [filter],
  );

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

  const handleLogout = async () => {
    setLoggingOut(true);

    try {
      await logout();
    } finally {
      router.push("/login");
      router.refresh();
      setLoggingOut(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-black/5">
        <div className="flex flex-wrap gap-2">
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

        <button
          type="button"
          onClick={() => void handleLogout()}
          disabled={loggingOut}
          className="rounded-lg border border-[#d6dcef] px-3 py-1.5 text-sm font-medium text-[#39445f] transition hover:bg-[#f5f7fc] disabled:cursor-not-allowed"
        >
          {loggingOut ? "로그아웃 중..." : "로그아웃"}
        </button>
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
  );
}
