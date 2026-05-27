import { Suspense } from "react";
import MonthlyReportPageClient from "./MonthlyReportPageClient";

function MonthlyReportFallback() {
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-4 py-8 sm:px-6 lg:px-10">
      <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
        <p className="rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">불러오는 중...</p>
      </section>
    </main>
  );
}

export default function MonthlyReportPage() {
  return (
    <Suspense fallback={<MonthlyReportFallback />}>
      <MonthlyReportPageClient />
    </Suspense>
  );
}
