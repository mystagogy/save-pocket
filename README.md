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
  - Redis (세션 저장소)
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
docker compose up -d
./gradlew bootRun
```

필수 환경변수(예시):
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
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

## Redis 운영 가이드
현재 Redis는 다음 두 용도로 사용합니다.
- 세션 저장소: `spring.session.store-type=redis`
- 월간 리포트 캐시: `spring.cache.type=redis`

주요 환경변수(`save-pocket/.env`):
- `REDIS_HOST` (기본 `localhost`)
- `REDIS_PORT` (기본 `6379`)
- `REDIS_PASSWORD` (비어 있으면 무암호)
- `SESSION_TIMEOUT` (기본 `30m`)
- `SESSION_NAMESPACE` (기본 `save-pocket:session`)
- `REPORT_CACHE_TTL` (기본 `PT10M`)

### 캐시/세션 확인 방법
1) Redis 컨테이너 상태 확인
```bash
cd save-pocket
docker compose ps
```

2) 월간 리포트 캐시 키 확인
```bash
# 비밀번호 설정 시
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" --scan --pattern 'monthlySavings*'

# 무암호 Redis일 때
docker compose exec redis redis-cli --scan --pattern 'monthlySavings*'
```

3) TTL 확인
```bash
docker compose exec redis redis-cli TTL 'monthlySavings::1:2026:5'
```

4) 세션 키 확인
```bash
docker compose exec redis redis-cli --scan --pattern 'save-pocket:session*'
```

### 무효화 정책
- 월간 리포트 캐시 키 형식: `monthlySavings::<userId>:<year>:<month>`
- 위시 상태 변경(구매/삭제/재활성화), 만료 스케줄 실행 시 캐시 무효화
- 무효화 시점: 트랜잭션 `afterCommit` (커밋 후 Redis eviction)

## 문서
- 기획안: [docs/project-plan.md](docs/project-plan.md)
- DB/API 설계서: [docs/db-api-design.md](docs/db-api-design.md)
- Redis 학습 가이드: [docs/redis-guide.md](docs/redis-guide.md)
