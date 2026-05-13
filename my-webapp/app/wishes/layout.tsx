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
      )}
      <section className="mt-5">{children}</section>
    </main>
  );
}
