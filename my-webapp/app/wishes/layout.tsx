"use client";

import Link from "next/link";
import { useSelectedLayoutSegment } from "next/navigation";
import NotificationCenter from "@/components/NotificationCenter";

export default function WishesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const segment = useSelectedLayoutSegment();
  const isDetailPage =
    segment !== null && segment !== "search" && segment !== "new";
  const isSearchPage = segment === "search";
  const primaryLink = isSearchPage ? "/wishes" : "/wishes/search";
  const primaryLabel = isSearchPage ? "위시 리스트" : "위시 상품 검색";

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
                href={primaryLink}
                aria-label={primaryLabel}
                title={primaryLabel}
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
              >
                {isSearchPage ? (
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
                ) : (
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
                )}
              </Link>
              <NotificationCenter />
              <form action="/logout" method="post">
                <button
                  id="wishes-logout-button"
                  type="submit"
                  aria-label="로그아웃"
                  title="로그아웃"
                  className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#d6dcef] bg-white text-[#39445f] transition hover:bg-[#f5f7fc] disabled:cursor-not-allowed"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-5 w-5"
                    aria-hidden="true"
                  >
                    <path d="M14 7L19 12L14 17" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M19 12H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                    <path d="M9 5H6C5.45 5 5 5.45 5 6V18C5 18.55 5.45 19 6 19H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                  </svg>
                </button>
              </form>
            </div>
          </div>
        </header>
      )}
      <section className="mt-5">{children}</section>
    </main>
  );
}
