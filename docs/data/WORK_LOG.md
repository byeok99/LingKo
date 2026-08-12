# 작업 이력

## 2026-08-12 - 광고 SSV session 데이터 모델 추가

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: session token hash, 상태·만료와 provider transaction 유일 제약·탈퇴 삭제를 반영했다.
- 검증: Entity·V20 migration과 대조
- 리스크: 없음

## 2026-08-12 - 누락된 테이블 3개를 데이터 모델에 반영

- 변경 파일: `data-model.md`
- 내용: ERD와 제약·소유권 절이 V16까지만 반영돼 있었다. `saved_sentence`(V17), `legal_consents`(V18), `ad_reward_receipts`(V19)를 엔티티·관계·유일 제약·인덱스·삭제 기준까지 추가했다. 동의 기록의 `client_agreed_at`은 기기 시각 참고값이고 `recorded_at`이 서버 감사 시각이라는 구분, 광고 영수증의 `(user_idx, reward_event_id)`가 중복 지급을 막는 멱등 경계라는 점을 명시했다.
- 검증: 마이그레이션 SQL 원문과 대조, mermaid 블록 구조 확인
- 리스크: 없음

## 2026-08-04 - users 테이블 정의 갱신

- 변경 파일: `data-model.md`
- 내용: 제거한 두 컬럼을 정의에서 뺐다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 평가 단어 데이터 모델 문서

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: `evaluation_word`, nullable `word_position`, 회원 탈퇴 삭제 순서를 ERD와 제약에 반영했다.
- 검증: V15 migration·JPA entity와 대조
- 리스크: 없음

## 2026-08-03 - User 목표 레벨 데이터 모델 제거

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: V13과 User entity에 맞춰 활성 데이터 모델의 `users.target_level` field를 제거했다.
- 검증: V13 migration test와 Entity 구조 대조
- 리스크: 실제 MySQL V1~V13 연속 migration은 미실행

## 2026-07-30 - 추천 발음 비영속 모델 반영

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: 추천 문장에는 원문·콘텐츠 metadata만 저장하고 평가 log·job의 표준 발음은 당시 기준 snapshot임을 명시했다.
- 검증: Entity와 V12 migration에 대조
- 리스크: 실제 MySQL migration 미검증

## 2026-07-29 - 회원 탈퇴 데이터 소유권 반영

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: 사용자 소유 S3·세션·평가·쿼터·프로필 삭제 순서와 공용 음절 보존 기준을 반영했다.
- 검증: JPA 삭제 구현·테스트와 대조
- 리스크: 실제 MySQL 대량 삭제 성능은 미측정

## 2026-07-29 - 평가 작업 보존·정리 모델 반영

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: 완료 작업 정리 인덱스와 기본 7일 보존 정책을 데이터 소유권에 반영했다.
- 검증: V9 migration과 Cleanup query 대조
- 리스크: 실제 MySQL migration 미실행

## 2026-07-27 - 평가 작업 데이터 모델 추가

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: `evaluation_jobs` 관계, 상태·lease·Idempotency 제약과 S3 원본 삭제 소유권을 문서화했다.
- 검증: V8 Flyway migration과 Entity 대조
- 리스크: 운영 MySQL 8 연속 migration 확인 필요

## 2026-07-24 - 일일 쿼터 예약 데이터 모델 반영

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: 무료·보상 예약 계수와 성공 확정·실패 복구 의미를 데이터 모델에 추가했다.
- 검증: JPA entity와 V7 migration 대조
- 리스크: MySQL migration 검증 필요

## 2026-07-23 - Refresh Session 데이터 모델 추가

- 변경 파일: `data-model.md`, `WORK_LOG.md`
- 내용: 사용자별 다중 기기 Refresh Session 관계, 해시·절대 만료·폐기 필드와 제약을 문서화했다.
- 검증: Flyway V6 migration 및 JPA entity와 구조 대조
- 리스크: 만료·폐기 세션 정리 배치는 후속 운영 작업
## 2026-08-03 - 에너지 데이터 모델 갱신

- 변경 파일: `data-model.md`
- 내용: `next_refill_at`과 최신 행 재사용 규칙을 반영했다.
- 검증: entity 및 V14 migration 대조
- 리스크: 없음
