# 작심삼일 긴축재정

운영 URL: https://save-pocket.vercel.app

충동구매를 줄이기 위해 상품을 일정 기간 보류하고, 관심이 사라진 상품을 절약 금액으로 기록하는 소비 관리 서비스입니다.

## 프로젝트 소개
- 핵심 아이디어: "3일 동안 계속 생각나면 사고, 아니면 사지 말자"
- 사용자 행동(재조회) 기반으로 상품 보류 기간을 연장합니다.
- 일정 시간 관심이 끊긴 상품은 자동 만료 처리하고 절약 금액으로 집계합니다.

## MVP 핵심 기능
- 회원가입/로그인(세션 기반)
- 로그인 사용자 정보 조회/수정(닉네임 변경, 비밀번호 변경)
- 상품 URL 등록 + 가격/이미지 자동 조회
- 동일 상품 식별자(`trackedProductId`) 기반 가격 갱신
- 자동 조회 실패 시 수동 입력 폴백
- 72시간 관심 기반 만료 로직
- 가격 변동 추적 및 이벤트 이력 저장
- 가격 하락/일일 절약 합산 알림
- 스케줄러 실행 이력 저장(`scheduler_run_history`)
- 월별 절약 리포트

## 프로젝트 구성
- 백엔드 API: `save-pocket` (Spring Boot)
- 프론트엔드 웹앱: `my-webapp` (Next.js)

## 배포 주소
- 프론트(운영): `https://save-pocket.vercel.app`
- 백엔드 API(운영): `https://save-pocket.up.railway.app`

## 기술 스택
- 백엔드
  - Java 17
  - Spring Boot
  - Spring Data JPA
  - MySQL
  - Redis (세션 저장소)
  - RabbitMQ (알림 이벤트 큐)
  - Kafka (위시 도메인 이벤트 스트림)
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
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `NOTIFICATION_DAILY_SAVINGS_ENABLED` (기본: `true`)
- `NOTIFICATION_DAILY_SAVINGS_CRON` (기본: `0 0 22 * * *`)
- `WISH_PRICE_REFRESH_RABBITMQ_ENABLED` (기본: `false`)
- `KAFKA_BOOTSTRAP_SERVERS` (기본: `localhost:9092`)
- `WISH_EVENTS_KAFKA_ENABLED` (기본: `false`)
- `WISH_EVENTS_KAFKA_TOPIC` (기본: `wish.events.v1`)
- `WISH_EVENTS_KAFKA_CONSUMER_ENABLED` (기본: `false`)
- `WISH_EVENTS_KAFKA_CONSUMER_GROUP_ID` (기본: `save-pocket-analytics-v1`)
- `NAVER_CLIENT_ID`
- `NAVER_CLIENT_SECRET`
- `WISH_EXPIRATION_CRON` (기본: `0 */10 * * * *`)
- `WISH_PRICE_REFRESH_CRON` (기본: `0 0 0,12 * * *`)

### 2) 프론트엔드 실행 (`my-webapp`)
```bash
cd my-webapp
npm install
npm run dev
```

배포된 백엔드(Railway)와 로컬 프론트를 바로 연동해 테스트하려면:
```bash
cd my-webapp
npm run dev:remote
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

## RabbitMQ 알림 가이드
현재 RabbitMQ는 아래 2가지 파이프라인 용도로 사용합니다.

1) P1: 가격 하락 알림 파이프라인
- Exchange: `wish.notification.exchange`
- Queue: `wish.notification.queue`
- DLQ: `wish.notification.dlq`
- Routing Key: `wish.notification.price-drop`

2) P2: 가격 갱신 작업 큐 파이프라인
- Exchange: `wish.price-refresh.exchange`
- Queue: `wish.price-refresh.queue`
- DLQ: `wish.price-refresh.dlq`
- Routing Key: `wish.price-refresh.request`

주요 환경변수(`save-pocket/.env`):
- `RABBITMQ_HOST` (기본 `localhost`)
- `RABBITMQ_PORT` (기본 `5672`)
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `RABBITMQ_VHOST` (기본 `/`)
- `NOTIFICATION_RABBITMQ_ENABLED` (기본 `false`, `true`일 때만 RabbitMQ 알림 파이프라인 활성화)
- `WISH_PRICE_REFRESH_RABBITMQ_ENABLED` (기본 `false`, `true`일 때만 가격 갱신 큐 파이프라인 활성화)

참고:
- `일일 절약 합산 알림`은 RabbitMQ를 거치지 않고 스케줄러에서 직접 생성 후 SSE로 발송합니다.
- 설정값:
  - `NOTIFICATION_DAILY_SAVINGS_ENABLED` (기본 `true`)
  - `NOTIFICATION_DAILY_SAVINGS_CRON` (기본 `0 0 22 * * *`)

관리 콘솔:
- `http://localhost:15672`

