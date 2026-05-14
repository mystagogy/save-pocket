import { DealSourceType } from "@/lib/types";

const dealSourceOptions: { value: DealSourceType; label: string }[] = [
  { value: "NAVER", label: "네이버" },
  { value: "SNS", label: "SNS" },
  { value: "INFLUENCER", label: "인플루언서" },
  { value: "MANUAL", label: "직접 입력" },
];

type SearchParams = Record<string, string | string[] | undefined>;

function readValue(searchParams: SearchParams, key: string): string {
  const value = searchParams[key];
  if (Array.isArray(value)) {
    return value[0] ?? "";
  }
  return value ?? "";
}

export default async function WishCreatePage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const params = await searchParams;

  const productUrl = readValue(params, "productUrl");
  const productName = readValue(params, "productName");
  const productImageUrl = readValue(params, "productImageUrl");
  const referencePrice = readValue(params, "referencePrice");
  const error = readValue(params, "error");

  return (
    <section className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
      <h2 className="text-xl font-semibold">새 위시 등록</h2>
      <p className="mt-2 text-sm text-[#4b556d]">
        자동 조회 실패 상황도 고려해 수동 입력 필드를 함께 제공합니다.
      </p>

      <form
        method="POST"
        action="/wishes/create"
        className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2"
      >
        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">상품 URL *</span>
          <input
            required
            name="productUrl"
            defaultValue={productUrl}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="https://..."
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">상품명 *</span>
          <input
            required
            name="productName"
            defaultValue={productName}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="예: 나이키 운동화"
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">메모</span>
          <textarea
            rows={3}
            name="memo"
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="이번엔 3일 동안 고민"
          />
        </label>

        <label className="block md:col-span-2">
          <span className="mb-1 block text-sm font-medium">상품 이미지 URL</span>
          <input
            name="productImageUrl"
            defaultValue={productImageUrl}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">기준 가격</span>
          <input
            name="referencePrice"
            defaultValue={referencePrice}
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="129000"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">체감 최저가</span>
          <input
            name="userDealPrice"
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
            placeholder="119000"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">딜 URL</span>
          <input
            name="dealUrl"
            className="w-full rounded-xl border border-[#d5dceb] px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium">딜 출처</span>
          <select
            name="dealSourceType"
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
          className="md:col-span-2 rounded-xl bg-[#1d4ed8] px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-[#173da9]"
        >
          위시 등록
        </button>
      </form>
    </section>
  );
}
