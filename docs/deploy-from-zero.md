# 무료 배포 + 도메인 + CI/CD (처음부터)

이 문서는 현재 레포 구조(`my-webapp` + `save-pocket`) 기준으로,
무료에 가깝고 복잡도를 낮춘 배포 절차를 정리합니다.

- 프론트: Vercel (Hobby)
- 백엔드/API: Railway (Trial/Hobby)
- DB/Redis: Railway 템플릿(MySQL, Redis)
- CI: GitHub Actions (선택)

## 0. 준비물

- GitHub 저장소
- Vercel 계정
- Railway 계정
- (선택) 커스텀 도메인 1개

## 1. GitHub Actions CI 설정(선택)

현재 레포에는 CI 워크플로우를 필수로 포함하지 않습니다.
원하면 아래 예시로 `.github/workflows/ci.yml`을 추가해 사용합니다.

- PR/Push(main) 시 실행
  - Frontend: `npm ci` -> `npm run lint` -> `npm run build`
  - Backend: `./gradlew test`

### 확인 방법

1. GitHub 저장소에서 PR 생성
2. `Actions` 탭에서 `CI` 워크플로우 통과 확인

## 2. 백엔드(Railway) 배포

1. Railway에서 `New Project` -> `Deploy from GitHub repo`
2. 현재 저장소 선택
3. 서비스 루트 디렉터리를 `save-pocket`으로 지정
4. 자동 배포 브랜치는 `main`으로 설정
5. Java 버전 고정 파일 확인: `save-pocket/.java-version` = `17`

### 2-1. Railway에 MySQL/Redis 추가

1. 같은 프로젝트에서 `Add Service` -> `Database` -> `MySQL`
2. 다시 `Add Service` -> `Database` -> `Redis`
3. 생성 후 각 서비스의 접속 정보(호스트/포트/유저/비밀번호) 확인

### 2-1-1. Kafka 사용 시 추가 준비(P3 선택)

Kafka 이벤트 발행을 운영에서 켜려면 Kafka 브로커가 필요합니다.

- Kafka 미구성: `WISH_EVENTS_KAFKA_ENABLED=false` 유지
- Kafka 구성: 관리형 Kafka(Confluent/Upstash 등) 또는 별도 브로커 서비스 준비

### 2-2. 백엔드 환경변수 설정

`save-pocket/src/main/resources/application.properties` 기준으로 아래 변수를 Railway API 서비스에 설정합니다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `NAVER_CLIENT_ID`
- `NAVER_CLIENT_SECRET`
- `NAVER_SHOPPING_API_PATH` (기본 `/v1/search/shop.json`)
- `WISH_EVENTS_KAFKA_ENABLED` (기본 `false`)
- `WISH_EVENTS_KAFKA_TOPIC` (기본 `wish.events.v1`)
- `WISH_EVENTS_KAFKA_CONSUMER_ENABLED` (기본 `false`)
- `WISH_EVENTS_KAFKA_CONSUMER_GROUP_ID` (기본 `save-pocket-analytics-v1`)
- `KAFKA_BOOTSTRAP_SERVERS` (`host:port` 형태)

중요:
- 값에 따옴표(`"`)를 넣지 않습니다.
- `<MYSQLHOST>`, `<MYSQLPORT>` 같은 플레이스홀더 문자열을 그대로 넣지 않습니다.
- Railway Reference 변수 사용 시 서비스명이 정확히 일치해야 합니다.

예시(`DB_URL`):
- `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul`

설정 후 재배포합니다.

Kafka를 아직 도입하지 않았다면:
- `WISH_EVENTS_KAFKA_ENABLED=false`로 유지합니다.

## 3. 프론트(Vercel) 배포

1. Vercel에서 `Add New Project` -> GitHub 저장소 선택
2. Root Directory를 `my-webapp`으로 지정
3. Environment Variable 추가
   - `BACKEND_ORIGIN=https://save-pocket.up.railway.app`
4. 배포 실행

현재 기본 운영 주소:
- 프론트: `https://save-pocket.vercel.app`
- 백엔드: `https://save-pocket.up.railway.app`

## 4. 도메인 연결

### 4-1. 기본 도메인 사용(가장 간단)
- Vercel 기본 도메인: `save-pocket.vercel.app`
- Railway 기본 도메인: `save-pocket.up.railway.app`

### 4-2. 커스텀 도메인 사용(선택)
권장 구성:
- `savepocket.app` -> 프론트(Vercel)
- `www.savepocket.app` -> 프론트(Vercel)
- `api.savepocket.app` -> 백엔드(Railway)

#### Vercel 도메인 연결

1. Vercel 프로젝트 -> `Settings` -> `Domains`
2. `savepocket.app`, `www.savepocket.app` 추가
3. Vercel이 안내하는 DNS 레코드를 도메인 업체 DNS에 등록

#### Railway API 도메인 연결

1. Railway API 서비스 -> `Settings` -> `Networking` -> `Custom Domain`
2. `api.savepocket.app` 추가
3. Railway가 안내하는 CNAME/TXT 레코드를 DNS에 등록

## 5. CD 자동화 설정(간단)

### Vercel

- Git 연동 상태면 `main` 머지 시 자동 배포됩니다.

### Railway

1. 서비스 `Settings` -> GitHub Autodeploy `Enable`
2. `Wait for CI`는 CI를 실제로 운영할 때만 활성화
3. 자동 배포가 안 돌면 `Source`에서 브랜치 `main` 재연결(Disconnect -> Connect)

## 6. 최종 점검 체크리스트

1. `https://save-pocket.vercel.app` 접속 확인
2. 로그인/회원가입 후 보호 페이지(`/wishes`) 이동 확인
3. 프론트에서 API 호출 시 `/sp/*` 응답 확인
4. Railway 로그에서 백엔드 에러 없는지 확인
5. DNS 전파가 끝날 때까지(최대 수시간~48시간) 재확인

## 7. 트러블슈팅

### 7-1. 빌드 실패: Railpack이 언어를 못 찾음
- 원인: Root Directory 미설정
- 조치: Railway `Settings -> Source -> Root Directory`를 `/save-pocket`으로 설정

### 7-2. 배포 후 즉시 CRASHED + DB 연결 실패
- 원인: `DB_URL` 값 오입력(플레이스홀더/따옴표)
- 조치: 실제 Railway Reference 변수로 다시 입력

### 7-3. 로그인 직후 기능 실행 시 세션 만료
- 원인: 로그아웃 엔드포인트가 GET으로 노출돼 프리패치에 의해 의도치 않게 호출될 수 있음
- 조치: 프론트 로그아웃을 POST 기반으로 처리하고 GET `/logout`는 실제 로그아웃 동작을 하지 않게 구성
## 8. 운영 팁

- 첫 주는 Railway/Vercel 에러 로그를 매일 확인
- 배포 장애 대비를 위해 main 병합 전 CI 필수 통과 정책(Branch protection) 적용
