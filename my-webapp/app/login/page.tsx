import { Suspense } from "react";
import LoginPanel from "@/components/LoginPanel";

export default function LoginPage() {
  return (
    <>
      <Suspense fallback={null}>
        <LoginPanel />
      </Suspense>
      <script
        dangerouslySetInnerHTML={{
          __html: `
            (function () {
              function sanitizeRedirectPath(path) {
                if (!path || typeof path !== "string" || !path.startsWith("/") || path.startsWith("//")) {
                  return "/";
                }
                if (path === "/?") {
                  return "/";
                }
                return path;
              }

              function resolveRedirectPath() {
                var params = new URLSearchParams(window.location.search);
                return sanitizeRedirectPath(params.get("redirect"));
              }

              function showError(message) {
                var target = document.getElementById("login-error-fallback");
                if (!(target instanceof HTMLElement)) {
                  target = document.createElement("p");
                  target.id = "login-error-fallback";
                  target.className = "rounded-xl bg-[#ffe9e9] px-3 py-2 text-sm text-[#b71d1d]";
                  var form = document.getElementById("login-form");
                  if (form instanceof HTMLElement) {
                    form.appendChild(target);
                  } else {
                    return;
                  }
                }
                target.textContent = message;
              }

              function bindNativeSubmitFallback() {
                var form = document.getElementById("login-form");
                if (!(form instanceof HTMLFormElement) || form.dataset.nativeBound === "1") {
                  return;
                }
                form.dataset.nativeBound = "1";
                form.addEventListener("submit", async function (event) {
                  if (window.__spLoginHydrated) {
                    return;
                  }

                  event.preventDefault();

                  var emailInput = document.getElementById("login-email");
                  var passwordInput = document.getElementById("login-password");
                  if (!(emailInput instanceof HTMLInputElement) || !(passwordInput instanceof HTMLInputElement)) {
                    return;
                  }

                  var submitButton = document.getElementById("login-submit");
                  if (submitButton instanceof HTMLButtonElement) {
                    submitButton.disabled = true;
                  }

                  try {
                    var response = await fetch("/sp/auth/login", {
                      method: "POST",
                      headers: {
                        "Accept": "application/json",
                        "Content-Type": "application/json"
                      },
                      credentials: "include",
                      body: JSON.stringify({
                        email: emailInput.value,
                        password: passwordInput.value
                      })
                    });

                    var text = await response.text();
                    var errorMessage = "로그인 중 알 수 없는 오류가 발생했습니다.";

                    if (text) {
                      try {
                        var payload = JSON.parse(text);
                        if (payload && payload.error && payload.error.message) {
                          errorMessage = payload.error.message;
                        }
                      } catch (_) {}
                    }

                    if (!response.ok) {
                      showError(errorMessage);
                      return;
                    }

                    var sessionCheck = await fetch("/sp/auth/me", {
                      method: "GET",
                      headers: {
                        "Accept": "application/json"
                      },
                      credentials: "include",
                      cache: "no-store"
                    });
                    if (!sessionCheck.ok) {
                      showError("로그인은 성공했지만 세션이 유지되지 않았습니다. 동일한 주소로 다시 접속 후 로그인해주세요.");
                      return;
                    }

                    window.location.href = resolveRedirectPath();
                  } catch (_) {
                    showError("네트워크 오류로 로그인에 실패했습니다.");
                  } finally {
                    if (submitButton instanceof HTMLButtonElement) {
                      submitButton.disabled = false;
                    }
                  }
                });
              }

              if (document.readyState === "loading") {
                document.addEventListener("DOMContentLoaded", bindNativeSubmitFallback);
              } else {
                bindNativeSubmitFallback();
              }
            })();
          `,
        }}
      />
    </>
  );
}
