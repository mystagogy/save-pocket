# my-webapp

작심삼일 긴축재정 프론트엔드(Next.js App Router) 프로젝트입니다.

## 실행
```bash
npm install
npm run dev
```

- 기본 주소: `http://localhost:3000`
- 백엔드 API 프록시: `/sp/*` -> `BACKEND_ORIGIN` (기본 `http://localhost:8080`)

## 주요 페이지
- `/login` 로그인
- `/signup` 회원가입
- `/wishes/search` 위시 상품 검색
- `/wishes/new` 위시 등록
- `/wishes` 위시 목록
- `/wishes/{id}` 위시 상세
- `/reports/monthly` 월간 리포트

## 위시 등록 연동 메모
- 검색 결과에서 등록 이동 시 `trackedProductId`를 함께 전달합니다.
- 등록 폼 제출(`POST /wishes/create`) 시 `trackedProductId`가 백엔드 `POST /wishes` payload에 포함됩니다.
- 백엔드가 URL 기반 식별자 파싱을 우선 적용하므로 사용자가 URL을 수정한 경우에도 URL 기준 식별자가 저장됩니다.
