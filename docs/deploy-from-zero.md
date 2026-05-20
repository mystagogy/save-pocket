# 무료 배포 + 도메인 + CI/CD (처음부터)

이 문서는 현재 레포 구조(`my-webapp` + `save-pocket`) 기준으로,
무료에 가깝고 복잡도를 낮춘 배포 절차를 정리합니다.

- 프론트: Vercel (Hobby)
- 백엔드/API: Railway (Free)
- DB/Redis: Railway 템플릿(MySQL, Redis)
- CI: GitHub Actions

## 0. 준비물

- GitHub 저장소
- Vercel 계정
- Railway 계정
- 도메인 1개

## 1. GitHub Actions CI 확인

이 레포에는 아래 CI 워크플로우가 포함되어 있습니다.

- 파일: `.github/workflows/ci.yml`
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

### 2-1. Railway에 MySQL/Redis 추가

1. 같은 프로젝트에서 `Add Service` -> `Database` -> `MySQL`
2. 다시 `Add Service` -> `Database` -> `Redis`
3. 생성 후 각 서비스의 접속 정보(호스트/포트/유저/비밀번호) 확인

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

설정 후 `Deploy Latest Commit` 또는 재배포합니다.

## 3. 프론트(Vercel) 배포

1. Vercel에서 `Add New Project` -> GitHub 저장소 선택
2. Root Directory를 `my-webapp`으로 지정
3. Environment Variable 추가
   - `BACKEND_ORIGIN=https://api.mystagogy.savepocket.app` (도메인 연결 후)
4. 배포 실행

## 4. 도메인 연결

권장 주소 구성:

- `savepocket.app` -> 프론트(Vercel)
- `www.savepocket.app` -> 프론트(Vercel)
- `mystagogy.savepocket.app` -> 프론트(Vercel)
- `api.mystagogy.savepocket.app` -> 백엔드(Railway)

### 4-1. Vercel 도메인 연결

1. Vercel 프로젝트 -> `Settings` -> `Domains`
2. `savepocket.app`, `www.savepocket.app` 추가
3. Vercel이 안내하는 DNS 레코드를 도메인 업체 DNS에 등록

### 4-2. Railway API 도메인 연결

1. Railway API 서비스 -> `Settings` -> `Networking` -> `Custom Domain`
2. `api.mystagogy.savepocket.app` 추가
3. Railway가 안내하는 CNAME/TXT 레코드를 DNS에 등록

## 5. CD 자동화 설정(간단)

### Vercel

- Git 연동 상태면 `main` 머지 시 자동 배포됩니다.

### Railway

1. 서비스 `Settings` -> GitHub Autodeploy `Enable`
2. `Wait for CI` 활성화
   - CI가 성공해야 배포 진행

## 6. 최종 점검 체크리스트

1. `https://savepocket.app` 접속 확인
2. 로그인/회원가입 후 보호 페이지(`/wishes`) 이동 확인
3. 프론트에서 API 호출 시 `/sp/*` 응답 확인
4. Railway 로그에서 백엔드 에러 없는지 확인
5. DNS 전파가 끝날 때까지(최대 수시간~48시간) 재확인

## 7. 운영 팁

- 첫 주는 Railway/Vercel 에러 로그를 매일 확인
- 배포 장애 대비를 위해 main 병합 전 CI 필수 통과 정책(Branch protection) 적용
