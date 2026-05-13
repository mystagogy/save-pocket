export interface ApiErrorBody {
  code: string;
  message: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  error: ApiErrorBody | null;
  timestamp: string;
}

export type WishStatus = "WAITING" | "EXPIRED" | "PURCHASED" | "DELETED";

export type DealSourceType = "NAVER" | "SNS" | "INFLUENCER" | "MANUAL";

export interface AuthUserResponse {
  id: number;
  email: string;
  nickname: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
}

export interface WishCreateRequest {
  productUrl: string;
  memo?: string;
  productName: string;
  productImageUrl?: string;
  referencePrice?: number;
  userDealPrice?: number;
  dealUrl?: string;
  dealSourceType?: DealSourceType;
}

export interface WishCreateResponse {
  id: number;
  name: string;
  url: string;
  imageUrl: string | null;
  referencePrice: number | null;
  userDealPrice: number | null;
  effectivePrice: number | null;
  status: WishStatus;
  lastViewedAt: string;
  expireAt: string;
  reactivatedCount: number;
}

export interface WishSummaryResponse {
  id: number;
  name: string;
  imageUrl: string | null;
  status: WishStatus;
  effectivePrice: number | null;
  expireAt: string;
}

export interface WishSearchItemResponse {
  name: string;
  url: string;
  imageUrl: string | null;
  referencePrice: number | null;
  mallName: string | null;
}

export interface WishPriceHistoryItem {
  id: number;
  priceType: "REFERENCE" | "USER_DEAL";
  previousPrice: number;
  changedPrice: number;
  changedAt: string;
}

export interface WishEventItem {
  id: number;
  eventType:
    | "REGISTERED"
    | "VIEWED"
    | "PRICE_CHANGED"
    | "EXPIRED"
    | "REACTIVATED"
    | "PURCHASED"
    | "DELETED"
    | "MANUAL_PRICE_UPDATED";
  eventAt: string;
  description: string | null;
  metadata: string | null;
}

export interface WishDetailResponse {
  id: number;
  name: string;
  productUrl: string;
  imageUrl: string | null;
  memo: string | null;
  referencePrice: number | null;
  userDealPrice: number | null;
  effectivePrice: number | null;
  status: WishStatus;
  lastViewedAt: string;
  expireAt: string;
  reactivatedCount: number;
  priceHistories: WishPriceHistoryItem[];
  events: WishEventItem[];
}

export interface WishStatusUpdateResponse {
  id: number;
  status: WishStatus;
}

export interface MonthlySavingsResponse {
  year: number;
  month: number;
  expiredAmount: number;
  purchasedAmount: number;
  netSavedAmount: number;
  details: MonthlySavingsDetailItem[];
}

export interface MonthlySavingsDetailItem {
  wishId: number;
  wishName: string;
  status: "EXPIRED" | "PURCHASED";
  occurredAt: string;
  signedAmount: number;
}
