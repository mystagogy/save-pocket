# 작심삼일 긴축재정 - 프로젝트 기획안 (MVP v2)

## 1. 프로젝트 개요
- 프로젝트명: 작심삼일 긴축재정
- 한 줄 소개: 사용자의 반복 조회 행동을 기준으로 구매 관심도를 판단하고, 일정 기간 관심이 사라진 상품을 자동 만료 처리해 절약 금액으로 기록하는 소비 절제 서비스.

## 2. 기획 배경
"사고 싶은 물건이 생겼을 때 3일 동안 계속 생각나면 사고, 아니면 사지 마라"라는 소비 습관에서 시작했다.
사용자는 충동구매를 즉시 실행하지 않고 상품을 보류 등록한다.

## 3. 문제 정의
- 기존 가계부는 구매 이후의 기록 중심이다.
- 실제로는 구매 이전의 충동 구간 관리가 더 중요하다.
- 보류, 재관심, 만료, 재활성화 흐름을 추적할 수 있는 서비스가 필요하다.

## 4. 핵심 가치
- 구매 보류
- 관심 지속 여부 추적
- 가격 변동 확인
- 절약 기록
- 소비 패턴 관리

## 5. MVP 기능 범위
### 5.1 인증
- 세션/쿠키 기반 회원가입, 로그인, 로그아웃

### 5.2 상품 등록
- 입력: 상품 URL, 메모(선택)
- 자동 조회: 상품명, 이미지, 기준 가격
- 상품 식별자(`trackedProductId`) 저장(검색 결과 또는 URL 파싱)
- 자동 조회 실패 시 수동 입력 폴백 허용

### 5.3 관심 기반 만료 로직
- 등록 시: `status=WAITING`, `lastViewedAt=now`, `expireAt=now+72h`
- 사용자가 다시 조회하면: `lastViewedAt` 갱신, `expireAt=now+72h` 재설정
- 만료 판정: `expireAt <= now`이면 `EXPIRED`

### 5.4 재활성화/구매/삭제
- EXPIRED 상품 재조회 시 재활성화 이벤트 기록 후 즉시 WAITING 복귀
- 구매 시 `PURCHASED`
- 삭제 시 `DELETED` (소프트 삭제 성격)

### 5.5 가격 추적
- 기본 12시간(0시/12시) 주기로 가격 재조회(환경변수로 주기 조정 가능)
- `trackedProductId` 일치 상품만 반영해 오탐 방지
- 변동 시 `price_history` + `PRICE_CHANGED` 이벤트 저장

### 5.6 스케줄 실행 이력
- 만료/가격 갱신 스케줄 실행 결과를 `scheduler_run_history`에 저장
- 상태(`SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`) 및 처리 건수 기록

### 5.7 월 리포트
- 참은 상품 수
- 총 절약 금액
- 가격 하락 상품 수
- 카테고리 지표는 초기 MVP에서 미분류/기타 처리 허용

## 6. 가격 정책 (자동 + 수동 병행)
- `referencePrice`: 외부 API 기준가
- `userDealPrice`: 사용자가 직접 입력한 체감 최저가(SNS/공구/인플루언서)
- `effectivePrice`: 절약 계산 기준가 (`userDealPrice` 우선, 없으면 `referencePrice`)
- `savedAmount`: 만료 시점 `effectivePrice`

추가 필드:
- `dealUrl` (선택)
- `dealSourceType` (`NAVER`, `SNS`, `INFLUENCER`, `MANUAL`)

## 7. 데이터 기준 주의사항
공공데이터포털 15080757(온라인 수집 가격 정보)는 개별 상품 URL 기준 최저가가 아니라 품목코드+조회일자 기반 통계성 가격 데이터다.
따라서 서비스 내에서는 참고 가격(`referencePrice`)으로 활용하고, 실제 체감 최저가는 수동 입력가(`userDealPrice`)로 병행 관리한다.

## 8. 상태 및 이벤트 모델
### 8.1 상태
- `WAITING`
- `EXPIRED`
- `PURCHASED`
- `DELETED`

### 8.2 이벤트
- `REGISTERED`
- `VIEWED`
- `PRICE_CHANGED`
- `EXPIRED`
- `REACTIVATED`
- `PURCHASED`
- `DELETED`
- `MANUAL_PRICE_UPDATED`

## 9. 스케줄 정책
- 시간대: `Asia/Seoul`
- 만료 판정 스케줄: 기본 10분 주기 (`WISH_EXPIRATION_CRON`)
- 가격 갱신 스케줄: 기본 12시간 주기 (`WISH_PRICE_REFRESH_CRON`)
- 실패 처리: 상품 단위 실패 격리, 전체 배치 중단 방지

## 10. 기술 스택
- 백엔드
  - Java 17
  - Spring Boot
  - Spring Scheduler
  - Spring Data JPA
  - MySQL
  - Redis
  - RabbitMQ
  - Kafka
  - Swagger/OpenAPI
- 프론트엔드
  - Next.js 16 (App Router)
  - React 19
  - TypeScript
  - Tailwind CSS 4
- (확장 후보) 소셜 로그인

## 11. API 초안
### 인증
- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/logout`

### Wish
- `POST /wishes`
- `GET /wishes/search?query=...`
- `GET /wishes?status=...`
- `GET /wishes/{id}`
- `POST /wishes/{id}/purchase`
- `POST /wishes/{id}/delete`
- `POST /wishes/{id}/reactivate`

### 리포트
- `GET /reports/monthly?year=YYYY&month=MM`

## 12. 성공 기준
- 72시간 만료 로직이 사용자 행동 기반으로 정확히 작동
- 만료/재활성화/가격변동 이벤트 이력이 누락 없이 저장
- 절약 금액이 월 리포트에서 일관되게 집계
- 수동 가격 폴백으로 실사용 상황(SNS 공구 등)을 반영 가능

## 13. 비동기 이벤트 아키텍처(적용 현황)
- P1 완료: RabbitMQ + SSE 알림 파이프라인
- P2 완료: 가격 갱신 작업 RabbitMQ 큐 분리
- P3 진행: 위시 도메인 이벤트 Kafka 발행(Producer 우선)
- P4 예정: Kafka Consumer 기반 분석/통계 파이프라인 분리
