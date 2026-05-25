# Kafka 위시 이벤트 가이드 (P3)

## 1. 왜 필요한가
P3의 목적은 `WishService` 내부 변경(생성, 상태 변경, 가격 변경)을 동기 API 흐름과 분리해 이벤트 스트림으로 표준화하는 것입니다.

- API/배치의 핵심 로직은 DB 트랜잭션에 집중
- 후속 기능(분석, 추천, 통계, 알림 확장)은 Kafka Consumer에서 비동기 처리
- 도메인 변경 이력을 공통 포맷으로 축적

## 2. 현재 적용 범위
현재 구현은 **Producer만** 포함합니다.

- Producer: `WishService` -> `KafkaWishDomainEventPublisher`
- Consumer: 없음 (P4에서 분석/통계 컨슈머 분리 예정)

발행 이벤트 타입:
- `WISH_CREATED`
- `PRICE_CHANGED`
- `WISH_EXPIRED`
- `WISH_PURCHASED`
- `WISH_DELETED`
- `WISH_REACTIVATED`

## 3. 토픽/메시지 규약
- Topic: `wish.events.v1`
- Message Key: `wishId` (없으면 `eventId`)
- 발행 시점: 트랜잭션 `afterCommit`

이벤트 JSON 예시:
```json
{
  "eventId": "2ce3f2bc-84c0-40b3-8c91-9a2f9a6d99d4",
  "eventType": "PRICE_CHANGED",
  "schemaVersion": 1,
  "occurredAt": "2026-05-25T15:00:00",
  "wishId": 10,
  "userId": 1,
  "status": "WAITING",
  "previousReferencePrice": 5610,
  "currentReferencePrice": 4610
}
```

`schemaVersion` 의미:
- 이벤트 포맷 버전 번호입니다.
- 현재는 `1`이고, 필드 구조가 바뀔 때 `2`, `3`으로 올려 하위 호환을 관리합니다.

## 4. 환경변수
`save-pocket/.env` 기준:
```env
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
WISH_EVENTS_KAFKA_ENABLED=false
WISH_EVENTS_KAFKA_TOPIC=wish.events.v1
```

- `WISH_EVENTS_KAFKA_ENABLED=false`면 No-op Publisher가 동작합니다.
- Kafka 미구성 환경에서는 반드시 `false` 유지가 안전합니다.

## 5. 로컬 검증 방법
1) 브로커 준비 (예: Redpanda/Kafka)
2) 애플리케이션 실행 전 환경변수 적용
```bash
set -a
source .env
set +a
```
3) 값 확인
```bash
echo "$WISH_EVENTS_KAFKA_ENABLED" "$KAFKA_BOOTSTRAP_SERVERS" "$WISH_EVENTS_KAFKA_TOPIC"
```
4) 서버 실행
```bash
./gradlew bootRun
```
5) 위시 생성/삭제/구매/재활성화 또는 가격 갱신 실행
6) 소비 확인 (Redpanda 예시)
```bash
docker exec -it redpanda rpk topic consume wish.events.v1 -o start -n 20
```

주의:
- `-o beginning`은 동작하지 않을 수 있습니다. `-o start` 사용을 권장합니다.
- 출력이 없으면 이벤트가 아직 발행되지 않았거나, `WISH_EVENTS_KAFKA_ENABLED`가 `false`일 수 있습니다.

## 6. 장애/운영 포인트
- DB는 성공했는데 Kafka 전송이 실패하면 이벤트 누락 가능성이 있습니다(이중 쓰기 위험).
- 현재는 단순성과 구현 속도를 위해 `afterCommit` 방식으로 운영합니다.
- 신뢰성 강화를 원하면 다음 단계에서 Outbox 패턴(P3.5)을 적용합니다.

## 7. 보안 주의사항
- payload에는 비밀번호/토큰/민감 개인정보를 넣지 않습니다.
- 운영 브로커 접근은 사설 네트워크 또는 최소 허용 IP 정책을 사용합니다.
- 토픽 권한은 Producer/Consumer 역할별 최소 권한으로 분리합니다.
