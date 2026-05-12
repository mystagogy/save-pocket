import { WishStatus } from "@/lib/types";

const statusLabelMap: Record<WishStatus, string> = {
  WAITING: "보류 중",
  EXPIRED: "만료",
  PURCHASED: "구매 완료",
  DELETED: "삭제됨",
};

export function statusToLabel(status: WishStatus): string {
  return statusLabelMap[status];
}

export function formatCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${value.toLocaleString("ko-KR")}원`;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}
