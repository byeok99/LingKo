# 작업 이력

## 2026-07-29 - Queue 없는 독립 Worker 요구 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: SQS·4 replica 완료 표현을 제거하고 API와 독립 DB Worker 한 개 운영을 현재 완료 기준으로 갱신했다.
- 검증: Compose·Worker 배포 조건·통합 테스트 대조
- 리스크: 다중 Worker 또는 Queue 요구는 #52 측정 후 재정의 필요

## 2026-07-29 - 평가 Worker 독립 확장 요구 구현 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: NFR-PERF-009와 NFR-SCALE-005에 SQS jobId 전달, DB 원본과 API/Worker 독립 replica 구현·검증 상태를 반영했다.
- 검증: Docker 구성과 4 Worker 통합 테스트 대조
- 리스크: 운영 안전 처리량은 #52에서 확정 필요

## 2026-07-29 - Idempotency 중복 요청 검증 완료

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: 동일 평가 요청 10개의 작업·쿼터 단일화 통합 테스트 통과를 MVP 체크리스트에 반영했다.
- 검증: H2 MySQL mode 통합 테스트와 전체 Backend 테스트 통과
- 리스크: 실제 MySQL 동시 부하는 미검증

## 2026-07-27 - 음성 저장·Worker 비기능 요구 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: 비공개 직접 업로드, 영속 작업·재시도, 임시 파일 정리와 운영 확장 요구를 추가했다.
- 검증: ADR·보안·운영 문서와 교차 확인
- 리스크: 수치 SLO는 부하 측정 후 확정 필요

## 2026-07-26 - 쿼터 DB 정합성 요구사항 구현 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: NFR-DB-005 원자 차감과 NFR-DB-006 사용자·날짜별 단일 행 요구사항을 #38 구현 및 동시성 검증 완료 상태로 갱신했다.
- 검증: 동시 예약 10개와 신규 행 동시 생성 반복 테스트 결과 대조
- 리스크: 운영 MySQL 부하와 lock wait은 별도 측정 필요

## 2026-07-24 - 평가 통합 요구사항 구현 상태 반영

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 인증 평가, 쿼터 확인·차감, 결과 저장, 실패 보상과 기록 내용 요구사항을 구현 완료로 갱신했다.
- 검증: 단위·Controller·Spring/JPA 통합 테스트와 대조
- 리스크: 당시 남은 쿼터 동시성은 2026-07-26 보완, 요청 멱등성 #39는 미구현

## 2026-07-24 - 전체 기기 로그아웃 Issue 연결

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-AUTH-007의 후속 작업을 새 GitHub Issue #61에 연결했다.
- 검증: 생성된 GitHub Issue 제목·완료 기준과 요구사항 대조, `git diff --check`
- 리스크: 전체 기기 로그아웃 API와 UI는 미구현

## 2026-07-24 - 전체 기기 로그아웃 후속 범위 분리

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 현재 기기 세션 갱신·폐기를 완료한 Issue #40과 미구현인 전체 기기 로그아웃 요구사항을 분리하고 후속 Issue가 필요함을 명시했다.
- 검증: AuthService 로그아웃 범위와 FR-AUTH-005·007 대조, `git diff --check`
- 리스크: 전체 기기 로그아웃 API와 UI는 별도 Issue에서 구현 필요

## 2026-07-23 - 인증 세션 요구사항 구현 상태 갱신

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 자동 갱신, 회전, 재사용 탐지와 현재 기기 로그아웃 요구사항을 구현 상태로 갱신했다.
- 검증: Backend·Flutter 테스트 항목과 요구사항 대조
- 리스크: 전체 기기 로그아웃 요구사항은 미구현