보안 권장사항:
- 운영 환경에서는 관리 콘솔(`15672`)을 외부에 공개하지 않습니다.
- RabbitMQ 계정은 기본 `guest` 대신 전용 사용자 계정을 사용합니다.
- 큐 payload에 비밀번호/토큰/개인식별정보를 포함하지 않습니다.

## Kafka 위시 이벤트 가이드 (P3)
P3에서는 위시 도메인 변경을 Kafka 이벤트로 발행합니다.

- Topic: `wish.events.v1`
- Key: `wishId` (없으면 `eventId`)
- 발행 시점: 트랜잭션 `afterCommit`
- 기본값: `WISH_EVENTS_KAFKA_ENABLED=false` (명시적으로 켤 때만 발행)

주요 환경변수(`save-pocket/.env`):
- `KAFKA_BOOTSTRAP_SERVERS` (예: `localhost:9092`)
- `WISH_EVENTS_KAFKA_ENABLED` (기본 `false`)
- `WISH_EVENTS_KAFKA_TOPIC` (기본 `wish.events.v1`)

운영 팁:
- Kafka 브로커가 없으면 `WISH_EVENTS_KAFKA_ENABLED=false`로 유지합니다.
- 활성화 시 `schemaVersion=1` 기반 이벤트 계약으로 발행됩니다.
- 통계 컨슈머를 사용할 때만 `WISH_EVENTS_KAFKA_CONSUMER_ENABLED=true`로 켭니다.

## 가격 갱신 정책
- 갱신 대상: `WAITING` 상태 위시
- 매칭 기준: `trackedProductId`가 일치하는 상품만 반영
- 식별자 추출: 등록 시 URL에서 추출 우선(`id`, `nvMid`, `productNo`, `/item/{id}`, `/catalog/{id}`, `/products/{id}`)
- 정확한 식별자를 못 찾는 경우: 가격 갱신 스킵(오탐 반영 방지)

## 문서
- 기획안: [docs/project-plan.md](docs/project-plan.md)
- DB/API 설계서: [docs/db-api-design.md](docs/db-api-design.md)
- Redis 학습 가이드: [docs/redis-guide.md](docs/redis-guide.md)
- RabbitMQ 기술 설명서(입문): [docs/rabbitmq-basics.md](docs/rabbitmq-basics.md)
- RabbitMQ 알림 가이드: [docs/rabbitmq-notification-guide.md](docs/rabbitmq-notification-guide.md)
- RabbitMQ 가격 갱신 큐화 가이드(P2): [docs/rabbitmq-price-refresh-guide.md](docs/rabbitmq-price-refresh-guide.md)
- Kafka 기술 설명서(입문): [docs/kafka-basics.md](docs/kafka-basics.md)
- Kafka 위시 이벤트 가이드(P3): [docs/kafka-wish-events-guide.md](docs/kafka-wish-events-guide.md)
- Kafka P3.5 안정화 가이드: [docs/kafka-p35-stability-guide.md](docs/kafka-p35-stability-guide.md)
- Kafka Analytics Consumer 가이드(P4): [docs/kafka-analytics-consumer-guide.md](docs/kafka-analytics-consumer-guide.md)
