"use client";

import Link from "next/link";
import { useSelectedLayoutSegment } from "next/navigation";

export default function WishesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const segment = useSelectedLayoutSegment();
  const isDetailPage =
    segment !== null && segment !== "search" && segment !== "new";

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-4 py-8 sm:px-6 lg:px-10">
      {!isDetailPage && (
        <header className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-semibold tracking-wide text-[#1d4ed8]">
                SAVE-POCKET
              </p>
              <h1 className="mt-1 text-2xl font-bold">위시 관리</h1>
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
              <Link
                href="/wishes/new"
                className="rounded-lg bg-[#1d4ed8] px-3 py-2 text-sm font-medium text-white"
              >
                수동 등록
              </Link>
            </div>
          </div>
        </header>
      )}
      <section className="mt-5">{children}</section>
    </main>
  );
}
