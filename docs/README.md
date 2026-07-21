# LingKo 문서 인덱스

이 디렉터리는 제품, 요구사항, 아키텍처, API, 데이터, 개발, 운영, 보안 문서의 기준 위치입니다.

## 빠른 탐색

### 제품과 요구사항

- [제품·범위·용어](overview/product-and-scope.md)
- [MVP 기능 정의서](requirements/functional-requirements.md)
- [MVP 비기능 정의서](requirements/non-functional-requirements.md)
- [출시 로드맵](roadmap/release-roadmap.md)
- [출시·성능 Issue Backlog](roadmap/issue-backlog.md)
- [성능·확장성 계획](performance/scalability-plan.md)

### 설계와 구현

- [시스템 아키텍처](architecture/system-architecture.md)
- [인증 흐름](architecture/authentication-flow.md)
- [발음 평가 흐름](architecture/evaluation-flow.md)
- [API 레퍼런스](api/api-reference.md)
- [오류 코드](api/error-codes.md)
- [데이터 모델](data/data-model.md)
- [마이그레이션 정책](data/migration-policy.md)
- [ADR 목록](architecture/adr/README.md)

### 개발과 운영

- [로컬 개발](development/local-development.md)
- [테스트·트러블슈팅](development/testing-and-troubleshooting.md)
- [운영 Runbook](operations/operations-runbook.md)
- [보안·개인정보](security/security-and-privacy.md)
- [기술 부채](technical-debt.md)

### 보관 문서

- [과거 로컬 기획 및 구현 참고 문서](archive/README.md)

## 문서 책임

문서는 코드 변경과 같은 PR에서 갱신합니다.

| 변경 유형 | 함께 수정할 문서 |
|---|---|
| 사용자 기능·비즈니스 규칙 변경 | `requirements/functional-requirements.md` |
| 성능·보안·운영 목표 변경 | `requirements/non-functional-requirements.md` |
| API 요청·응답·오류 변경 | `api/` |
| DB 테이블·컬럼·제약 변경 | `data/` |
| 외부 서비스·컴포넌트 변경 | `architecture/` |
| 환경변수·실행 명령 변경 | `development/` |
| 장애·배포·복구 절차 변경 | `operations/` |
| 인증·토큰·개인정보 변경 | `security/` |
| 출시 우선순위·일정 변경 | `roadmap/` |

## 상태 표기

문서에서 다음 표현을 사용합니다.

- **구현됨**: 현재 코드에 존재하고 기본 테스트가 있음
- **부분 구현**: 일부 흐름만 연결됨
- **계획**: 코드에 아직 없음
- **운영 전 필수**: 출시 전에 반드시 해결해야 함

## 요구사항 ID 규칙

- 기능 요구사항: `FR-영역-번호`
- 비기능 요구사항: `NFR-영역-번호`
- 비즈니스 규칙: `BR-번호`

PR과 Issue에는 관련 요구사항 ID를 기록해 기능, 코드, 테스트, 운영 문서를 연결합니다.
