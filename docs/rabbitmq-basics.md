# RabbitMQ 기술 설명서 (프로젝트 기준)

## 1. RabbitMQ가 뭐야?
- RabbitMQ는 "메시지 큐(Message Queue)" 서버다.
- 쉽게 말해, 서비스 A가 바로 서비스 B를 호출하지 않고, "할 일 쪽지(메시지)"를 큐에 넣어두면 서비스 B가 나중에 꺼내 처리하게 해준다.

비유:
- 주문서 접수함(큐)에 종이를 넣어두고
- 담당자가 순서대로 꺼내 처리하는 구조

## 2. 왜 이 프로젝트에 필요해?
이 프로젝트에서는 `가격 갱신 작업`과 `알림 발송`을 분리하려고 RabbitMQ를 쓴다.

분리 전:
- 가격 갱신 중 알림 처리까지 한 번에 하다가 알림 쪽 실패가 전체 흐름에 영향을 줄 수 있음

분리 후:
- 가격 갱신은 DB에 먼저 안전하게 반영
- 알림은 RabbitMQ 큐로 넘겨 비동기로 처리
- 알림 처리 실패 시 재시도/DLQ 분석이 가능

## 3. 우리 프로젝트에서 실제 동작 흐름
1. 배치가 위시 가격을 갱신한다.
2. `역대 최저가 갱신` 조건이면 메시지를 RabbitMQ에 발행한다.
3. Consumer가 메시지를 받아 `notification` 테이블에 저장한다.
4. 저장 후 SSE로 현재 로그인 사용자 화면에 실시간 푸시한다.

핵심:
- "가격 갱신 성공"과 "알림 전달 성공"을 강하게 묶지 않는다.
- 사용자 경험은 유지하면서 서버 안정성을 높인다.

## 4. 꼭 알아야 할 용어 4개
- Exchange: 메시지를 어디 큐로 보낼지 결정하는 분배기
- Queue: 메시지가 쌓이는 대기열
- Routing Key: 메시지 라우팅 규칙 키
- DLQ(Dead Letter Queue): 처리 실패 메시지를 모아두는 큐

현재 설정(알림 파이프라인):
- Exchange: `wish.notification.exchange` (direct)
- Queue: `wish.notification.queue`
- DLQ: `wish.notification.dlq`
- Routing Key: `wish.notification.price-drop`

현재 설정(가격 갱신 큐 파이프라인):
- Exchange: `wish.price-refresh.exchange` (direct)
- Queue: `wish.price-refresh.queue`
- DLQ: `wish.price-refresh.dlq`
- Routing Key: `wish.price-refresh.request`

## 5. RabbitMQ를 켜야 할 때 / 꺼도 될 때
- 켜기: 실제 알림 파이프라인을 검증하거나 운영 반영할 때
  - `NOTIFICATION_RABBITMQ_ENABLED=true`
- 끄기: 로컬에서 큐 없이 기본 기능만 확인할 때
  - `NOTIFICATION_RABBITMQ_ENABLED=false`

참고:
- 비활성화 시에도 앱은 동작한다(알림 큐 파이프라인만 비활성).
- Kafka는 별도 목적(도메인 이벤트 스트림 표준화)에 사용하며 RabbitMQ를 대체하지 않는다.

## 6. 보안 관점에서 최소 체크
- 메시지 payload에 비밀번호/토큰/PII 넣지 않기
- 운영에서 RabbitMQ 관리 포트(`15672`) 외부 공개 금지
- 기본 계정(`guest`) 대신 전용 계정 사용
- 실패 메시지는 DLQ로 보내고 원인 분석 후 재처리
