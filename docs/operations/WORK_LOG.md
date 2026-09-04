# 작업 이력

## 2026-08-19 - App Review 접근·Review Notes Runbook

- 변경 파일: `app-review-access.md`, `WORK_LOG.md`
- 내용: 전용 계정 준비, 4~128자 code hash 주입, 제출용 영문 Review Notes, 짧은 코드의 심사 직후 비활성화·세션 폐기 절차를 추가했다.
- 검증: 앱·Backend 구현, 환경변수와 SQL column 계약 대조
- 리스크: 실제 App Store Connect 입력·운영 배포·심사자 접속은 수동 수행 필요

## 2026-07-29 - Lifecycle 적용·탈퇴 장애 절차

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: S3 IAM 권한, Lifecycle 적용·조회 명령과 회원 탈퇴 503 장애 대응·수동 삭제 금지를 추가했다.
- 검증: Lifecycle JSON과 Backend 삭제 순서 대조
- 리스크: AWS CLI 미설치로 실제 명령 실행은 미검증이며 #71에서 추적

## 2026-07-29 - 독립 DB Worker 1개 운영 절차

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: SQS·DLQ 절차를 제거하고 API/Worker 설정 분리, DB backlog·lease 복구와 단일 replica 운영 절차를 추가했다.
- 검증: Compose 환경변수와 DB Worker 상태 전이 대조
- 리스크: 운영 자원 제한과 경보 임계값 미확정

## 2026-07-29 - SQS Worker 배포·장애 대응 절차

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: API dispatcher와 web 없는 Worker 독립 실행, replica 확장, Queue 장애·중복 전달·DLQ 점검 절차를 추가했다.
- 검증: 설정 예시·Compose와 Queue 상태 전이 대조
- 리스크: 운영 metric 임계값과 redrive 횟수 미확정

## 2026-07-29 - 평가 작업 보존·정리 운영 절차 추가

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: 정리 환경변수, 기본값, 대상 상태와 backlog 대응 절차를 추가했다.
- 검증: 설정 예시와 Cleanup 구현 대조
- 리스크: 운영 DB 부하에 따른 설정 조정 필요

## 2026-07-27 - 평가 Worker 운영 절차 추가

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: Worker 설정, S3 CORS·Lifecycle, 작업 적체와 실패 확인 절차를 추가했다.
- 검증: 설정 예시·Worker 상태 전이와 대조
- 리스크: Azure timeout과 알림 임계치는 운영 측정 후 확정 필요

## 2026-07-23 - 운영 Docker 런타임을 Java 21로 갱신

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: Backend 운영 이미지가 Java 21 JRE와 FFmpeg를 포함하도록 런타임 설명을 갱신했다.
- 검증: Docker 이미지 빌드 통과, 런타임 OpenJDK 21.0.11 확인
- 리스크: 배포 전 Java 21 이미지의 FFmpeg 설치와 애플리케이션 기동 확인 필요
