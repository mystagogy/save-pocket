# 작심삼일 긴축재정

충동구매를 줄이기 위해 상품을 일정 기간 보류하고, 관심이 사라진 상품을 절약 금액으로 기록하는 소비 관리 서비스입니다.

## 프로젝트 소개
- 핵심 아이디어: "3일 동안 계속 생각나면 사고, 아니면 사지 말자"
- 사용자 행동(재조회) 기반으로 상품 보류 기간을 연장합니다.
- 일정 시간 관심이 끊긴 상품은 자동 만료 처리하고 절약 금액으로 집계합니다.

## MVP 핵심 기능
- 회원가입/로그인(세션 기반)
- 상품 URL 등록 + 가격/이미지 자동 조회
- 자동 조회 실패 시 수동 입력 폴백
- 72시간 관심 기반 만료 로직
- 가격 변동 추적 및 이벤트 이력 저장
- 월별 절약 리포트

## 프로젝트 구성
- 백엔드 API: `save-pocket` (Spring Boot)
- 프론트엔드 웹앱: `my-webapp` (Next.js)

## 기술 스택
- 백엔드
  - Java 17
  - Spring Boot
  - Spring Data JPA
  - MySQL
  - Swagger/OpenAPI
- 프론트엔드
  - Next.js 16 (App Router)
  - React 19
  - TypeScript
  - Tailwind CSS 4

## 로컬 실행
### 1) 백엔드 실행 (`save-pocket`)
```bash
cd save-pocket
./gradlew bootRun
```

필수 환경변수(예시):
- `DB_USERNAME`
- `DB_PASSWORD`
- `NAVER_CLIENT_ID`
- `NAVER_CLIENT_SECRET`

### 2) 프론트엔드 실행 (`my-webapp`)
```bash
cd my-webapp
npm install
npm run dev
```

기본 접속:
- 프론트: `http://localhost:3000`
- 백엔드: `http://localhost:8080`

프론트는 `/sp/*` 경로를 백엔드로 프록시합니다.
필요 시 `my-webapp`에서 `BACKEND_ORIGIN` 환경변수로 백엔드 주소를 변경할 수 있습니다.

## 문서
- 기획안: [docs/project-plan.md](docs/project-plan.md)
- DB/API 설계서: [docs/db-api-design.md](docs/db-api-design.md)
