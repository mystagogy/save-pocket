# Kafka 기술 설명서 (프로젝트 기준)

## 1. Kafka가 뭐야?
- Kafka는 데이터를 "이벤트 로그" 형태로 계속 쌓아두고, 필요한 서비스가 가져가서 처리하는 이벤트 스트리밍 플랫폼이다.
- 쉽게 말하면 "실시간 변경 이력 저장소 + 전달 시스템"이다.

비유:
- 은행 거래내역처럼 기록을 순서대로 남긴다.
- 나중에 다른 팀이 같은 거래내역을 다시 읽어도 된다.

## 2. 왜 필요해?
서비스가 커지면 한 기능의 결과를 여러 기능이 같이 쓰게 된다.

예:
- 위시 가격 변경
- 위시 만료
- 위시 삭제

이런 변경을 API 코드 안에서 전부 동기 처리하면:
- 코드가 무거워지고
- 실패 전파가 커지고
- 기능 추가할 때 기존 코드를 계속 건드리게 된다.

Kafka를 두면:
- 변경 사실을 이벤트로 한 번 발행
- 분석/통계/추천 같은 후속 기능은 각자 비동기로 소비
- 기능 간 결합도를 낮출 수 있다.

## 3. 핵심 용어 7개
- Broker: Kafka 서버 노드
- Topic: 이벤트가 쌓이는 논리적 통로(예: `wish.events.v1`)
- Partition: Topic을 나눈 저장 단위(순서 보장은 파티션 단위)
- Producer: 이벤트를 보내는 쪽
- Consumer: 이벤트를 읽는 쪽
- Consumer Group: 같은 일을 나눠 처리하는 소비자 그룹
- Offset: "어디까지 읽었는지" 위치 번호

## 4. 우리 프로젝트에 매핑하면
- Producer: `WishService`
- Topic: `wish.events.v1`
- 발행 이벤트:
  - `WISH_CREATED`
  - `PRICE_CHANGED`
  - `WISH_EXPIRED`
  - `WISH_PURCHASED`
  - `WISH_DELETED`
  - `WISH_REACTIVATED`
- 이벤트 버전: `schemaVersion=1`

즉, 위시 도메인에서 상태/가격이 바뀌면 이벤트를 발행하고, 이후 기능은 이 이벤트를 읽어서 확장한다.

## 5. 현재 구현 방식(중요)
- 현재는 Producer만 구현되어 있다(P3).
- 발행 시점은 트랜잭션 `afterCommit`이다.
  - DB 커밋 성공 후에만 Kafka로 보낸다.
- Kafka 비활성화 시에는 no-op Publisher가 동작한다.
  - `WISH_EVENTS_KAFKA_ENABLED=false`

## 6. 로컬에서 확인하는 법
환경변수:
```env
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
WISH_EVENTS_KAFKA_ENABLED=true
WISH_EVENTS_KAFKA_TOPIC=wish.events.v1
```

확인 순서:
1. Kafka(또는 Redpanda) 실행
2. 백엔드 실행 (`./gradlew bootRun`)
3. 위시 생성/삭제/구매/재활성화 또는 가격 갱신 수행
4. 토픽 consume으로 이벤트 확인

Redpanda 예시:
```bash
docker exec -it redpanda rpk topic consume wish.events.v1 -o start -n 20
```

## 7. 초보자가 자주 헷갈리는 포인트
- "Consumer가 없으면 Kafka가 의미 없나?"
  - 아니다. 먼저 이벤트를 표준화해두면 후속 기능을 안전하게 붙일 수 있다.
- "Kafka 켜면 무조건 빨라지나?"
  - 아니다. 목적은 속도보다 "분리, 확장성, 장애 격리"에 가깝다.
- "이벤트가 100% 안 사라지나?"
  - 기본 구현만으로는 누락 위험이 있다(이중 쓰기 문제). 안정성을 더 높이려면 Outbox 패턴을 추가한다.

## 8. 보안 체크리스트
- 이벤트 payload에 비밀번호/토큰/민감정보를 넣지 않는다.
- 토픽 권한은 Producer/Consumer 최소 권한으로 분리한다.
- 운영 브로커는 외부 공개를 최소화하고 접근 제어를 둔다.
