import { NextResponse } from "next/server";

const backendOrigin = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

function parseNumber(value: string): number | undefined {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? NaN : parsed;
}

function withError(request: Request, message: string) {
  const url = new URL("/wishes/new", request.url);
  url.searchParams.set("error", message);
  return NextResponse.redirect(url, 303);
}

export async function POST(request: Request) {
  const formData = await request.formData();

  const productUrl = String(formData.get("productUrl") ?? "").trim();
  const trackedProductId = String(formData.get("trackedProductId") ?? "").trim();
  const productName = String(formData.get("productName") ?? "").trim();
  const memo = String(formData.get("memo") ?? "").trim();
  const productImageUrl = String(formData.get("productImageUrl") ?? "").trim();
  const referencePriceRaw = String(formData.get("referencePrice") ?? "").trim();
  const userDealPriceRaw = String(formData.get("userDealPrice") ?? "").trim();
  const dealUrl = String(formData.get("dealUrl") ?? "").trim();
  const dealSourceType = String(formData.get("dealSourceType") ?? "").trim();

  if (!productUrl || !productName) {
    return withError(request, "상품 URL과 상품명은 필수입니다.");
  }

  const referencePrice = parseNumber(referencePriceRaw);
  const userDealPrice = parseNumber(userDealPriceRaw);
  if (Number.isNaN(referencePrice) || Number.isNaN(userDealPrice)) {
    return withError(request, "가격은 숫자로 입력해주세요.");
  }

  const payload = {
    productUrl,
    trackedProductId: trackedProductId || undefined,
    productName,
    memo: memo || undefined,
    productImageUrl: productImageUrl || undefined,
    referencePrice,
    userDealPrice,
    dealUrl: dealUrl || undefined,
    dealSourceType: dealSourceType || undefined,
  };

  try {
    const cookie = request.headers.get("cookie");
    const response = await fetch(`${backendOrigin}/wishes`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...(cookie ? { Cookie: cookie } : {}),
      },
      cache: "no-store",
      body: JSON.stringify(payload),
    });

    const text = await response.text();
    const envelope = text ? (JSON.parse(text) as { success?: boolean; data?: { id?: number }; error?: { message?: string; code?: string } }) : null;

    if (response.status === 401 || envelope?.error?.code === "UNAUTHORIZED") {
      const loginUrl = new URL("/login", request.url);
      loginUrl.searchParams.set("reason", "expired");
      loginUrl.searchParams.set("redirect", "/wishes/new");
      return NextResponse.redirect(loginUrl, 303);
    }

    if (!response.ok || !envelope?.success || !envelope.data?.id) {
      return withError(request, envelope?.error?.message ?? "등록 중 오류가 발생했습니다.");
    }

    return NextResponse.redirect(new URL(`/wishes/${envelope.data.id}`, request.url), 303);
  } catch {
    return withError(request, "네트워크 오류로 등록에 실패했습니다.");
  }
}
