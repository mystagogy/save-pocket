# AGENTS.md

## 1) 프로젝트 목표/범위
- 프로젝트: `save-pocket` (작심삼일 긴축재정)
- 목표: 충동구매 보류/만료/재활성화 흐름을 기록하고 절약 금액을 집계한다.
- MVP 범위:
- 인증(회원가입/로그인/로그아웃)
- 위시 등록/조회/구매/삭제
- 72시간 관심 기반 만료
- 가격 변경 이력 저장
- 월별 절약 리포트

## 2) 디렉터리 규칙
- 루트 앱 디렉터리: `save-pocket`
- 주요 경로:
- `src/main/java`: 애플리케이션 코드
- `src/main/resources`: 설정/템플릿/정적 리소스
- `docs`: 기획/DB/API 설계 문서
- 패키지 기본 경로: `io.github.mystagogy.savepocket`
- 권장 패키지 분리:
- `domain`: 엔티티/enum
- `repository`: JPA Repository
- `service`: 비즈니스 로직
- `controller`: API/Web 엔드포인트
- `dto`: 요청/응답 모델
- `common`: 공통 예외/응답/유틸

## 3) 코딩 규칙
- Java 17, Spring Boot 3.x 기준
- 클래스명: PascalCase, 메서드/필드: camelCase
- Entity와 DTO는 분리한다.
- Controller는 검증/입출력, 핵심 로직은 Service에 둔다.
- 예외는 도메인 단위 커스텀 예외 + 전역 핸들러(`@ControllerAdvice`)로 처리한다.
- 시간은 `Asia/Seoul` 기준으로 처리하고, 비즈니스 시간 필드는 `LocalDateTime`을 사용한다.
- 인증/인가, 비밀번호, 세션, 사용자 권한과 관련된 코드는 보안 민감 영역으로 분류하고 리뷰 우선순위를 최고로 둔다.

## 4) DB 원칙
- DBMS: MySQL (로컬 포트 `3308`)
- 로컬 기본 접속 정보:
- DB/USER/PASSWORD는 코드에 하드코딩하지 않고 환경변수(`.env`)로만 관리한다.
- 테이블명/컬럼명은 `snake_case`를 사용한다.
- PK는 `BIGINT` 자동 증가를 기본으로 한다.
- 상태/이벤트 값은 enum으로 고정하고 DB에는 문자열로 저장한다.
- 인덱스는 조회 패턴 기준으로 최소부터 시작하고, 측정 후 확장한다.
- 스키마 변경은 점진적으로 수행한다:
- 1차는 JPA 엔티티 + `ddl-auto=update`로 빠르게 검증
- 안정화 후 Flyway/Liquibase 마이그레이션으로 고정

## 5) 작업 절차
- 기능 단위로 작은 변경을 만든다.
- 순서:
- 엔티티/리포지토리
- 서비스/비즈니스 규칙
- API/DTO
- 테스트
- 문서 반영
- 커밋 메시지는 `type: subject` 형식 사용:
- 예) `feat: add product_wish entity`
- PR 전 체크:
- 애플리케이션 기동 확인
- 주요 API 수동 점검
- 서비스 테스트 + 통합 테스트가 모두 존재하는지 확인
- 스키마/인덱스 변경 시 `docs/db-api-design.md` 동기화

## 6) 보안 기준
- 비밀값(DB 비밀번호, API 키, 토큰, 시크릿)은 코드/문서에 하드코딩하지 않는다.
- 비밀값은 `.env` 또는 배포 환경변수로만 주입하고, `.env`는 Git 추적에서 제외한다.
- 로그에 비밀번호/토큰/세션ID/개인정보를 남기지 않는다.
- 인증 실패/권한 오류/입력 검증 실패 케이스를 반드시 테스트한다.

## 7) 테스트 기준
- 테스트는 최소 2계층으로 작성한다:
- 서비스 테스트: 비즈니스 규칙/상태 전이/예외 케이스 검증
- 통합 테스트: DB 연동 + API 흐름 + 보안 설정(인증/인가) 검증
- 신규 기능/수정 기능은 서비스 테스트와 통합 테스트를 모두 추가해야 한다.
- 보안 민감 변경(인증/인가/비밀번호/세션)은 정상/실패/권한없음 케이스를 모두 포함한다.

## 8) 참고 문서
- 기획: `docs/project-plan.md`
- DB/API 설계: `docs/db-api-design.md`

## 9) AI 작업 워크플로우 (Mandatory)

AI must follow this workflow strictly for all feature implementations.

### Step 1. Requirement Analysis
- Analyze the request before coding.
- Identify affected domain/service/API/test areas.

### Step 2. Implementation Plan
Before implementation, explain:
- implementation strategy
- files to modify
- database/schema impact
- API changes
- validation/security considerations
- test strategy

### Step 3. Approval Required
- NEVER start implementation before explicit user approval.
- Do not generate code immediately after planning.

### Step 4. Implementation
- Implement only approved scope.
- Keep commits logically small and isolated.

### Step 5. Implementation Review
After implementation, explain:
- what changed
- why it changed
- important design decisions
- possible side effects

### Step 6. Confirmation Required
- Wait for user confirmation before writing tests or proceeding.

### Step 7. Testing
- Add/update:
    - service tests
    - integration tests
- Explain covered scenarios.

### Step 8. Local Verification
- Ask user to run local verification before commit/push.

### Step 9. Git Workflow
- Generate commit message after verification.
- NEVER commit automatically without approval.
- NEVER push automatically without approval.

### Step 10. Pull Request
- PR must follow `.github/pull_request_template.md`
