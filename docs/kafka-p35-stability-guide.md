# Kafka P3.5 안정화 가이드 (Outbox + 재시도/모니터링)

## 1. 왜 P3.5가 필요한가
현재 P3는 `DB 저장`과 `Kafka 발행`이 분리된 구조입니다.

- 장점: 구현이 단순하고 빠르게 도입 가능
- 한계: DB는 성공했는데 Kafka 발행만 실패하면 이벤트 누락 가능

이 한계를 줄이기 위해 P3.5에서 안정화 전략을 추가합니다.

## 2. Outbox 패턴 초안
핵심 아이디어:
1. 도메인 변경 트랜잭션 안에서 `outbox_event` 테이블에 이벤트를 함께 저장
2. 별도 퍼블리셔(스케줄러/워커)가 outbox를 읽어 Kafka로 발행
3. 발행 성공 시 outbox 상태를 `PUBLISHED`로 갱신

권장 outbox 스키마(초안):
- `id` (PK)
- `aggregate_type` (`WISH`)
- `aggregate_id` (`wishId`)
- `event_type`
- `schema_version`
- `payload_json`
- `status` (`PENDING`/`PUBLISHED`/`FAILED`)
- `retry_count`
- `next_retry_at`
- `created_at`, `published_at`

## 3. 최소 재시도 전략
- 실패 시 `retry_count + 1`
- 백오프: `1m -> 5m -> 15m -> 1h`
- 최대 재시도 초과 시 `FAILED`로 고정하고 알림

운영 단순화를 위해 초기에는:
- 최대 재시도 10회
- 초과 건은 운영자 수동 재처리 큐(또는 배치)로 분리

## 4. 모니터링 지표(최소)
- Outbox `PENDING` 건수
- Outbox `FAILED` 건수
- 최근 10분 발행 성공/실패 건수
- Consumer 지연(최신 offset lag)

장애 알림 기준 예시:
- `FAILED` 1건 이상 즉시 알림
- `PENDING`이 5분 이상 지속 증가 시 경고

## 5. 장애 대응 런북(요약)
1. Kafka 브로커 상태 확인
2. Outbox 상태 분포(`PENDING/FAILED`) 확인
3. `FAILED` 원인(직렬화/권한/연결) 확인
4. 원인 조치 후 `FAILED -> PENDING` 재처리
5. 재처리 후 체크포인트/lag 정상화 확인

## 6. 현재 구현과 연결
- 현재 P3는 `afterCommit` Producer 방식
- P4는 Consumer 집계(일/월 테이블) 추가
- P3.5는 Producer 측 안정화 레이어를 도입하는 단계
