import { Suspense } from "react";
import LoginPanel from "@/components/LoginPanel";

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginPanel />
    </Suspense>
  );
}
