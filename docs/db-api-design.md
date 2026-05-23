# 작심삼일 긴축재정 - DB/API 설계서 (MVP v2)

## 1. 설계 원칙
- 타임존: `Asia/Seoul`
- 만료 기준: `expireAt <= now`
- 관심 유지 기준: 마지막 조회 시점부터 `72시간`
- 가격 우선순위: `effectivePrice = COALESCE(userDealPrice, referencePrice)`
- 재활성화 처리: 상태값이 아닌 이벤트(`REACTIVATED`) 기록 후 즉시 `WAITING` 상태 복귀

## 2. DB 설계

## 2.1 ERD 관계
- `users (1) ---- (N) product_wish`
- `product_wish (1) ---- (N) price_history`
- `product_wish (1) ---- (N) wish_event_history`
- `scheduler_run_history` (스케줄 실행 로그 독립 테이블)

## 2.2 테이블 정의

### users
| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AI | 사용자 ID |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 로그인 이메일 |
| password_hash | VARCHAR(255) | NOT NULL | 비밀번호 해시 |
| nickname | VARCHAR(50) | NOT NULL | 닉네임 |
| created_at | DATETIME | NOT NULL | 생성시각 |
| updated_at | DATETIME | NOT NULL | 수정시각 |

### product_wish
| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AI | 위시 ID |
| user_id | BIGINT | NOT NULL, FK(users.id) | 소유자 |
| product_name | VARCHAR(255) | NOT NULL | 상품명 |
| product_url | TEXT | NOT NULL | 원본 상품 URL |
| tracked_product_id | VARCHAR(100) | NULL | 가격 추적용 외부 상품 식별자 |
| product_image_url | TEXT | NULL | 대표 이미지 URL |
| memo | VARCHAR(500) | NULL | 사용자 메모 |
| reference_price | BIGINT | NULL | 자동 조회 기준가 |
| user_deal_price | BIGINT | NULL | 사용자 입력 체감가 |
| deal_url | TEXT | NULL | 공구/인플루언서 링크 |
| deal_source_type | VARCHAR(20) | NULL | NAVER/SNS/INFLUENCER/MANUAL |
| status | VARCHAR(20) | NOT NULL | WAITING/EXPIRED/PURCHASED/DELETED |
| first_registered_at | DATETIME | NOT NULL | 최초 등록시각 |
| last_viewed_at | DATETIME | NOT NULL | 마지막 조회시각 |
| expire_at | DATETIME | NOT NULL | 만료 예정시각 |
| expired_at | DATETIME | NULL | 실제 만료시각 |
| reactivated_count | INT | NOT NULL DEFAULT 0 | 재활성화 횟수 |
| saved_amount | BIGINT | NULL | 만료 시 절약금 |
| created_at | DATETIME | NOT NULL | 생성시각 |
| updated_at | DATETIME | NOT NULL | 수정시각 |

### price_history
| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AI | 이력 ID |
| wish_id | BIGINT | NOT NULL, FK(product_wish.id) | 위시 ID |
| price_type | VARCHAR(20) | NOT NULL | REFERENCE/USER_DEAL |
| previous_price | BIGINT | NOT NULL | 이전 가격 |
| changed_price | BIGINT | NOT NULL | 변경 가격 |
| changed_at | DATETIME | NOT NULL | 변경시각 |
| created_at | DATETIME | NOT NULL | 생성시각 |

### wish_event_history
| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AI | 이벤트 ID |
| wish_id | BIGINT | NOT NULL, FK(product_wish.id) | 위시 ID |
| event_type | VARCHAR(30) | NOT NULL | REGISTERED/VIEWED/PRICE_CHANGED/EXPIRED/REACTIVATED/PURCHASED/DELETED/MANUAL_PRICE_UPDATED |
| event_at | DATETIME | NOT NULL | 이벤트 시각 |
| description | VARCHAR(500) | NULL | 이벤트 설명 |
| metadata | JSON | NULL | 부가 데이터 |
| created_at | DATETIME | NOT NULL | 생성시각 |

