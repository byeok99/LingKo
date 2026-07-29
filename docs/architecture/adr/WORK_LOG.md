# 작업 이력

## 2026-07-29 - 독립 DB Worker 결정으로 SQS ADR 대체

- 변경 파일: `0007-s3-direct-upload-and-db-evaluation-worker.md`, `0008-sqs-independent-evaluation-workers.md`, `0009-independent-db-evaluation-worker.md`, `README.md`, `WORK_LOG.md`
- 내용: ADR-0008을 폐기하고 Queue 없이 DB polling Worker 한 개를 API와 분리 운영하는 ADR-0009를 승인했으며 PR #70과 실제 MySQL 복구 검증 Issue #69를 연결했다.
- 검증: 구현·Compose·운영 문서와 결정 대조
- 리스크: Issue #69의 강제 종료·lease 복구와 운영 측정 후 Worker 확장 또는 Queue 선택 재결정 필요

## 2026-07-29 - SQS 독립 Worker ADR 추가

- 변경 파일: `0007-s3-direct-upload-and-db-evaluation-worker.md`, `0008-sqs-independent-evaluation-workers.md`, `README.md`, `WORK_LOG.md`
- 내용: DB를 작업 원본으로 유지하고 SQS에는 jobId만 전달하며 API·Worker를 독립 확장하는 결정과 PR #68을 기록했다.
- 검증: 구현·테스트·운영 문서와 결정 대조
- 리스크: 실제 AWS SQS 처리량과 DLQ 운영은 미검증

## 2026-07-27 - S3 직접 업로드·DB Worker ADR 추가

- 변경 파일: `0007-s3-direct-upload-and-db-evaluation-worker.md`, `README.md`, `WORK_LOG.md`
- 내용: Queue 없이 MVP를 보호하는 영속 작업 Worker 선택과 대안·확장 조건을 기록했다.
- 검증: 구현·운영 문서와 의사결정 대조
- 리스크: 트래픽 증가 전 Queue 전환 지표 수집 필요

## 2026-07-26 - 일일 쿼터 동시성 결정 기록

- 변경 파일: `0006-atomic-practice-quota-transitions.md`, `README.md`, `WORK_LOG.md`
- 내용: 일반 상태 전이는 조건부 원자 UPDATE, 최초 생성만 짧은 사용자 부모 lock으로 처리하고 외부 호출 중 lock을 유지하지 않는 결정을 승인했다.
- 검증: 구현, H2·MySQL 동시성 테스트와 ADR의 선택·대안·결과를 대조
- 리스크: 비정상 종료 예약 회수와 운영 lock 지표는 후속 작업

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
