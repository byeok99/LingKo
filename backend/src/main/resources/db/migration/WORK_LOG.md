# Work Log

## 2026-08-12 - SSV 광고 session migration 추가

- 변경 파일: `V20__secure_ad_rewards_with_ssv.sql`, `WORK_LOG.md`
- 내용: session table과 nullable provider transaction 전역 unique 컬럼을 추가해 기존 receipt와 호환한다.
- 검증: H2 MySQL mode migration 테스트 통과
- 리스크: 실제 MySQL 8 migration은 배포 전 검증 필요

## 2026-08-08 - 광고 보상 receipt migration 추가

- 변경 파일: `V19__add_ad_reward_receipts.sql`, `WORK_LOG.md`
- 내용: 사용자별 reward event unique 제약, 사용자 FK cascade와 조회 index를 가진 receipt table을 추가했다.
- 검증: Backend integration test의 Flyway migration 적용 성공
- 리스크: 기존 실행 중인 로컬 Docker backend는 image 재빌드가 필요하다

## 2026-08-07 - 법무 동의 이력 테이블 추가

- 변경 파일: `V18__add_legal_consents.sql`
- 내용: 사용자·문서 버전 유일 제약, 서버 기록 시각, 사용자 삭제 cascade와 이력 조회 index를 포함한 `legal_consents`를 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 실제 MySQL 운영 적용 전 backup과 migration 권한 확인 필요

## 2026-08-06 - 저장 문장 테이블 추가

- 변경 파일: `V17__add_saved_sentences.sql`
- 내용: 사용자·문장 조합을 유일하게 두어 중복 저장을 막고, 최근 저장 순 조회를 인덱스로 받친다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 취약 목록은 어절 단위다. 디자인의 음절 단위 표기는 화면에서 함께 조정해야 함

## 2026-08-04 - 언어 설정 컬럼 제거

- 변경 파일: `V16__remove_user_language_preferences.sql`
- 내용: 코드에서 참조가 사라진 display_language·native_language 컬럼을 제거했다. V13의 target_level 제거와 같은 방식이다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 단어 점수 schema 추가

- 변경 파일: `V15__add_evaluation_word_scores.sql`, `WORK_LOG.md`
- 내용: `evaluation_word`와 음절의 nullable `word_position`을 추가하고 기존 음절 score를 nullable로 명시했다.
- 검증: H2 MySQL mode migration test 통과
- 리스크: 운영 MySQL migration 적용 전 backup·rollback 절차 확인 필요

## 2026-08-03 - 사용자 목표 레벨 컬럼 제거

- 변경 파일: `V13__remove_user_target_level.sql`, `WORK_LOG.md`
- 내용: 적용된 V4를 변경하지 않고 후속 migration으로 사용되지 않는 `users.target_level` 컬럼을 제거했다.
- 검증: H2 MySQL mode에서 V1→V4→V13 적용과 언어 컬럼 보존·목표 레벨 컬럼 부재 확인, Backend 전체 217개 테스트 통과
- 리스크: 실제 MySQL V1~V13 연속 migration은 미실행

## 2026-08-03 - 생성 가이드 repeatable seed 추가

- 변경 파일: `R__seed_generated_syllable_guides.sql`, `WORK_LOG.md`
- 내용: 출시 전 생성된 `바` 입 영상과 `각` 혀 영상을 기존 `syllables` 테이블에 보존형 upsert하는 반복 migration을 추가했다.
- 검증: H2 MySQL mode migration 회귀 테스트 통과
- 리스크: 출시 전 새로 검증된 MP4 URL은 같은 seed 파일에 계속 누적해야 함

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
## 2026-08-03 - 시간 충전 컬럼 migration

- 변경 파일: `V14__add_hourly_practice_refill.sql`
- 내용: 기존 쿼터 테이블에 nullable `next_refill_at`을 추가했다.
- 검증: H2 migration test 및 integrationTest 통과
- 리스크: 실제 MySQL 환경 적용 확인 필요
