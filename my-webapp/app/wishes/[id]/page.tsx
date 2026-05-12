"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiRequestError, deleteWish, getWishDetail, purchaseWish } from "@/lib/api";
import { formatCurrency, formatDateTime, statusToLabel } from "@/lib/format";
import { WishDetailResponse } from "@/lib/types";

function getSafeExternalUrl(rawUrl: string): string | null {
  try {
    const parsed = new URL(rawUrl);
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
      return null;
    }
    return parsed.toString();
  } catch {
    return null;
  }
}

export default function WishDetailPage() {
  const params = useParams<{ id: string }>();
  const wishId = Number(params.id);

  const [detail, setDetail] = useState<WishDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<"purchase" | "delete" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const reloadWishDetail = async () => {
    const response = await getWishDetail(wishId);
    setDetail(response);
  };

  const handleAction = async (action: "purchase" | "delete") => {
    if (!detail) {
      return;
    }

    const confirmed = window.confirm(
      action === "purchase"
        ? "이 상품을 구매 완료로 처리할까요?"
        : "이 위시를 삭제 처리할까요?",
    );
    if (!confirmed) {
      return;
    }

    setActionLoading(action);
    setActionError(null);

    try {
      if (action === "purchase") {
        await purchaseWish(detail.id);
      } else {
        await deleteWish(detail.id);
      }
      await reloadWishDetail();
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setActionError(err.message);
        return;
      }
      setActionError("요청 처리 중 오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      if (Number.isNaN(wishId)) {
        setError("유효하지 않은 상품 ID입니다.");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const response = await getWishDetail(wishId);
        if (!cancelled) {
          setDetail(response);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiRequestError) {
            setError(err.message);
          } else {
            setError("상세 정보를 불러오지 못했습니다.");
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
  }, [wishId]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white px-4 py-8 text-center text-sm text-[#4b556d] shadow-sm ring-1 ring-black/5">
        불러오는 중...
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="rounded-2xl bg-[#ffe9e9] px-4 py-3 text-sm text-[#b71d1d]">
        {error ?? "상세 정보를 표시할 수 없습니다."}
      </div>
    );
  }

  const isActionDisabled =
    actionLoading !== null || detail.status === "PURCHASED" || detail.status === "DELETED";
  const safeProductUrl = getSafeExternalUrl(detail.productUrl);

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            <p className="text-sm text-[#4b556d]">ID: {detail.id}</p>
            <h2 className="mt-1 text-2xl font-bold">{detail.name}</h2>
            <p className="mt-2 inline-block rounded-full bg-[#edf2ff] px-2.5 py-1 text-xs font-medium text-[#2d3b62]">
              {statusToLabel(detail.status)}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Link
              href="/wishes"
              className="rounded-lg border border-[#d6dcef] px-3 py-2 text-sm font-medium text-[#39445f] transition hover:bg-[#f5f7fc]"
            >
              목록
            </Link>
            <button
              type="button"
              onClick={() => handleAction("purchase")}
              disabled={isActionDisabled}
              className="rounded-lg border border-[#9ecfba] px-3 py-2 text-sm font-medium text-[#1b6f4d] transition hover:bg-[#eaf9f2] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {actionLoading === "purchase" ? "구매 처리 중..." : "구매"}
            </button>
            <button
              type="button"
              onClick={() => handleAction("delete")}
              disabled={isActionDisabled}
              className="rounded-lg border border-[#f2b8bf] px-3 py-2 text-sm font-medium text-[#a03141] transition hover:bg-[#fff1f3] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {actionLoading === "delete" ? "삭제 처리 중..." : "삭제"}
            </button>
          </div>
        </div>
        {actionError && (
          <p className="mt-3 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
            {actionError}
          </p>
        )}

        {detail.imageUrl && (
          <div className="mt-5 overflow-hidden rounded-2xl border border-[#e3e8f4]">
            <Image
              src={detail.imageUrl}
              alt={detail.name}
              width={1200}
              height={630}
              className="h-auto w-full object-cover"
            />
          </div>
        )}

        <dl className="mt-5 grid grid-cols-1 gap-3 text-sm text-[#4b556d] md:grid-cols-2">
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>상품 URL</dt>
            <dd className="mt-1 break-all font-medium text-[#1d2433]">
              {detail.productUrl}
            </dd>
            {safeProductUrl && (
              <a
                href={safeProductUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-3 inline-flex items-center rounded-lg border border-[#cfd8ee] bg-white px-3 py-1.5 text-xs font-semibold text-[#2c426f] transition hover:bg-[#eef3ff]"
              >
                사이트로 이동
              </a>
            )}
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>메모</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {detail.memo || "-"}
            </dd>
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>기준 가격</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {formatCurrency(detail.referencePrice)}
            </dd>
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>체감 최저가</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {formatCurrency(detail.userDealPrice)}
            </dd>
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>절약 계산 기준가</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {formatCurrency(detail.effectivePrice)}
            </dd>
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>만료 예정 시각</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {formatDateTime(detail.expireAt)}
            </dd>
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>마지막 조회 시각</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {formatDateTime(detail.lastViewedAt)}
            </dd>
          </div>
          <div className="rounded-xl bg-[#f6f8ff] px-4 py-3">
            <dt>재활성화 횟수</dt>
            <dd className="mt-1 font-medium text-[#1d2433]">
              {detail.reactivatedCount}회
            </dd>
          </div>
        </dl>
      </section>

      <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <h3 className="text-lg font-semibold">가격 변동 이력</h3>
        {detail.priceHistories.length === 0 ? (
          <p className="mt-3 text-sm text-[#4b556d]">가격 변동 이력이 없습니다.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {detail.priceHistories.map((item) => (
              <li
                key={item.id}
                className="rounded-xl bg-[#f6f8ff] px-4 py-3 text-sm"
              >
                <p className="font-medium text-[#1d2433]">{item.priceType}</p>
                <p className="mt-1 text-[#4b556d]">
                  {formatCurrency(item.previousPrice)} → {formatCurrency(item.changedPrice)}
                </p>
                <p className="mt-1 text-[#4b556d]">
                  {formatDateTime(item.changedAt)}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <h3 className="text-lg font-semibold">이벤트 이력</h3>
        {detail.events.length === 0 ? (
          <p className="mt-3 text-sm text-[#4b556d]">이벤트 이력이 없습니다.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {detail.events.map((event) => (
              <li key={event.id} className="rounded-xl bg-[#f6f8ff] px-4 py-3 text-sm">
                <p className="font-medium text-[#1d2433]">{event.eventType}</p>
                <p className="mt-1 text-[#4b556d]">{formatDateTime(event.eventAt)}</p>
                <p className="mt-1 text-[#4b556d]">{event.description ?? "설명 없음"}</p>
                {event.metadata && (
                  <pre className="mt-2 overflow-x-auto rounded-lg bg-white p-2 text-xs text-[#3e4b6b]">
                    {event.metadata}
                  </pre>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
