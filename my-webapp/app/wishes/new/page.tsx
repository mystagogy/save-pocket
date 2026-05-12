"use client";

import { Suspense, FormEvent, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ApiRequestError, createWish } from "@/lib/api";
import { DealSourceType, WishCreateRequest } from "@/lib/types";

const dealSourceOptions: { value: DealSourceType; label: string }[] = [
  { value: "NAVER", label: "네이버" },
  { value: "SNS", label: "SNS" },
  { value: "INFLUENCER", label: "인플루언서" },
  { value: "MANUAL", label: "직접 입력" },
];

export default function WishCreatePage() {
  return (
    <Suspense
      fallback={
        <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
          <p className="text-sm text-[#4b556d]">입력 화면을 준비 중입니다...</p>
        </section>
      }
    >
      <WishCreateForm />
    </Suspense>
  );
}

function WishCreateForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState(() => ({
    productUrl: searchParams.get("productUrl") ?? "",
    productName: searchParams.get("productName") ?? "",
    memo: "",
    productImageUrl: searchParams.get("productImageUrl") ?? "",
    referencePrice: searchParams.get("referencePrice") ?? "",
    userDealPrice: "",
    dealUrl: "",
    dealSourceType: "",
  }));

  const handleChange = (field: keyof typeof form, value: string): void => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    const referencePrice =
      form.referencePrice.trim() === "" ? undefined : Number(form.referencePrice);
    const userDealPrice =
      form.userDealPrice.trim() === "" ? undefined : Number(form.userDealPrice);

    if (Number.isNaN(referencePrice ?? 0) || Number.isNaN(userDealPrice ?? 0)) {
      setError("가격은 숫자로 입력해주세요.");
      setSubmitting(false);
      return;
    }

    const payload: WishCreateRequest = {
      productUrl: form.productUrl,
      productName: form.productName,
      memo: form.memo || undefined,
      productImageUrl: form.productImageUrl || undefined,
      referencePrice,
      userDealPrice,
      dealUrl: form.dealUrl || undefined,
      dealSourceType: (form.dealSourceType as DealSourceType | "") || undefined,
    };

    try {
      const created = await createWish(payload);
      router.push(`/wishes/${created.id}`);
      router.refresh();
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError("등록 중 알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
      <h2 className="text-xl font-semibold">새 위시 등록</h2>
      <p className="mt-2 text-sm text-[#4b556d]">
        자동 조회 실패 상황도 고려해 수동 입력 필드를 함께 제공합니다.
      </p>

      <form className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2" onSubmit={handleSubmit}>
        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">상품 URL *</span>
          <input
            required
            value={form.productUrl}
            onChange={(event) => handleChange("productUrl", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="https://..."
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">상품명 *</span>
          <input
            required
            value={form.productName}
            onChange={(event) => handleChange("productName", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="예: 나이키 운동화"
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">메모</span>
          <textarea
            rows={3}
            value={form.memo}
            onChange={(event) => handleChange("memo", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="이번엔 3일 동안 고민"
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">상품 이미지 URL</span>
          <input
            value={form.productImageUrl}
            onChange={(event) => handleChange("productImageUrl", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">기준 가격</span>
          <input
            value={form.referencePrice}
            onChange={(event) => handleChange("referencePrice", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="129000"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">체감 최저가</span>
          <input
            value={form.userDealPrice}
            onChange={(event) => handleChange("userDealPrice", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="119000"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">딜 URL</span>
          <input
            value={form.dealUrl}
            onChange={(event) => handleChange("dealUrl", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">딜 출처</span>
          <select
            value={form.dealSourceType}
            onChange={(event) => handleChange("dealSourceType", event.target.value)}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
          >
            <option value="">선택 안 함</option>
            {dealSourceOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        {error && (
          <p className="rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d] md:col-span-2">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="md:col-span-2 rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#173da9] disabled:cursor-not-allowed disabled:bg-[#8ca8ec]"
        >
          {submitting ? "등록 중..." : "위시 등록"}
        </button>
      </form>
    </section>
  );
}
