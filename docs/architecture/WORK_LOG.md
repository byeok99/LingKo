# 작업 이력

## 2026-07-29 - 평가 Idempotency 동시성·만료 흐름 반영

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: 사용자 lock 기반 단일 생성과 완료 후 7일 보존·batch 삭제 흐름을 반영했다.
- 검증: CreationService, CleanupService와 통합 테스트 대조
- 리스크: 실제 MySQL lock wait 미측정

## 2026-07-27 - S3·DB Worker 평가 구조 반영

- 변경 파일: `evaluation-flow.md`, `system-architecture.md`, `WORK_LOG.md`
- 내용: 앱 직접 업로드, MySQL 작업 상태, 단일 Worker 처리와 폴링 흐름으로 아키텍처 문서를 갱신했다.
- 검증: Backend·Flutter 구현 흐름과 대조
- 리스크: 독립 Worker·Queue 확장은 후속 단계

## 2026-07-24 - 평가 통합 흐름과 트랜잭션 경계 확정

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: 활성 세션 확인부터 쿼터 예약, 외부 평가, 결과 저장·예약 확정, 실패 복구까지 실제 구현 흐름을 문서화했다.
- 검증: application·completion service 및 통합 테스트와 대조
- 리스크: 비정상 종료 예약 회수와 멱등성은 후속 작업

## 2026-07-23 - 인증 토큰 회전·폐기 흐름 문서화

- 변경 파일: `authentication-flow.md`, `WORK_LOG.md`
- 내용: Refresh Token 해시 저장, 원자적 회전, 재사용 탐지, 절대 만료와 모바일 자동 갱신 흐름을 문서화했다.
- 검증: API·보안·데이터 모델 문서와 용어 및 endpoint 일치 여부 확인
- 리스크: 전체 기기 로그아웃은 후속 기능

## 2026-07-23 - Backend 런타임을 Java 21로 갱신

- 변경 파일: `system-architecture.md`, `WORK_LOG.md`
- 내용: Backend 배포 단위의 Java 런타임 기준을 17에서 21로 갱신했다.
- 검증: 활성 문서의 Java 버전 참조 검색과 Java 21 Docker 이미지 빌드 통과
- 리스크: 없음
