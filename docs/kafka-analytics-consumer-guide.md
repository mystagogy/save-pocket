# Kafka Analytics Consumer 가이드 (P4)

## 1. 목표
`wish.events.v1`를 소비해 사용자 기준 일별/월별 집계 테이블을 비동기로 갱신합니다.

현재 범위:
- 이벤트 소비: `WISH_EXPIRED`, `WISH_PURCHASED`
- 집계 테이블:
  - `wish_event_analytics_daily`
  - `wish_event_analytics_monthly`
- 중복 방지 체크포인트:
  - `wish_event_analytics_checkpoint`

## 2. 동작 흐름
1. `WishEventAnalyticsConsumer`가 Kafka 이벤트 수신
2. `WishEventAnalyticsService`가 이벤트 유효성/중복 체크
3. `ProductWish` 조회 후 금액 계산
4. 일별/월별 집계 row upsert(찾아서 누적)
5. 체크포인트 저장(`event_id`)

## 3. 환경변수
```env
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
WISH_EVENTS_KAFKA_ENABLED=true
WISH_EVENTS_KAFKA_TOPIC=wish.events.v1
WISH_EVENTS_KAFKA_CONSUMER_ENABLED=true
WISH_EVENTS_KAFKA_CONSUMER_GROUP_ID=save-pocket-analytics-v1
```

주의:
- Consumer를 켜기 전 Kafka 브로커 연결 가능 여부를 먼저 확인
- 운영에서 Kafka 미사용이면 `WISH_EVENTS_KAFKA_CONSUMER_ENABLED=false`

## 4. 이벤트 반영 규칙
- `WISH_EXPIRED`
  - `expired_count +1`
  - `expired_amount += savedAmount` (없으면 `effectivePrice` 폴백)
- `WISH_PURCHASED`
  - `purchased_count +1`
  - `purchased_amount += effectivePrice`
- `net_amount = expired_amount - purchased_amount`

## 5. 검증 포인트
- 같은 `eventId` 재수신 시 중복 반영되지 않아야 함
- 일/월 집계가 동일한 기준 금액으로 누적되는지 확인
- Consumer 비활성화 시 앱 정상 구동 여부 확인
