import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "작심삼일 긴축재정",
  description: "소비 보류 위시를 관리하고 절약 금액을 추적하는 웹앱",
  other: {
    google: "notranslate",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      className="h-full antialiased notranslate"
      suppressHydrationWarning
      translate="no"
    >
      <body className="min-h-full flex flex-col notranslate" suppressHydrationWarning translate="no">
        {children}
      </body>
    </html>
  );
}
