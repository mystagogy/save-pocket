# RabbitMQ + SSE 알림 가이드 (1차)

## 1. 목적
- 12시간 가격 갱신 배치에서 `역대 최저가 갱신`이 발생하면 사용자에게 앱 내 실시간 알림을 전달한다.
- 배치 트랜잭션과 알림 전송을 분리해 실패 전파를 줄이고, 이후 확장(Kafka/다중 채널)을 대비한다.

## 2. 아키텍처
1. `WishService.refreshLowestReferencePrices`가 가격 변동을 감지한다.
2. 역대 최저가 갱신 조건을 만족하면 트랜잭션 커밋 후 RabbitMQ 메시지를 발행한다.
3. `NotificationMessageConsumer`가 메시지를 소비해 `notification` 테이블에 저장한다.
4. 저장 직후 `NotificationSseService`가 사용자별 SSE 채널로 실시간 이벤트를 푸시한다.
5. 프론트엔드는 `/sp/notifications/stream`을 구독하여 배지/목록을 즉시 갱신한다.

## 3. 큐 토폴로지
- Exchange: `wish.notification.exchange` (direct)
- Queue: `wish.notification.queue`
- DLQ: `wish.notification.dlq`
- Routing key: `wish.notification.price-drop`

큐 속성:
- 영속 큐(durable)
- 메인 큐 실패 메시지는 DLQ로 이동
- 리스너 `default-requeue-rejected=false`로 무한 재큐잉 방지

## 4. 도메인 규칙
- 알림 트리거: `latestReferencePrice < previousReferencePrice` 이면서 `latestReferencePrice < historicalLowestBeforeUpdate`
- 중복 억제: 동일 사용자/위시/알림유형 `24시간` 쿨다운
- 알림 유형: `PRICE_DROP_LOWEST`

## 5. 로컬 실행
1) 환경변수 준비
- `save-pocket/.env`에 RabbitMQ 값 추가:
  - `RABBITMQ_HOST=localhost`
  - `RABBITMQ_PORT=5672`
  - `RABBITMQ_USERNAME=...`
  - `RABBITMQ_PASSWORD=...`
  - `RABBITMQ_VHOST=/`

2) 인프라 기동
```bash
cd save-pocket
docker compose up -d
```

3) 백엔드/프론트 실행
```bash
cd save-pocket
./gradlew bootRun

cd ../my-webapp
npm run dev
```

4) 확인 포인트
- RabbitMQ 관리 콘솔: `http://localhost:15672`
- 로그인 후 홈/위시 화면 헤더에서 알림 배지 확인
- 가격 하락 이벤트 발생 시 드롭다운 목록이 실시간 갱신되는지 확인

## 6. 보안 주의사항
- 큐 메시지에는 `userId`, `wishId`, 가격값만 전달하고 개인정보/인증정보를 포함하지 않는다.
- SSE 엔드포인트(`/notifications/stream`)는 세션 인증이 필요한 API로 유지한다.
- 운영 환경에서는 RabbitMQ 관리 포트를 사설망/VPN 내부로 제한한다.
- RabbitMQ 계정은 최소 권한 원칙으로 분리한다.

## 7. 장애 대응
- RabbitMQ 장애 시 배치의 핵심 DB 갱신은 유지되고, 알림만 지연될 수 있다.
- 소비자 예외 메시지는 DLQ(`wish.notification.dlq`)에 쌓여 원인 분석/재처리가 가능하다.
- SSE 연결은 프론트에서 지수 백오프로 재연결한다.
