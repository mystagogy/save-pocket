# Redis 학습 가이드

이 문서는 "Redis를 처음 접하는 사람" 기준으로 작성했습니다.
코드보다 먼저 "왜 쓰는지"를 이해하고, 그다음 "어떻게 확인하는지"를 따라가면 됩니다.

## 1) Redis가 뭐예요?

Redis는 아주 빠른 **메모리 기반 저장소**입니다.
- 메모리(RAM)에 데이터를 저장해서 디스크(DB 파일)보다 빠릅니다.
- 형태는 기본적으로 `key -> value`입니다.
  - 예: `user:1:name -> "mystagogy"`

쉽게 비유하면:
- MySQL = 오래 보관하는 **창고** (정확하고 영속적)
- Redis = 자주 꺼내 쓰는 **책상 위 메모지** (빠르지만 임시 성격)

## 2) 왜 이 프로젝트에서 Redis를 써요?

이 프로젝트에서 Redis는 두 가지 역할을 합니다.

1. **로그인 세션 저장소**
- 사용자가 로그인하면 "이 사용자는 로그인된 상태"라는 정보를 Redis에 저장합니다.
- 서버가 재시작돼도(설정에 따라) 세션을 안정적으로 관리할 수 있습니다.

2. **월간 리포트 캐시 저장소**
- `/reports/monthly` 결과를 잠깐 저장해 두고, 같은 요청이 오면 DB를 다시 계산하지 않고 빠르게 응답합니다.
- DB 부하를 줄이고 응답 속도를 높일 수 있습니다.

## 3) 세션이 Redis에 저장되는 흐름

1. 사용자가 이메일/비밀번호로 로그인합니다.
2. 서버는 로그인 성공 시 세션 ID를 만들고 Redis에 저장합니다.
3. 브라우저는 쿠키로 세션 ID를 가지고 있습니다.
4. 이후 요청마다 쿠키를 보내면 서버가 Redis에서 세션을 확인해 인증 상태를 판단합니다.
5. 세션 만료 시간(`SESSION_TIMEOUT`)이 지나면 Redis에서 세션이 만료되어 로그아웃 상태가 됩니다.

핵심:
- "로그인 상태" 그 자체를 브라우저가 기억하는 게 아니라,
- 브라우저는 **세션 ID**만 들고 있고, 실제 로그인 상태 데이터는 Redis에 있습니다.

## 4) 월간 리포트 캐시 흐름

1. 사용자가 월간 리포트를 처음 조회합니다.
2. 서버가 MySQL에서 집계 계산 후 결과를 Redis 캐시에 저장합니다.
3. 같은 사용자/같은 월 요청이 다시 오면 Redis 값을 바로 반환합니다.
4. 대신 데이터가 바뀌는 이벤트(구매/삭제/재활성화/만료)가 발생하면 캐시를 지웁니다(무효화).

### 캐시 키 형식

이 프로젝트는 아래 형식을 사용합니다.
- `monthlySavings::<userId>:<year>:<month>`
- 예: `monthlySavings::1:2026:5`

## 5) TTL(만료시간)이 뭐예요?

TTL(Time To Live)은 "이 데이터가 Redis에 얼마나 살아있을지"입니다.
- 예: TTL 600초 = 10분 후 자동 삭제
- 이 프로젝트 기본 캐시 TTL: `REPORT_CACHE_TTL=PT10M` (10분)

즉, 캐시는 "영구 저장"이 아니라 "잠깐 저장"입니다.

## 6) 캐시 무효화는 왜 필요해요?

예를 들어 리포트를 캐시해둔 뒤 사용자가 위시를 구매하면,
기존 리포트 값은 오래된 값이 됩니다.

그래서 아래 이벤트에서 캐시를 삭제합니다.
- 구매(PURCHASED)
- 삭제(DELETED)
- 재활성화(REACTIVATED)
- 만료(EXPIRED)

그리고 중요한 구현 포인트:
- 캐시 삭제는 트랜잭션 **커밋 후(afterCommit)**에 실행합니다.
- 이유: DB 반영 전에 캐시를 지우면, 타이밍에 따라 오래된 값이 다시 캐시에 들어갈 수 있기 때문입니다.

## 7) 실습: Redis 키 직접 확인하기

아래 명령은 `save-pocket` 디렉터리에서 실행합니다.

### 7-1. Redis 컨테이너 상태 확인
```bash
docker compose ps
```

### 7-2. 월간 리포트 캐시 키 확인
```bash
# 비밀번호 설정한 경우
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" --scan --pattern 'monthlySavings*'

# 비밀번호 없는 경우
docker compose exec redis redis-cli --scan --pattern 'monthlySavings*'
```

### 7-3. TTL 확인
```bash
docker compose exec redis redis-cli TTL 'monthlySavings::1:2026:5'
```

### 7-4. 세션 키 확인
```bash
docker compose exec redis redis-cli --scan --pattern 'save-pocket:session*'
```

## 8) 최소 체크리스트 (개발 중)

1. `docker compose up -d` 후 mysql/redis 둘 다 healthy인지 확인
2. 로그인 후 `save-pocket:session*` 키가 생기는지 확인
3. `/reports/monthly` 조회 후 `monthlySavings*` 키가 생기는지 확인
4. 위시 상태 변경 후 해당 월 캐시 키가 사라졌다가 재조회 시 다시 생기는지 확인

---

처음에는 "세션"과 "캐시"가 둘 다 Redis를 써서 헷갈리기 쉽습니다.
이 프로젝트에서는 다음처럼 기억하면 가장 쉽습니다.
- 세션: "로그인 상태 저장"
- 캐시: "리포트 계산 결과 임시 저장"
