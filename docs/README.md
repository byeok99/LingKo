# LingKo 문서 인덱스

이 디렉터리는 제품, 아키텍처, API, 데이터, 개발, 운영, 보안 문서의 기준 위치입니다.

## 빠른 탐색

- [제품·범위·용어](overview/product-and-scope.md)
- [시스템 아키텍처](architecture/system-architecture.md)
- [인증 흐름](architecture/authentication-flow.md)
- [발음 평가 흐름](architecture/evaluation-flow.md)
- [API 레퍼런스](api/api-reference.md)
- [오류 코드](api/error-codes.md)
- [데이터 모델](data/data-model.md)
- [마이그레이션 정책](data/migration-policy.md)
- [로컬 개발](development/local-development.md)
- [테스트·트러블슈팅](development/testing-and-troubleshooting.md)
- [운영 Runbook](operations/operations-runbook.md)
- [보안·개인정보](security/security-and-privacy.md)
- [기술 부채](technical-debt.md)
- [ADR 목록](architecture/adr/README.md)

## 문서 책임

문서는 코드 변경과 같은 PR에서 갱신합니다.

| 변경 유형 | 함께 수정할 문서 |
|---|---|
| API 요청·응답·오류 변경 | `api/` |
| DB 테이블·컬럼·제약 변경 | `data/` |
| 외부 서비스·컴포넌트 변경 | `architecture/` |
| 환경변수·실행 명령 변경 | `development/` |
| 장애·배포·복구 절차 변경 | `operations/` |
| 인증·토큰·개인정보 변경 | `security/` |

## 상태 표기

문서에서 다음 표현을 사용합니다.

- **구현됨**: 현재 코드에 존재하고 기본 테스트가 있음
- **부분 구현**: 일부 흐름만 연결됨
- **계획**: 코드에 아직 없음
- **운영 전 필수**: 출시 전에 반드시 해결해야 함
