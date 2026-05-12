import {
  ApiEnvelope,
  AuthUserResponse,
  WishStatus,
  LoginRequest,
  SignupRequest,
  WishCreateRequest,
  WishCreateResponse,
  WishDetailResponse,
  WishSearchItemResponse,
  WishStatusUpdateResponse,
  WishSummaryResponse,
} from "@/lib/types";

const API_PREFIX = "/sp";

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.code = code;
  }
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (init.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_PREFIX}${path}`, {
    ...init,
    headers,
    credentials: "include",
    cache: "no-store",
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const envelope = await parseEnvelope<T>(response);

  if (!response.ok) {
    if (envelope?.error) {
      throw new ApiRequestError(
        envelope.error.message,
        response.status,
        envelope.error.code,
      );
    }

    throw new ApiRequestError(
      `요청이 실패했습니다. (HTTP ${response.status})`,
      response.status,
    );
  }

  if (!envelope?.success || envelope.data === null) {
    throw new ApiRequestError(
      envelope?.error?.message ?? "응답 형식이 올바르지 않습니다.",
      response.status,
      envelope?.error?.code,
    );
  }

  return envelope.data;
}

export function signup(payload: SignupRequest) {
  return request<AuthUserResponse>("/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function login(payload: LoginRequest) {
  return request<AuthUserResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function logout() {
  return request<void>("/auth/logout", {
    method: "POST",
  });
}

export function getWishes(status?: WishStatus) {
  const query = status ? `?status=${status}` : "";
  return request<WishSummaryResponse[]>(`/wishes${query}`);
}

export function getWishDetail(id: number) {
  return request<WishDetailResponse>(`/wishes/${id}`);
}

export function createWish(payload: WishCreateRequest) {
  return request<WishCreateResponse>("/wishes", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function searchWishProducts(query: string) {
  const params = new URLSearchParams({ query });
  return request<WishSearchItemResponse[]>(`/wishes/search?${params.toString()}`);
}

export function purchaseWish(id: number) {
  return request<WishStatusUpdateResponse>(`/wishes/${id}/purchase`, {
    method: "POST",
  });
}

export function reactivateWish(id: number) {
  return request<WishStatusUpdateResponse>(`/wishes/${id}/reactivate`, {
    method: "POST",
  });
}

export function deleteWish(id: number) {
  return request<WishStatusUpdateResponse>(`/wishes/${id}/delete`, {
    method: "POST",
  });
}
