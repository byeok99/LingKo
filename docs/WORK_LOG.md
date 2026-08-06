## 2026-08-06 - LingKo Blue 디자인 기준 연결

- 변경 파일: `README.md`, `design-repair/README.md`, `design-repair/LingKo Blue Merged.dc.html`
- 내용: 새 design-repair 시안을 활성 디자인 기준으로 정리하고 문서 인덱스에서 바로 찾을 수 있게 연결했다.
- 검증: 상대 링크 확인, `git diff --check`
- 리스크: 실제 기기 시각 검수는 후속 확인 필요

## 2026-08-06 - MVP 출시 실행 체크리스트 추가

- 변경 파일: `mvp-release-execution-checklist.md`, `README.md`, `WORK_LOG.md`
- 내용: Direction A 구현과 현재 운영 준비 상태를 기준으로 출시 잔여 작업을 결정, 통합, 보안·정책, 실환경 E2E, 관측성, 배포, 복구, 초대 베타 순서의 단일 실행 체크리스트로 정리하고 문서 인덱스에 연결했다.
- 검증: 문서 내부 상대 링크와 체크리스트 순서 확인, `git diff --check`
- 리스크: 출시 플랫폼, 광고 범위, 가이드 생성 운영 방식, 목표 SLO·비용 상한은 0번 작업에서 결정 필요

## 2026-08-03 - Replicate 안정화 기술부채 범위 갱신

- 변경 파일: `technical-debt.md`, `WORK_LOG.md`
- 내용: 완료된 Replicate 제한 재시도·timeout 취소를 반영하고 남은 `Retry-After`·Jitter와 Azure·S3·Circuit Breaker를 #44 후속 범위로 명확히 했다.
- 검증: 구현·테스트·Issue #44 완료 조건과 대조
- 리스크: #44 전체 범위는 미완료이므로 Issue 유지 필요

## 2026-07-29 - 음성 보존 기술부채 완료

- 변경 파일: `technical-debt.md`, `WORK_LOG.md`
- 내용: #43 구현에 따라 음성 보존·Lifecycle·탈퇴 연계를 P0/P1 미완료에서 제거하고 확정 정책으로 기록했다.
- 검증: 코드·테스트·보안·운영 문서와 대조
- 리스크: 실제 AWS Lifecycle 적용과 표본 검증은 #71에서 추적

## 2026-07-26 - 일일 쿼터 동시성 기술부채 완료

- 변경 파일: `technical-debt.md`, `WORK_LOG.md`
- 내용: #38 구현에 따라 일일 쿼터 동시성 제어를 P1 미완료 목록에서 제거하고 ADR-0006의 원자 UPDATE·최초 생성 lock 결정을 완료 기록에 반영했다.
- 검증: 구현, 동시성 테스트, ADR과 기술부채 표현 대조
- 리스크: 비정상 종료 예약 회수 정책은 후속 작업

## 2026-07-24 - 트러블슈팅 문서 인덱스 연결

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 재발 방지용 트러블슈팅 노트를 일반 실행 오류 가이드와 구분하고 문서 빠른 탐색·책임 표에 새 경로를 연결했다.
- 검증: `docs/troubleshooting/README.md` 상대 링크와 문서 역할 구분 확인
- 리스크: 없음

## 2026-07-24 - 평가 통합 기술부채 정리

- 변경 파일: `technical-debt.md`, `WORK_LOG.md`
- 내용: 완료된 평가 인증·쿼터·영속화 연결을 P0 부채에서 제거하고 예약 후 확정·실패 복구 정책을 완료 결정으로 기록했다.
- 검증: 코드·통합 테스트·관련 문서 대조
- 리스크: #38 동시성, #39 멱등성, 비정상 종료 예약 회수 정책

## 2026-07-24 - Refresh Token 운영 검증 Issue 연결

- 변경 파일: `technical-debt.md`, `WORK_LOG.md`
- 내용: 남아 있던 실기기 만료 E2E와 동시 DB 부하 검증을 새 GitHub Issue #60·#62에 연결했다.
- 검증: 생성된 GitHub Issue 제목·완료 기준과 기술부채 문서 대조, `git diff --check`
- 리스크: 두 운영 검증 Issue는 미착수

## 2026-07-24 - Refresh Token 기술부채 완료 처리

- 변경 파일: `technical-debt.md`, `WORK_LOG.md`
- 내용: 구현 완료된 Refresh Token 정책을 P0 기술부채와 미결정 항목에서 제거하고 DB 해시 저장·회전·폐기·자동 갱신 결정을 완료 기록으로 옮겼다.
- 검증: Refresh Token 구현·ADR·테스트 이력과 문서 대조, `git diff --check`
- 리스크: 실제 만료 기반 실기기 E2E와 동시 DB 부하 테스트는 운영 전 후속 검증 필요

## 2026-07-21 - 브랜치 전략 문서 불일치 해소

- 변경 파일: `architecture/adr/0005-branch-strategy.md`, `architecture/adr/README.md`, `technical-debt.md`, `WORK_LOG.md`
- 내용: 실제 저장소 운영과 다르던 `main` 단일화 제안을 정리하고 `develop` 통합·`main` 릴리스 전략을 승인 상태로 확정했다. 기술 부채의 오래된 브랜치 상태와 미결정 항목도 제거했다.
- 검증: 브랜치명 참조 검색, 문서 링크 확인, `git diff --check`
- 리스크: GitHub 보호 규칙과 CI/CD는 ADR 후속 작업으로 남아 있음

## 2026-07-21 - 과거 로컬 문서 보존 위치 추가

- 변경 파일: `README.md`, `archive/README.md`, `archive/legacy/*`, `WORK_LOG.md`
- 내용: 최신 기준 문서와 구분하기 위해 과거에 추적 중단된 로컬 기획 및 앱 참고 문서를 `archive/legacy/`로 이동하고 문서 인덱스에 보관 위치를 추가했다.
- 검증: 문서 목록, 내부 상대 링크, `git diff --check` 확인
- 리스크: 보관 문서의 API 및 구현 설명은 현재 코드와 다를 수 있음

## 2026-07-20 - Phase 8.3 완료 상태 반영

- 변경 파일: `task-breakdown.md`, `WORK_LOG.md`
- 내용: Flutter quota UI 연결 작업 완료에 맞춰 Phase 8.3 상태를 `[x]`로 변경하고 실제 변경 파일, 검증 명령, 남은 리스크를 기록했다.
- 검증: 문서 변경으로 별도 빌드/테스트는 실행하지 않음. 앱 변경 검증은 `app/WORK_LOG.md`에 기록함.
- 리스크: 없음
## 2026-08-03 - 에너지 후속 작업 현황 동기화

- 변경 파일: `technical-debt.md`
- 내용: 광고 SDK·보상 지급 경계를 미구현 후속 작업으로 명시했다.
- 검증: 활성 문서 키워드 검색 및 diff 점검
- 리스크: 광고 공급자 선정 필요
