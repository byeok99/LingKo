# 작업 이력

## 2026-07-29 - 단일 DB Worker 확장 정책 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: SQS·4 Worker 완료 표현을 제거하고 독립 DB Worker 한 개·40 작업 검증과 측정 후 확장 원칙을 반영했다.
- 검증: 독립 Worker 통합 테스트 결과와 문서 수치 대조
- 리스크: 실제 RPS·p95·DB lock wait 미측정

## 2026-07-29 - SQS Worker 독립 확장 상태 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: SQS·독립 Worker 구현과 4 Worker/40 작업 정합성 검증을 완료로 반영하고 실제 처리량 측정은 #52로 구분했다.
- 검증: 확장 통합 테스트 결과와 문서 수치 대조
- 리스크: 실제 RPS·p95·CPU·DB Pool·SQS 지연 미측정

## 2026-07-29 - 평가 Idempotency 저장소 수명 정책 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: 동시 요청 단일화와 완료 작업 7일 보존·제한 batch 정리 정책을 확장 계획에 반영했다.
- 검증: 동시성·정리 통합 테스트와 구현 대조
- 리스크: 실제 처리량과 정리 query 비용 미측정

## 2026-07-27 - 음성 평가 부하 격리 단계 갱신

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: API 서버 multipart 처리 제거와 단일 DB Worker 도입 상태, Queue 전환 조건을 반영했다.
- 검증: ADR·구현 구조와 대조
- 리스크: 실제 처리량과 작업 대기시간 측정 필요

## 2026-07-26 - 쿼터 동시성 설계 구현 상태 반영

- 변경 파일: `scalability-plan.md`, `WORK_LOG.md`
- 내용: 쿼터 예약 기준을 예약량 포함 조건부 UPDATE로 구체화하고 최초 생성 부모 lock, 무재시도 429, 외부 호출 중 lock 미유지 정책을 반영했다.
- 검증: `PracticeQuotaService`, repository 원자 SQL, ADR-0006과 대조
- 리스크: 운영 부하에서 응답시간과 lock wait 측정 필요
