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
  MonthlySavingsResponse,
} from "@/lib/types";

const API_PREFIX = "/sp";
let redirectingToLogin = false;
const LOGIN_HINT_KEY = "sp_has_logged_in";
const SESSION_ACTIVE_KEY = "sp_session_active";

interface ApiRequestInit extends RequestInit {
  redirectOnUnauthorized?: boolean;
}

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

async function request<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const { redirectOnUnauthorized = true, ...fetchInit } = init;
  const headers = new Headers(fetchInit.headers);
  headers.set("Accept", "application/json");

  if (fetchInit.body && !(fetchInit.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_PREFIX}${path}`, {
    ...fetchInit,
    headers,
    credentials: "include",
    cache: "no-store",
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const envelope = await parseEnvelope<T>(response);
  const unauthorized =
    response.status === 401 || envelope?.error?.code === "UNAUTHORIZED";
  if (unauthorized && redirectOnUnauthorized && !path.startsWith("/auth/")) {
    handleUnauthorizedRedirect();
  }

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

function handleUnauthorizedRedirect() {
  if (typeof window === "undefined" || redirectingToLogin) {
    return;
  }

  const hasActiveSessionHint = readSessionActiveHint();
  redirectingToLogin = true;
  const currentPath = `${window.location.pathname}${window.location.search}`;
  if (hasActiveSessionHint) {
    const params = new URLSearchParams({
      reason: "expired",
      redirect: currentPath,
    });
    window.alert("세션이 만료되었습니다. 다시 로그인해주세요.");
    window.location.href = `/login?${params.toString()}`;
    return;
  }

  const params = new URLSearchParams({
    reason: "required",
    redirect: currentPath,
  });
  window.alert("로그인이 필요합니다.");
  window.location.href = `/login?${params.toString()}`;
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
  }).then(async (response) => {
    try {
      await request<AuthUserResponse>("/auth/me", {
        redirectOnUnauthorized: false,
      });
      writeLoginHint();
      return response;
    } catch {
      throw new ApiRequestError(
        "로그인은 성공했지만 세션이 유지되지 않았습니다. 같은 주소(IP/도메인)로 다시 접속 후 로그인해주세요.",
        401,
        "SESSION_NOT_PERSISTED",
      );
    }
  });
}

export function logout() {
  return request<void>("/auth/logout", {
    method: "POST",
  }).finally(() => {
    clearLoginHint();
  });
}

export function clearAuthClientHints() {
  clearLoginHint();
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

export function getMonthlySavings(options?: { redirectOnUnauthorized?: boolean }) {
  return request<MonthlySavingsResponse>("/reports/monthly", options);
}

function writeLoginHint() {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.setItem(LOGIN_HINT_KEY, "1");
    window.sessionStorage.setItem(SESSION_ACTIVE_KEY, "1");
  } catch {
    // localStorage is optional for this UX signal.
  }
}

function clearLoginHint() {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.removeItem(LOGIN_HINT_KEY);
    window.sessionStorage.removeItem(SESSION_ACTIVE_KEY);
  } catch {
    // localStorage is optional for this UX signal.
  }
}

function readSessionActiveHint(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  try {
    return window.sessionStorage.getItem(SESSION_ACTIVE_KEY) === "1";
  } catch {
    return false;
  }
}
