# 작업 이력

## 2026-08-06 - 가이드 영상 흰 화면 노트 작성

- 변경 파일: `2026-08-06-guide-video-yuv444-white-screen.md`(신규), `README.md`
- 내용: 해상도 홀짝과 재생 여부가 100% 일치하는 실측 7건, 기각한 가설 3건(시뮬레이터 texture 제약·S3 설정·정적 PNG 혼동), 근본 원인(yuv444p를 Apple VideoToolbox가 디코딩 못 함)을 기록했다. 이전에 "시뮬레이터 texture 한계"로 잘못 결론냈던 조사를 정정하는 기록이기도 하다.
- 검증: `ffprobe` 전수 확인과 재인코딩 결과 대조
- 리스크: 관련 Issue·PR이 `미할당`

## 2026-08-06 - Azure 단어 점수 폐기는 문제가 아님을 확인

- 변경 파일: 없음(노트 미작성)
- 내용: `evaluation_word` 행이 없는 평가가 많아 정렬 검증이 어절 점수를 대량 폐기하는 줄 알았으나, 분모를 잘못 잡은 측정이었다. 테이블은 V15(2026-08-04)에 생겼는데 그 이전 평가 17건까지 세고 있었다. 그 건들은 게이트가 버린 게 아니라 테이블이 없어서 행이 없었다. 마이그레이션 이후로만 세면 6건 중 6건이 통과라 문제가 존재하지 않아 노트를 만들지 않았다.
- 검증: `WHERE created_at > (마이그레이션 적용 시각)` 조건으로 재측정
- 리스크: 없음. 교훈은 "보존율·성공률을 잴 때 기능이 도입된 시점 이후로 분모를 잘라야 한다"이며, 폐기 사유를 남기는 진단 로그는 관측 수단으로 유지한다

## 2026-08-03 - Replicate timeout·429 연쇄 실패 사례 기록

- 변경 파일: `2026-08-03-replicate-prediction-timeout-rate-limit.md`, `README.md`, `WORK_LOG.md`
- 내용: 60초 cold start timeout, 미취소 작업과 후속 429의 증거·완화·검증·롤백을 Issue #44와 PR #77에 연결했다.
- 검증: Backend 단위·내부 통합과 Replicate 직접 호출·S3 cache 외부 테스트 결과 대조
- 리스크: `Retry-After`·Jitter, Azure·S3와 Circuit Breaker는 #44 후속 범위

## 2026-07-30 - 누락 예약으로 인한 최종 실패 고착 사례 추가

- 변경 파일: `2026-07-29-evaluation-job-status-lost-after-quota-update.md`, `README.md`, `WORK_LOG.md`
- 내용: 삭제된 S3 원본과 이미 사라진 쿼터 예약 때문에 4개 작업이 1,500회 이상 재실행된 증거와 비예외 복구 해결을 기존 #47/#68 노트에 추가했다.
- 검증: Docker Worker 로그·MySQL 상태, Backend 단위 201개·통합 11개와 배포 후 기존 4개 작업의 `FAILED` 수렴 확인
- 리스크: 같은 예약 불일치의 신규 발생은 오류 로그·metric으로 감시 필요

## 2026-07-29 - 작업 상태 유실 노트를 DB Worker 기준으로 갱신

- 변경 파일: `2026-07-29-evaluation-job-status-lost-after-quota-update.md`, `WORK_LOG.md`
- 내용: 재현·검증 명령과 남은 위험을 현재 단일 DB Worker·실제 MySQL lease 복구 기준으로 갱신했다.
- 검증: 변경된 통합 테스트와 문서 사실 관계 대조
- 리스크: 실제 MySQL 환경 검증 미실행

## 2026-07-29 - 평가 작업 완료 상태 유실 해결 기록

- 변경 파일: `2026-07-29-evaluation-job-status-lost-after-quota-update.md`, `README.md`, `WORK_LOG.md`
- 내용: 쿼터 native UPDATE의 persistence context clear 뒤 작업 상태 변경이 유실된 원인과 호출 순서 수정, 40건 회귀 검증과 PR #68을 기록했다.
- 검증: Queue 확장 통합 테스트와 구현 대조
- 리스크: 실제 MySQL flush 순서와 운영 부하 미검증

## 2026-07-29 - 평가 요청 Idempotency 해결 기록

- 변경 파일: `2026-07-29-evaluation-request-idempotency.md`, `README.md`, `WORK_LOG.md`
- 내용: 동시 중복 생성 위험, 사용자 lock 기반 단일화와 완료 작업 7일 보존·batch 정리 결정을 기록했다.
- 검증: Spring/JPA 통합 테스트, 구현·API·운영 문서와 사실 관계 대조
- 리스크: 실제 MySQL 부하 검증은 #67에서 추적

## 2026-07-26 - 일일 쿼터 경쟁 조건 해결 기록

- 변경 파일: `2026-07-26-practice-quota-race-condition.md`, `README.md`, `WORK_LOG.md`
- 내용: 초과 예약·신규 행 생성 경쟁의 재현, MySQL Repeatable Read 조사, 원자 UPDATE와 짧은 부모 lock 해결 및 남은 위험을 기록했다.
- 검증: H2 MySQL mode와 실제 MySQL 8 테스트 결과 및 코드와 사실 관계 대조
- 리스크: 운영 부하에서 응답시간과 lock wait 측정 필요

## 2026-07-24 - 선별적 트러블슈팅 문서 체계 추가

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 영향도·재발 가능성·조사 난이도·학습 가치에 따른 작성 기준, 제외 기준, 심각도, 인덱스와 표준 템플릿을 추가했다.
- 검증: 루트 `AGENTS.md` 정책과 필수 항목 대조, 내부 링크 및 Markdown 형식 확인
- 리스크: 실제 사례 문서는 이후 기준을 만족하는 문제 수정 PR에서 추가해야 함
## 2026-08-03 - 쿼터 경쟁 조건 노트 확장

- 변경 파일: `2026-07-26-practice-quota-race-condition.md`
- 내용: 시간 충전 전환 후에도 유지되는 잠금 invariant와 migration rollback을 추가했다.
- 검증: service·repository·migration 대조
- 리스크: 실제 MySQL concurrency 운영 검증 필요
