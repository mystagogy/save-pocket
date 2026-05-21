"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import {
  ApiRequestError,
  getNotifications,
  markNotificationAsRead,
} from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { NotificationItem } from "@/lib/types";

const MAX_ITEMS = 20;
const BASE_RETRY_DELAY_MS = 1000;
const MAX_RETRY_DELAY_MS = 10000;

export default function NotificationCenter() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const retryCountRef = useRef(0);
  const retryTimerRef = useRef<number | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const stopReconnectRef = useRef(false);
  const unmountedRef = useRef(false);

  const loadNotifications = async (): Promise<boolean> => {
    try {
      const response = await getNotifications(MAX_ITEMS);
      if (unmountedRef.current) {
        return false;
      }
      setItems(response.items);
      setUnreadCount(response.unreadCount);
      setError(null);
      return true;
    } catch (err) {
      if (!unmountedRef.current && err instanceof ApiRequestError) {
        setError(err.message);
        if (err.status === 401 || err.code === "UNAUTHORIZED") {
          stopReconnectRef.current = true;
        }
      }
      return false;
    }
  };

  useEffect(() => {
    const timerId = window.setTimeout(() => {
      void loadNotifications();
    }, 0);
    return () => {
      window.clearTimeout(timerId);
      unmountedRef.current = true;
    };
  }, []);

  useEffect(() => {
    const scheduleReconnect = () => {
      if (stopReconnectRef.current || unmountedRef.current) {
        return;
      }

      retryCountRef.current += 1;
      const delay = Math.min(
        BASE_RETRY_DELAY_MS * 2 ** Math.max(retryCountRef.current - 1, 0),
        MAX_RETRY_DELAY_MS,
      );

      if (retryTimerRef.current !== null) {
        window.clearTimeout(retryTimerRef.current);
      }

      retryTimerRef.current = window.setTimeout(() => {
        if (!stopReconnectRef.current && !unmountedRef.current) {
          connect();
        }
      }, delay);
    };

    const handleStreamError = async () => {
      // 끊긴 동안 유실된 알림을 재동기화하고, 인증 만료 시 재연결을 중단한다.
      await loadNotifications();
      scheduleReconnect();
    };

    const connect = () => {
      if (stopReconnectRef.current || unmountedRef.current) {
        return;
      }

      const eventSource = new EventSource("/sp/notifications/stream", {
        withCredentials: true,
      });
      eventSourceRef.current = eventSource;

      eventSource.addEventListener("connected", () => {
        retryCountRef.current = 0;
        void loadNotifications();
      });

      eventSource.addEventListener("notification", (event) => {
        retryCountRef.current = 0;
        setError(null);
        const parsed = parseNotificationEvent(event);
        if (!parsed) {
          return;
        }

        setItems((prev) => {
          const exists = prev.some((item) => item.id === parsed.id);
          if (exists) {
            return prev;
          }
          return [parsed, ...prev].slice(0, MAX_ITEMS);
        });
        setUnreadCount((prev) => prev + 1);
      });

      eventSource.onerror = () => {
        eventSource.close();
        void handleStreamError();
      };
    };

    connect();

    return () => {
      unmountedRef.current = true;
      if (retryTimerRef.current !== null) {
        window.clearTimeout(retryTimerRef.current);
      }
      eventSourceRef.current?.close();
      eventSourceRef.current = null;
    };
  }, []);

  const handleMarkRead = async (item: NotificationItem) => {
    if (item.read) {
      return;
    }

    try {
      await markNotificationAsRead(item.id);
      setItems((prev) =>
        prev.map((current) =>
          current.id === item.id ? { ...current, read: true } : current,
        ),
      );
      setUnreadCount((prev) => Math.max(prev - 1, 0));
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      }
    }
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="relative inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#bfcbec] bg-white text-[#1f2a44] transition hover:bg-[#f4f7ff]"
        aria-label="알림 보기"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          aria-hidden="true"
        >
          <path
            d="M8 17H16M6.5 17C7.5 15.8 8 14.2 8 12.5V10.5C8 8.01 10.01 6 12.5 6C14.99 6 17 8.01 17 10.5V12.5C17 14.2 17.5 15.8 18.5 17H6.5Z"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
          />
          <path d="M10.5 18.5C10.8 19.4 11.6 20 12.5 20C13.4 20 14.2 19.4 14.5 18.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-[#dc2626] px-1.5 text-center text-[11px] font-semibold text-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-20 mt-2 w-80 rounded-xl border border-[#dbe4f5] bg-white p-3 shadow-lg">
          <div className="mb-2 flex items-center justify-between">
            <p className="text-sm font-semibold text-[#1f2a44]">알림</p>
            <p className="text-xs text-[#5a6788]">읽지 않음 {unreadCount}개</p>
          </div>

          {error && (
            <p className="mb-2 rounded-lg bg-[#ffe9e9] px-2 py-1 text-xs text-[#b71d1d]">
              {error}
            </p>
          )}

          {items.length === 0 ? (
            <p className="rounded-lg bg-[#f4f7ff] px-3 py-4 text-center text-sm text-[#4b556d]">
              새로운 알림이 없습니다.
            </p>
          ) : (
            <ul className="max-h-72 space-y-2 overflow-y-auto pr-1">
              {items.map((item) => (
                <li
                  key={item.id}
                  className={`rounded-lg border px-3 py-2 ${
                    item.read ? "border-[#e4e9f5] bg-white" : "border-[#cfe0ff] bg-[#f7fbff]"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-[#1f2a44]">{item.title}</p>
                      <p className="mt-1 text-xs text-[#4b556d]">{item.message}</p>
                      <p className="mt-1 text-[11px] text-[#6b7280]">{formatDateTime(item.createdAt)}</p>
                    </div>
                    {!item.read && <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-[#2563eb]" />}
                  </div>

                  <div className="mt-2 flex items-center justify-end gap-2">
                    {!item.read && (
                      <button
                        type="button"
                        onClick={() => {
                          void handleMarkRead(item);
                        }}
                        className="text-xs font-medium text-[#2563eb] hover:underline"
                      >
                        읽음
                      </button>
                    )}
                    {item.linkUrl && (
                      <Link
                        href={item.linkUrl}
                        onClick={() => {
                          setOpen(false);
                          void handleMarkRead(item);
                        }}
                        className="text-xs font-medium text-[#1d4ed8] hover:underline"
                      >
                        상세 보기
                      </Link>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

function parseNotificationEvent(event: Event): NotificationItem | null {
  const message = event as MessageEvent<string>;
  if (!message.data) {
    return null;
  }
  try {
    return JSON.parse(message.data) as NotificationItem;
  } catch {
    return null;
  }
}
