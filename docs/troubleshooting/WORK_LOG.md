# 작업 이력

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
