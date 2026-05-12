# 작심삼일 긴축재정 - DB/API 설계서 (MVP v1)

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

## 2.3 인덱스 설계
```sql
CREATE UNIQUE INDEX ux_users_email ON users (email);

CREATE INDEX idx_wish_user_status_expire
ON product_wish (user_id, status, expire_at);

CREATE INDEX idx_wish_status_expire
ON product_wish (status, expire_at);

CREATE INDEX idx_wish_user_updated
ON product_wish (user_id, updated_at DESC);

CREATE INDEX idx_price_history_wish_changed
ON price_history (wish_id, changed_at DESC);

CREATE INDEX idx_event_history_wish_event
ON wish_event_history (wish_id, event_at DESC);
```

## 2.4 상태 전이 규칙
1. 등록: `WAITING` + `lastViewedAt=now` + `expireAt=now+72h`
2. 조회(상태 WAITING): `lastViewedAt=now`, `expireAt=now+72h`
3. 조회(상태 EXPIRED): 이벤트 `REACTIVATED` 기록 후 `WAITING` 복귀, `reactivatedCount+1`
4. 구매: `PURCHASED`
5. 삭제: `DELETED`
6. 만료 스케줄: `WAITING` 중 `expireAt <= now`를 `EXPIRED`로 변경, `savedAmount=effectivePrice`

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
  "memo": "이번엔 꼭 3일 고민",
  "manual": {
    "productName": "선택 입력",
    "referencePrice": 129000,
    "productImageUrl": "https://image.example.com/a.jpg",
    "userDealPrice": 119000,
    "dealUrl": "https://instagram.com/...",
    "dealSourceType": "INFLUENCER"
  }
}
```
- 동작
- 네이버 API 성공 시 자동값 우선 저장
- 실패 시 `manual` 필수값 검증 후 수동 저장
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

### POST /wishes/{id}/view
- 동작
- WAITING: `lastViewedAt/expireAt` 연장
- EXPIRED: `REACTIVATED` 이벤트 + `WAITING` 복귀 + `reactivatedCount+1`
- Response `200`
```json
{
  "id": 10,
  "status": "WAITING",
  "lastViewedAt": "2026-05-11T10:00:00+09:00",
  "expireAt": "2026-05-14T10:00:00+09:00",
  "reactivatedCount": 1
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
- `AUTH_REQUIRED` (401)
- `FORBIDDEN_RESOURCE` (403)
- `VALIDATION_ERROR` (400)
- `WISH_NOT_FOUND` (404)
- `INVALID_STATUS_TRANSITION` (409)
- `EXTERNAL_API_FAILED` (502)
- `INTERNAL_SERVER_ERROR` (500)

## 3.6 프론트엔드 연동 메모 (Next.js)
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
- 만료 판정: `0 */10 * * * *`
- 가격 갱신: `0 0 */6 * * *`
- 대상 조회
  - 만료: `status='WAITING' AND expire_at <= now`
  - 가격 갱신: `status IN ('WAITING')`
- 실패 처리
  - 상품 단위 try/catch
  - 실패 건만 warn 로깅, 배치는 계속

## 5. 구현 시 검증 체크리스트
- [ ] 72시간 연장 규칙 테스트
- [ ] EXPIRED -> VIEW -> WAITING 재활성화 테스트
- [ ] 만료 시 `savedAmount=effectivePrice` 저장 테스트
- [ ] 가격 변동 시 `price_history` + `PRICE_CHANGED` 이벤트 생성 테스트
- [ ] 인증 없는 접근 차단(401) 테스트
- [ ] 타 사용자 리소스 접근 차단(403) 테스트
