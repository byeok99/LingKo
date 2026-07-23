# 작업 이력

## 2026-07-23 - JWT Refresh Session 결정 보완

- 변경 파일: `0002-google-oauth-and-jwt.md`, `WORK_LOG.md`
- 내용: Refresh Token 회전·재사용 탐지·DB 해시 저장과 Access Token의 활성 세션 검증 결정을 추가했다.
- 검증: 인증 흐름 및 보안 문서와 결정 내용 대조
- 리스크: 서명 키 `kid` 기반 무중단 회전은 후속 작업

## 2026-07-21 - 통합·릴리스 브랜치 전략 확정

- 변경 파일: `0005-branch-strategy.md`, `README.md`, `WORK_LOG.md`
- 내용: 실제 GitHub 기본 브랜치와 최근 작업 흐름에 맞춰 `develop`을 통합 브랜치, `main`을 릴리스 브랜치로 사용하는 전략을 승인했다.
- 검증: 문서 링크와 브랜치 역할 표현을 확인하고 `git diff --check` 실행
- 리스크: GitHub 보호 규칙과 CI/CD 설정은 후속 작업으로 적용해야 함
