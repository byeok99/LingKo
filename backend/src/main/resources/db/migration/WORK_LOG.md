# 작업 이력

## 2026-07-30 - 추천 표준 발음 컬럼 제거

- 변경 파일: `V12__remove_recommended_pronunciation.sql`, `WORK_LOG.md`
- 내용: 적용된 V2를 수정하지 않고 후속 migration으로 `recommended_sentences.standard_pronunciation`을 제거했다.
- 검증: H2 MySQL mode V2→V12 적용과 컬럼 부재 테스트, Backend 전체 테스트 통과
- 리스크: 실제 MySQL V1~V12 연속 migration은 미실행

## 2026-07-29 - Queue 발행 스키마 제거 migration

- 변경 파일: `V11__remove_evaluation_job_queue_dispatch.sql`, `WORK_LOG.md`
- 내용: 적용 가능성이 있는 V10을 수정하지 않고 후속 migration으로 dispatch 인덱스와 `enqueued_at`을 제거했다.
- 검증: H2 MySQL mode V1·V8~V11 연속 적용과 제거 상태 확인 통과
- 리스크: 실제 MySQL V1~V11 연속 migration은 미실행

## 2026-07-29 - 평가 Queue 발행 복구 migration

- 변경 파일: `V10__add_evaluation_job_queue_dispatch.sql`, `WORK_LOG.md`
- 내용: `enqueued_at`과 PENDING 재발행 조회용 복합 인덱스를 추가했다.
- 검증: H2 MySQL mode V1~V10 마이그레이션 테스트 통과
- 리스크: 실제 MySQL V1~V10 연속 migration과 실행 계획은 미검증

## 2026-07-29 - 평가 완료 작업 정리 인덱스

- 변경 파일: `V9__add_evaluation_job_cleanup_index.sql`, `WORK_LOG.md`
- 내용: 상태와 완료 시점 기준 만료 작업 조회를 위한 복합 인덱스를 추가했다.
- 검증: H2 MySQL mode 마이그레이션 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 MySQL V1~V9 연속 migration은 미실행

## 2026-07-27 - 평가 작업 테이블 마이그레이션

- 변경 파일: `V8__add_evaluation_jobs.sql`, `WORK_LOG.md`
- 내용: 작업 상태, Idempotency, S3 object 소유, lease·retry·결과를 저장하는 `evaluation_jobs`를 추가했다.
- 검증: H2 마이그레이션 테스트와 MySQL 8 임시 DB의 V1~V8 연속 적용·제약·인덱스 확인 통과
- 리스크: 운영 적용 전 백업과 Flyway 실행 권한 확인 필요

## 2026-07-24 - 일일 쿼터 예약 계수 migration

- 변경 파일: `V7__add_practice_quota_reservations.sql`, `WORK_LOG.md`
- 내용: 외부 평가 진행 중 무료·보상 횟수를 사용량과 분리하기 위한 예약 계수를 추가했다.
- 검증: H2 MySQL 모드 migration 테스트와 JPA 통합 테스트 통과, 실제 MySQL migration은 미실행
- 리스크: 배포 전 MySQL migration 검증 필요

## 2026-07-23 - Refresh Token 해시 세션 migration

- 변경 파일: `V6__add_auth_refresh_sessions.sql`, `WORK_LOG.md`
- 내용: 사용자별 기기 세션, 현재 토큰 해시, 절대 만료와 폐기 시각을 저장하는 테이블·인덱스를 추가했다.
- 검증: H2 migration 구조 테스트와 MySQL Docker migration
- 리스크: 만료·폐기 세션 정리 배치는 운영 데이터 증가 전에 추가 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