### scheduler_run_history
| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AI | 실행 이력 ID |
| job_name | VARCHAR(100) | NOT NULL | 스케줄러 작업명 |
| status | VARCHAR(20) | NOT NULL | SUCCESS/PARTIAL_SUCCESS/FAILED |
| executed_at | DATETIME | NOT NULL | 실행 시작 시각 |
| finished_at | DATETIME | NOT NULL | 실행 종료 시각 |
| scanned_count | INT | NOT NULL | 조회 대상 건수 |
| success_count | INT | NOT NULL | 성공 처리 건수 |
| skipped_count | INT | NOT NULL | 스킵 건수 |
| failed_count | INT | NOT NULL | 실패 건수 |
| error_message | VARCHAR(1000) | NULL | 실패 메시지 |
| created_at | DATETIME | NOT NULL | 생성시각 |

### notification
| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AI | 알림 ID |
| user_id | BIGINT | NOT NULL, FK(users.id) | 수신 사용자 |
| wish_id | BIGINT | NOT NULL, FK(product_wish.id) | 관련 위시 |
| notification_type | VARCHAR(40) | NOT NULL | PRICE_DROP_LOWEST / DAILY_SAVED_SUMMARY |
| title | VARCHAR(150) | NOT NULL | 알림 제목 |
| message | VARCHAR(500) | NOT NULL | 알림 본문 |
| link_url | VARCHAR(300) | NULL | 프론트 이동 링크 |
| is_read | BIT(1) | NOT NULL, DEFAULT 0 | 읽음 여부 |
| read_at | DATETIME | NULL | 읽음 시각 |
| created_at | DATETIME | NOT NULL | 생성시각 |
| updated_at | DATETIME | NOT NULL | 수정시각 |

## 2.3 인덱스 설계
```sql
CREATE UNIQUE INDEX ux_users_email ON users (email);

CREATE INDEX idx_wish_user_status_expire
ON product_wish (user_id, status, expire_at);

CREATE INDEX idx_wish_status_expire
ON product_wish (status, expire_at);

CREATE INDEX idx_wish_user_updated
ON product_wish (user_id, updated_at DESC);

CREATE INDEX idx_product_wish_tracked_product_id
ON product_wish (tracked_product_id);

CREATE INDEX idx_price_history_wish_changed
ON price_history (wish_id, changed_at DESC);

CREATE INDEX idx_event_history_wish_event
ON wish_event_history (wish_id, event_at DESC);

CREATE INDEX idx_scheduler_run_history_job_executed
ON scheduler_run_history (job_name, executed_at DESC);

CREATE INDEX idx_notification_user_created
ON notification (user_id, created_at DESC);

CREATE INDEX idx_notification_user_unread
ON notification (user_id, is_read, created_at DESC);

CREATE INDEX idx_notification_dedup
ON notification (user_id, wish_id, notification_type, created_at DESC);
```

## 2.4 상태 전이 규칙
1. 등록: `WAITING` + `lastViewedAt=now` + `expireAt=now+72h`
2. 재활성화: `EXPIRED` -> `WAITING`, `expireAt=now+72h`, `reactivatedCount+1`
3. 구매: `PURCHASED`
4. 삭제: `DELETED`
5. 만료 스케줄: `WAITING` 중 `expireAt <= now`를 `EXPIRED`로 변경, `savedAmount=effectivePrice`

## 3. API 설계

