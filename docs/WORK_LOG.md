# 작업 이력

## 2026-08-19 - App Review 운영 문서 인덱스 연결

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 개발·운영 탐색에서 App Review 접근 Runbook을 바로 찾을 수 있게 연결했다.
- 검증: 상대 링크와 대상 파일 존재 확인
- 리스크: 없음

## 2026-08-12 - Apple 로그인 출시 체크리스트 갱신

- 변경 파일: `mvp-release-execution-checklist.md`, `WORK_LOG.md`
- 내용: 코드·entitlement 완료와 capability·실기기·authorization code/revocation 미완료를 분리했다.
- 검증: 앱·Backend 구현 및 테스트 결과와 대조
- 리스크: 미완료 항목 완료 전 iOS 출시 불가

## 2026-08-12 - 법무 문서 운영자 정보 확정과 기술 부채 등록

- 변경 파일: `technical-debt.md` (법무 문서 상세는 `legal/WORK_LOG.md`)
- 내용: 운영 주체가 개인으로 확정되고 EU 배포가 제외되면서 법무 문서 4종을 갱신했다. 문서가 약속하는 휴면 계정 전환·통지·삭제가 구현되어 있지 않아 P1 기술 부채로 등록했다. 게시 상태에서 동작하지 않으면 허위 고지가 되므로 문서 게시 시점과 맞춰야 한다.
- 검증: `./gradlew test --tests '*Legal*'` 통과
- 리스크: Azure Speech의 음성 학습 이용 여부 미확인

## 2026-08-12 - AdMob SSV 구현 상태 문서 동기화

- 변경 파일: `mvp-release-execution-checklist.md`, `technical-debt.md`, `WORK_LOG.md` 및 하위 관련 문서
- 내용: client callback 단계 설명을 1회성 session·Google ECDSA 검증·전역 transaction 멱등성으로 갱신했다.
- 검증: 구현·API·보안·데이터·요구사항 문서 대조
- 리스크: AdMob console 공개 HTTPS callback E2E 필요

## 2026-08-12 - 문서 전반 최신성 점검과 갱신

- 변경 파일: `mvp-release-execution-checklist.md`, `technical-debt.md` (하위 폴더 상세는 각 `WORK_LOG.md`)
- 내용: docs 전체를 코드와 대조해 뒤처진 서술을 갱신했다. 출시 체크리스트는 기준 브랜치가 `feat/design-a-tokens`, 1번이 미완, coverage 73.59%로 남아 있어 현재(`develop`, 1·2번 완료, 125개 통과·80.19%)로 고쳤고 3번을 부분 완료로 바꿨다. HANDOFF의 다음 작업 판단도 광고 구현 진척을 반영해 다시 썼다. 기술 부채에서는 완료된 평가 멱등성과 법무 문서 게시 경로를 완료 항목으로 옮기고, Actuator는 의존성만 있고 probe 설정이 없다는 실제 상태로 정정했다.
- 검증: `./gradlew test integrationTest` BUILD SUCCESSFUL, `flutter analyze` 무경고, `flutter test --coverage` 125개 통과(80.19%)
- 리스크: Spring Boot·Java·Dart·MySQL 버전 표기는 실제와 일치해 수정하지 않았다. 0번 출시 채널 결정은 여전히 미결이며 4·5번 범위가 이에 묶여 있다

## 2026-08-09 - Issue #41 완료 상태 동기화

- 변경 파일: `mvp-release-execution-checklist.md`, `technical-debt.md`, `WORK_LOG.md`
- 내용: 가이드 생성 HTTP API의 기본 비활성화·내부 service 인증·비용 제한 구현을 완료로 반영하고 영속화 #42를 후속 위험으로 분리했다.
- 검증: 구현·API·보안 문서 대조, `git diff --check`
- 리스크: 작업 영속화와 다중 instance 전역 admission은 #42에 남음

## 2026-08-08 - 보상형 광고 MVP 상태 문서 동기화

- 변경 파일: `technical-debt.md`, `mvp-release-execution-checklist.md`, `WORK_LOG.md`
- 내용: 테스트 AdMob reward flow 구현을 완료 상태로 반영하고 운영 출시 전 Google SSV 검증을 별도 필수 후속 작업으로 구분했다.
- 검증: 구현·API·보안·ADR 문서 간 상태 문구 대조, `git diff --check`
- 리스크: 운영 광고 ID와 SSV는 아직 미설정이다

## 2026-08-07 - 법무 문서에 일본 APPI 세트 추가

- 변경 파일: `legal/jp/` 신설 (상세는 `legal/jp/WORK_LOG.md`)
- 내용: 한국·EU에 이어 일본어 프라이버시 정책과 이용약관 2종을 추가해 3개 관할 세트를 갖췄다. 미국은 CCPA 적용 기준 미달로 생성하지 않았고 판단 근거를 `legal/README.md`에 남겼다.
- 검증: 하위 폴더 WORK_LOG 기록, 4개 관할 문서 간 상호링크 확인
- 리스크: 전부 초안이며 일본 변호사 검토 전이다

## 2026-08-07 - 법무 문서 폴더 신설

- 변경 파일: `legal/` 신설 (하위 상세는 `legal/WORK_LOG.md`, `legal/eu/WORK_LOG.md`)
- 내용: 한국(PIPA)·EU(GDPR) 병기 처리방침·이용약관 초안 4종과 미확정 항목 체크리스트를 `docs/legal/` 아래에 두었다. 문서 사실관계는 `security/security-and-privacy.md`를 근거로 삼았으므로 보안 정책이 바뀌면 두 세트를 함께 갱신한다.
- 검증: 하위 폴더 WORK_LOG 기록, 상대 링크 확인
- 리스크: 전부 초안이며 법률 검토 전이다. `docs/security/security-and-privacy.md`의 "운영 전에 처리방침과 약관에 반영해야 한다"는 항목은 초안 작성까지만 진행된 상태

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
