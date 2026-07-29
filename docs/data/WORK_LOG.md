# 작업 이력

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