## 3.1 공통 규칙
- 인증 방식: 세션/쿠키
- 응답 포맷(JSON):
```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-05-10T23:30:00+09:00"
}
```
- 실패 응답:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "WISH_NOT_FOUND",
    "message": "상품을 찾을 수 없습니다."
  },
  "timestamp": "2026-05-10T23:30:00+09:00"
}
```

## 3.2 인증 API

### POST /auth/signup
- Request
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "nickname": "절약러"
}
```
- Response `201`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "절약러"
}
```

### POST /auth/login
- Request
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```
- Response `200`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "절약러"
}
```

### POST /auth/logout
- Response `204`

## 3.3 Wish API

### POST /wishes
- Request
```json
{
  "productUrl": "https://smartstore.naver.com/sample/products/123",
  "trackedProductId": "123", 
  "memo": "이번엔 꼭 3일 고민",
  "productName": "나이키 운동화",
  "referencePrice": 129000,
  "productImageUrl": "https://image.example.com/a.jpg",
  "userDealPrice": 119000,
  "dealUrl": "https://instagram.com/...",
  "dealSourceType": "INFLUENCER"
}
```
- 동작
- `trackedProductId`는 URL 파싱값 우선(`id`, `nvMid`, `productNo`, `/item/{id}`, `/catalog/{id}`, `/products/{id}`)
- URL에서 추출 실패 시 요청 `trackedProductId` 사용
- Response `201`
```json
{
  "id": 10,
  "name": "나이키 운동화",
  "url": "https://smartstore.naver.com/sample/products/123",
  "imageUrl": "https://image.example.com/a.jpg",
  "referencePrice": 129000,
  "userDealPrice": 119000,
  "effectivePrice": 119000,
  "status": "WAITING",
  "lastViewedAt": "2026-05-10T23:00:00+09:00",
  "expireAt": "2026-05-13T23:00:00+09:00",
  "reactivatedCount": 0
}
```

### GET /wishes/search?query=에어팟
- Response `200`
```json
[
  {
    "name": "에어팟 프로",
    "url": "https://shopping.naver.com/item/1",
    "productId": "1234567890",
    "imageUrl": "https://img/1.jpg",
    "referencePrice": 299000,
    "mallName": "몰A"
  }
]
```

### GET /wishes?status=WAITING
- Response `200`
```json
[
  {
    "id": 10,
    "name": "나이키 운동화",
    "imageUrl": "https://image.example.com/a.jpg",
    "status": "WAITING",
    "effectivePrice": 119000,
    "expireAt": "2026-05-13T23:00:00+09:00"
  }
]
```

### GET /wishes/{id}
- Response `200`
```json
{
  "id": 10,
  "name": "나이키 운동화",
  "productUrl": "https://...",
  "referencePrice": 129000,
  "userDealPrice": 119000,
  "effectivePrice": 119000,
  "status": "WAITING",
  "lastViewedAt": "2026-05-10T23:00:00+09:00",
  "expireAt": "2026-05-13T23:00:00+09:00",
  "priceHistories": [],
  "events": []
}
```

### POST /wishes/{id}/purchase
- 동작: 상태를 `PURCHASED`로 변경
- Response `200`
```json
{
  "id": 10,
  "status": "PURCHASED"
}
```

### POST /wishes/{id}/delete
- 동작: 상태를 `DELETED`로 변경
- Response `200`
```json
{
  "id": 10,
  "status": "DELETED"
}
```

### POST /wishes/{id}/reactivate
- 동작: `EXPIRED` 상태를 `WAITING`으로 복귀, 만료시각 재설정
- Response `200`
```json
{
  "id": 10,
  "status": "WAITING"
}
```

## 3.4 Report API

### GET /reports/monthly?year=2026&month=5
- 집계 기준: `expiredAt`이 해당 월인 데이터
- Response `200`
```json
{
  "year": 2026,
  "month": 5,
  "heldCount": 5,
  "totalSavedAmount": 438000,
  "topHeldCategory": "기타",
  "priceDroppedCount": 2,
  "savedByReferencePrice": 210000,
  "savedByManualDealPrice": 228000
}
```

## 3.5 오류 코드 표준
- `UNAUTHORIZED` (401)
- `AUTH_INVALID_CREDENTIALS` (401)
- `FORBIDDEN_RESOURCE` (403)
- `VALIDATION_FAILED` (400)
- `WISH_NOT_FOUND` (404)
- `NOTIFICATION_NOT_FOUND` (404)
- `INVALID_WISH_STATE` (400)
- `MANUAL_INPUT_REQUIRED` (400)
- `EMAIL_ALREADY_EXISTS` (409)
- `INTERNAL_SERVER_ERROR` (500)

## 3.6 Notification API

### GET /notifications?limit=20
- Response `200`
```json
{
  "unreadCount": 2,
  "items": [
    {
      "id": 31,
      "wishId": 10,
      "notificationType": "PRICE_DROP_LOWEST",
      "title": "최저가 갱신",
      "message": "나이키 운동화 최저가가 120000원에서 99000원으로 내려갔어요.",
      "linkUrl": "/wishes/10",
      "read": false,
      "createdAt": "2026-05-21T10:30:00"
    },
    {
      "id": 32,
      "wishId": 18,
      "notificationType": "DAILY_SAVED_SUMMARY",
      "title": "최근 24시간 절약 리포트",
      "message": "최근 24시간 내에 총 12,300원을 아끼셨어요.",
      "linkUrl": "/reports/monthly",
      "read": false,
      "createdAt": "2026-05-23T22:00:00"
    }
  ]
}
```

### POST /notifications/{id}/read
- 동작: 알림 읽음 처리
- Response `200`
```json
{
  "id": 31,
  "read": true
}
```

### GET /notifications/stream
- 콘텐츠 타입: `text/event-stream`
- 이벤트명: `notification`
- 데이터: `NotificationItem` JSON

## 3.7 프론트엔드 연동 메모 (Next.js)
- 프론트 프로젝트 경로: `my-webapp`
- UI 주요 경로
  - `/` : 로그인 메인 화면
  - `/signup` : 회원가입
  - `/wishes/search` : 위시 상품 검색
  - `/wishes` : 위시 리스트
  - `/wishes/{id}` : 위시 상세
- 프론트는 `/sp/*` 경로를 백엔드 API로 프록시한다.
  - 예: `/sp/auth/login` -> `http://localhost:8080/auth/login`
  - 예: `/sp/wishes` -> `http://localhost:8080/wishes`

## 4. 스케줄러 설계
- 만료 판정: `${WISH_EXPIRATION_CRON:0 */10 * * * *}`
- 가격 갱신: `${WISH_PRICE_REFRESH_CRON:0 0 * * * *}`
- 일일 절약 합산 알림: `${NOTIFICATION_DAILY_SAVINGS_CRON:0 0 22 * * *}`
- 대상 조회
  - 만료: `status='WAITING' AND expire_at <= now`
  - 가격 갱신: `status IN ('WAITING')`
  - 일일 절약 합산 알림: 최근 24시간(`executedAt-24h` ~ `executedAt`) 내 `EXPIRED` + `savedAmount > 0` 데이터 사용자별 집계
- 중복 방지
  - `DAILY_SAVED_SUMMARY`는 동일 사용자 기준 최근 24시간 윈도우에 1회만 생성
- 실패 처리
  - 상품 단위 try/catch
  - 실패 건만 warn 로깅, 배치는 계속
- 실행 로그 저장
  - `scheduler_run_history`에 job/status/카운트/에러메시지 저장
  - 상태값: `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`

## 5. 구현 시 검증 체크리스트
- [ ] 72시간 연장 규칙 테스트
- [ ] EXPIRED -> VIEW -> WAITING 재활성화 테스트
- [ ] 만료 시 `savedAmount=effectivePrice` 저장 테스트
- [ ] 가격 변동 시 `price_history` + `PRICE_CHANGED` 이벤트 생성 테스트
- [ ] 인증 없는 접근 차단(401) 테스트
- [ ] 타 사용자 리소스 접근 차단(403) 테스트

## 6. Redis/캐시 설계

### 6.1 세션 저장소
- Spring Session Redis 사용
- 설정 키:
  - `spring.session.store-type=redis`
  - `spring.session.timeout`
  - `spring.session.redis.namespace`
- 기본 namespace: `save-pocket:session`

### 6.2 월간 리포트 캐시
- 대상 API: `GET /reports/monthly`
- 캐시 키: `monthlySavings::<userId>:<year>:<month>`
- TTL: `REPORT_CACHE_TTL` (기본 `PT10M`)
- 값: `MonthlySavingsResponse` 직렬화 결과

### 6.3 캐시 무효화 규칙
- 무효화 트리거:
  - 위시 구매(`PURCHASED`)
  - 위시 삭제(`DELETED`)
  - 위시 재활성화(`REACTIVATED`)
  - 만료 스케줄(`EXPIRED`)
- 무효화 시점:
  - 트랜잭션 내부 즉시 삭제가 아니라 `afterCommit`에 수행
  - DB 커밋 전에 캐시가 오래된 데이터로 재생성되는 경쟁 상태를 방지
