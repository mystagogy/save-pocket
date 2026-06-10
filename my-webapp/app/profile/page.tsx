"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import {
  ApiRequestError,
  changePassword,
  getCurrentUser,
  updateNickname,
} from "@/lib/api";
import { AuthUserResponse } from "@/lib/types";

export default function ProfilePage() {
  const [user, setUser] = useState<AuthUserResponse | null>(null);
  const [nickname, setNickname] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(true);
  const [isEditingNickname, setIsEditingNickname] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [nicknameSubmitting, setNicknameSubmitting] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [profileMessage, setProfileMessage] = useState<string | null>(null);
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const loadUser = async () => {
      setLoading(true);
      setProfileError(null);

      try {
        const response = await getCurrentUser();
        if (!cancelled) {
          setUser(response);
          setNickname(response.nickname);
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiRequestError) {
            setProfileError(err.message);
          } else {
            setProfileError("사용자 정보를 불러오지 못했습니다.");
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void loadUser();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleNicknameSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setNicknameSubmitting(true);
    setProfileMessage(null);
    setProfileError(null);

    try {
      const response = await updateNickname({ nickname });
      setUser(response);
      setNickname(response.nickname);
      setIsEditingNickname(false);
      setProfileMessage("닉네임을 변경했습니다.");
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setProfileError(err.message);
      } else {
        setProfileError("닉네임 변경 중 알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setNicknameSubmitting(false);
    }
  };

  const openNicknameEditor = () => {
    setProfileMessage(null);
    setProfileError(null);
    setNickname(user?.nickname ?? "");
    setIsEditingNickname(true);
  };

  const closeNicknameEditor = () => {
    setProfileError(null);
    setNickname(user?.nickname ?? "");
    setIsEditingNickname(false);
  };

  const openPasswordModal = () => {
    setPasswordMessage(null);
    setPasswordError(null);
    setCurrentPassword("");
    setNewPassword("");
    setIsPasswordModalOpen(true);
  };

  const closePasswordModal = () => {
    if (passwordSubmitting) {
      return;
    }
    setCurrentPassword("");
    setNewPassword("");
    setPasswordError(null);
    setIsPasswordModalOpen(false);
  };

  const handlePasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPasswordSubmitting(true);
    setPasswordMessage(null);
    setPasswordError(null);

    try {
      await changePassword({ currentPassword, newPassword });
      setPasswordMessage("비밀번호를 변경했습니다.");
      setCurrentPassword("");
      setNewPassword("");
      setIsPasswordModalOpen(false);
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setPasswordError(err.message);
      } else {
        setPasswordError("비밀번호 변경 중 알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setPasswordSubmitting(false);
    }
  };

  return (
    <>
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-4 py-8 sm:px-6 lg:px-10">
        <header className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-semibold tracking-wide text-[#1d4ed8]">SAVE-POCKET</p>
              <h1 className="mt-1 text-2xl font-bold">내 정보 관리</h1>
              <p className="mt-1 text-sm text-[#4b556d]">
                계정 정보 확인과 닉네임, 비밀번호 변경을 이곳에서 관리합니다.
              </p>
            </div>
            <Link
              href="/"
              prefetch={false}
              aria-label="홈으로 이동"
              title="홈으로 이동"
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
                  d="M4 10.5L12 4L20 10.5V19C20 19.55 19.55 20 19 20H15V14H9V20H5C4.45 20 4 19.55 4 19V10.5Z"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinejoin="round"
                />
              </svg>
            </Link>
          </div>
        </header>

        <section className="mt-5 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-black/5 sm:p-6">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-2xl font-bold text-[#1f2a44]">계정 정보</h2>
            </div>
            <button
              type="button"
              onClick={openPasswordModal}
              className="inline-flex h-9 shrink-0 items-center justify-center rounded-lg border border-[#bfcbec] bg-white px-4 text-sm font-semibold text-[#1f2a44] transition hover:bg-[#f4f7ff]"
            >
              비밀번호 변경
            </button>
          </div>

          {loading ? (
            <p className="mt-5 rounded-xl bg-[#f4f7ff] px-3 py-2 text-sm text-[#4b556d]">
              사용자 정보를 불러오는 중입니다.
            </p>
          ) : profileError && !user ? (
            <p className="mt-5 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
              {profileError}
            </p>
          ) : (
            <div className="mt-5 space-y-4">
              <section className="rounded-2xl bg-[#f8fafc] p-5 ring-1 ring-black/5">
                <div className="flex flex-col gap-3">
                  <div>
                    <p className="text-xs font-semibold tracking-wide text-[#6b7a90]">닉네임</p>
                    <div className="mt-2 flex items-center justify-between gap-3">
                      <p className="min-w-0 text-xl font-semibold text-[#1f2a44]">{user?.nickname}</p>
                      <button
                        type="button"
                        onClick={openNicknameEditor}
                        className="inline-flex h-8 shrink-0 items-center justify-center rounded-md border border-[#bfcbec] bg-white px-3 text-xs font-semibold text-[#1f2a44] transition hover:bg-[#f4f7ff]"
                      >
                        변경
                      </button>
                    </div>
                  </div>
                </div>

                {isEditingNickname && (
                  <form className="mt-5 border-t border-[#dbe4f3] pt-4" onSubmit={handleNicknameSubmit}>
                    <label className="block">
                      <span className="mb-1 block text-sm font-medium text-[#24314d]">새 닉네임</span>
                      <input
                        type="text"
                        required
                        maxLength={50}
                        value={nickname}
                        onChange={(event) => setNickname(event.target.value)}
                        className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
                        placeholder="절약러"
                      />
                    </label>

                    {profileError && (
                      <p className="mt-3 rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
                        {profileError}
                      </p>
                    )}

                    <div className="mt-4 flex flex-wrap gap-2">
                      <button
                        type="submit"
                        disabled={nicknameSubmitting}
                        className="inline-flex h-10 items-center justify-center rounded-lg bg-[#1d4ed8] px-4 text-sm font-semibold text-white transition hover:bg-[#173da9] disabled:cursor-not-allowed disabled:bg-[#8ca8ec]"
                      >
                        {nicknameSubmitting ? "저장 중..." : "저장"}
                      </button>
                      <button
                        type="button"
                        onClick={closeNicknameEditor}
                        className="inline-flex h-10 items-center justify-center rounded-lg border border-[#d5dceb] bg-white px-4 text-sm font-semibold text-[#4b556d] transition hover:bg-[#f7f9fc]"
                      >
                        취소
                      </button>
                    </div>
                  </form>
                )}
              </section>

              <section className="rounded-2xl bg-[#f8fafc] p-5 ring-1 ring-black/5">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-xs font-semibold tracking-wide text-[#6b7a90]">이메일</p>
                    <p className="mt-2 break-all text-base font-medium text-[#1f2a44]">{user?.email}</p>
                  </div>
                </div>
              </section>

              {profileMessage && (
                <p className="rounded-xl bg-[#ebfff1] px-3 py-2 text-sm text-[#0a7b37]">
                  {profileMessage}
                </p>
              )}
              {passwordMessage && (
                <p className="rounded-xl bg-[#ebfff1] px-3 py-2 text-sm text-[#0a7b37]">
                  {passwordMessage}
                </p>
              )}
            </div>
          )}
        </section>
      </main>

      {isPasswordModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#0f172a]/48 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-5 shadow-2xl ring-1 ring-black/5 sm:p-6">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-semibold tracking-wide text-[#1d4ed8]">PASSWORD</p>
                <h2 className="mt-1 text-2xl font-bold text-[#1f2a44]">비밀번호 변경</h2>
                <p className="mt-1 text-sm text-[#4b556d]">
                  현재 비밀번호를 확인한 뒤 새 비밀번호를 설정합니다.
                </p>
              </div>
              <button
                type="button"
                onClick={closePasswordModal}
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#d5dceb] bg-white text-[#4b556d] transition hover:bg-[#f7f9fc]"
                aria-label="비밀번호 변경 창 닫기"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  aria-hidden="true"
                >
                  <path d="M6 6L18 18" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                  <path d="M18 6L6 18" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                </svg>
              </button>
            </div>

            <form className="mt-5 space-y-4" onSubmit={handlePasswordSubmit}>
              <label className="block">
                <span className="mb-1 block text-sm font-medium text-[#24314d]">현재 비밀번호</span>
                <input
                  type="password"
                  required
                  value={currentPassword}
                  onChange={(event) => setCurrentPassword(event.target.value)}
                  className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
                  placeholder="현재 비밀번호"
                />
              </label>

              <label className="block">
                <span className="mb-1 block text-sm font-medium text-[#24314d]">새 비밀번호</span>
                <input
                  type="password"
                  required
                  minLength={8}
                  maxLength={50}
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  className="w-full rounded-xl border border-[#d5dceb] bg-white px-3 py-2.5 text-sm outline-none ring-[#2b63e4] transition focus:ring-2"
                  placeholder="영문, 숫자, 특수문자 포함 8자 이상"
                />
              </label>

              {passwordError && (
                <p className="rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]">
                  {passwordError}
                </p>
              )}

              <div className="flex flex-wrap justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={closePasswordModal}
                  className="inline-flex h-10 items-center justify-center rounded-lg border border-[#d5dceb] bg-white px-4 text-sm font-semibold text-[#4b556d] transition hover:bg-[#f7f9fc]"
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={passwordSubmitting}
                  className="inline-flex h-10 items-center justify-center rounded-lg bg-[#1d4ed8] px-4 text-sm font-semibold text-white transition hover:bg-[#173da9] disabled:cursor-not-allowed disabled:bg-[#8ca8ec]"
                >
                  {passwordSubmitting ? "변경 중..." : "변경하기"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
