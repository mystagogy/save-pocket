# RabbitMQ 가격 갱신 큐화 가이드 (P2)

## 1. 목적
P2의 목표는 `가격 갱신 작업`을 스케줄러에서 직접 처리하지 않고 RabbitMQ 큐로 분리하는 것입니다.

- 기존: 스케줄러가 WAITING 위시를 직접 순회하며 가격 조회/갱신 실행
- 변경: 스케줄러는 "작업 생성(enqueue)"만 하고, Consumer가 실제 갱신을 수행

핵심은 `실행 경로 분리`입니다.

## 2. 왜 필요한가
가격 갱신은 외부 API 호출(네이버 쇼핑)과 DB 갱신을 포함해 상대적으로 무거운 작업입니다.
작업량이 증가하면 다음 문제가 생길 수 있습니다.

- 스케줄러 실행 시간이 길어져 다음 스케줄과 충돌
- 외부 API 일시 장애가 스케줄러 실패로 직접 전파
- 장애 원인 분석 시 "스케줄러 실패"와 "실제 처리 실패"가 섞여 추적 어려움

큐화 후 기대효과:
- 스케줄러는 짧게 끝남(큐 발행만 수행)
- 실제 처리 실패는 Consumer/DLQ 경로로 분리
- 재시도/확장(컨슈머 수평 확장) 기반 마련

## 3. 역할 분리
### 3.1 Scheduler (Producer 역할)
- 컴포넌트: `WishPriceRefreshScheduler`
- 역할: 실행 시점에 WAITING 위시 ID 목록을 조회하고 큐에 메시지 발행
- 결과: 작업 발행 통계(`scanned/updated/failed`)를 스케줄러 이력으로 기록

### 3.2 Dispatcher
- 인터페이스: `WishPriceRefreshDispatcher`
- 구현 2종:
  - `RabbitWishPriceRefreshDispatcher` (`WISH_PRICE_REFRESH_RABBITMQ_ENABLED=true`)
  - `DirectWishPriceRefreshDispatcher` (`false` 또는 미설정)

즉, 플래그에 따라 큐 모드/기존 직접 실행 모드를 안전하게 전환합니다.

### 3.3 Consumer
- 컴포넌트: `WishPriceRefreshMessageConsumer`
- 역할: `PriceRefreshMessage(wishId, requestedAt)` 수신 후 단건 갱신 수행
- 호출: `WishService.refreshLowestReferencePriceByWishId`

## 4. 동작 흐름
1. 크론 트리거로 `WishPriceRefreshScheduler` 실행
2. Dispatcher가 WAITING 위시 ID를 조회
3. 각 위시 ID를 `wish.price-refresh.queue`에 발행
4. Consumer가 메시지를 받아 단건 가격 갱신 실행
5. 가격 하락이 역대 최저가 조건을 만족하면(P1 로직) 알림 이벤트도 후속 발행

## 5. 큐 토폴로지
- Exchange: `wish.price-refresh.exchange` (direct)
- Queue: `wish.price-refresh.queue`
- DLQ: `wish.price-refresh.dlq`
- Routing Key: `wish.price-refresh.request`

큐 속성:
- durable 큐
- 메인 큐 실패 메시지는 DLQ 라우팅
- `default-requeue-rejected=false`로 무한 재큐잉 방지

## 6. 설정값
`save-pocket/.env` 기준:

```env
WISH_PRICE_REFRESH_RABBITMQ_ENABLED=false
WISH_PRICE_REFRESH_RABBITMQ_EXCHANGE=wish.price-refresh.exchange
WISH_PRICE_REFRESH_RABBITMQ_QUEUE=wish.price-refresh.queue
WISH_PRICE_REFRESH_RABBITMQ_DLQ=wish.price-refresh.dlq
WISH_PRICE_REFRESH_RABBITMQ_ROUTING_KEY=wish.price-refresh.request
```

운영 전환 시:
- 큐 모드 ON: `WISH_PRICE_REFRESH_RABBITMQ_ENABLED=true`
- 안전 롤백: `WISH_PRICE_REFRESH_RABBITMQ_ENABLED=false`

## 7. 적용 시 변경점 요약
P2에서 실제 반영된 핵심 변경:

- 스케줄러가 직접 실행 대신 Dispatcher 호출
- `WishService`에 단건 갱신 메서드 추가
- `ProductWishRepository`에 WAITING ID 조회 쿼리 추가
- 가격 갱신용 RabbitMQ Config/Properties/Message/Consumer 추가
- Rabbit 메시지 컨버터를 공용 설정으로 분리

## 8. 보안/단순성 원칙
- 메시지 payload는 최소 필드(`wishId`, `requestedAt`)만 포함
- 비밀번호/토큰/PII를 큐에 담지 않음
- 기능 플래그로 단계적 활성화(기본 OFF)
- 복잡한 Outbox/체인 라우팅은 P2 범위에서 제외

## 9. 테스트 방법
### 9.1 자동 테스트
```bash
cd save-pocket
./gradlew test
```

### 9.2 수동 테스트(큐 모드)
1) `WISH_PRICE_REFRESH_RABBITMQ_ENABLED=true`
2) 서버 실행 후 스케줄 로그 확인
3) RabbitMQ에서 아래 큐 상태 확인:
   - `wish.price-refresh.queue`
   - `wish.price-refresh.dlq`
4) 기대 결과:
   - 메인 큐 적체 없음
   - DLQ 누적 없음
   - WAITING 위시 기준가 갱신 반영

## 10. 장애 대응
- RabbitMQ 연결 실패: 기능 플래그를 `false`로 내려 즉시 직접 실행 모드로 복귀
- DLQ 누적: 소비 예외 원인 분석 후 재처리
- 외부 API 품질 이슈: 스케줄러 전체 실패 대신 단건 실패로 격리되어 영향 범위 축소
