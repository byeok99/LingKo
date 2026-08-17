# 작업 이력

## 2026-08-12 - Apple 인증 신뢰 경계 반영

- 변경 파일: `authentication-flow.md`, `system-architecture.md`, `WORK_LOG.md`
- 내용: native nonce 생성부터 Apple JWK 검증·세션 발급까지의 provider 중립 흐름과 남은 revocation 경계를 기록했다.
- 검증: 앱·Backend 호출 순서와 대조
- 리스크: authorization code·refresh token 운영 흐름 미구현

## 2026-08-12 - 인증 흐름의 동의 gate 반영과 삭제된 endpoint 정리

- 변경 파일: `authentication-flow.md`, `system-architecture.md`
- 내용: 로그인 sequence에 계정 생성 전 동의와 로그인 직후 동의 기록을 넣고, 세션 복원 시 현재 문서 버전 동의를 확인하는 fail-closed 규칙을 적었다. 인증 필요 API 목록에서 V16으로 제거된 `GET/PATCH /api/users/me/preferences`를 삭제하고 실제 존재하는 저장 문장·동의 endpoint로 교체했으며, `GET /legal/{document}`가 인증 예외인 이유를 남겼다. 구성도에 Consent Gate, Legal Document Pages, Rewarded Ads + UMP를 추가했다.
- 검증: controller 목록과 대조, mermaid 블록 확인
- 리스크: 없음

## 2026-08-05 - 로마자 파생 경계 문서화

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: 로마자는 저장하지 않고 확정된 표준 발음에서 응답 시 파생하는 흐름을 명시했다.
- 검증: 구현 service와 sequence diagram 대조
- 리스크: 없음

## 2026-08-04 - 단어 점수 평가 흐름 동기화

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: Azure 단어 검증, snapshot 저장, 음절 점수 비복제 흐름을 현재 구현과 맞췄다.
- 검증: 서비스·parser 구현과 대조
- 리스크: 운영 Azure E2E 필요

## 2026-07-30 - 점수 독립 영상·terminal 실패 수렴

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: 모든 평가 음절의 전환 영상 생성과 누락된 쿼터 예약에서도 최종 실패를 commit하는 Worker 흐름을 반영했다.
- 검증: Backend 단위 201개·통합 11개 테스트 대조
- 리스크: 모든 음절 최초 cache miss의 처리시간·비용은 운영 측정 필요

## 2026-07-30 - 평가 Worker 영상 가이드 생성 연결

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: Azure 평가 후 취약 음절의 MP4 cache 조회·Replicate 보간·S3 업로드와 PNG fallback, 600초 lease·polling 경계를 흐름에 반영했다.
- 검증: Backend 단위 199개·통합 11개와 Flutter 70개 테스트 대조
- 리스크: 다중 Worker의 최초 cache miss 분산 lock은 미구현

## 2026-07-30 - 표준 발음 단일 계산 경로

- 변경 파일: `evaluation-flow.md`, `WORK_LOG.md`
- 내용: 추천 DB 발음 정답을 제거하고 추천·자유·평가·재연습이 같은 정규화·음운 규칙을 사용하며 평가 저장값은 snapshot임을 반영했다.
- 검증: Backend·Flutter 구현과 전체 테스트 대조
- 리스크: 규칙 변경이 추천 응답에 즉시 반영됨

## 2026-07-29 - Queue 없는 독립 DB Worker 흐름 반영

- 변경 파일: `evaluation-flow.md`, `system-architecture.md`, `WORK_LOG.md`
- 내용: SQS dispatcher·메시지를 제거하고 API와 web 없는 DB polling Worker 한 개의 배포·복구 흐름으로 갱신했다.
- 검증: Backend·Compose·통합 테스트와 문서 흐름 대조
- 리스크: 같은 호스트 자원 공유와 실제 MySQL lease 복구 검증 필요

## 2026-07-29 - SQS 독립 평가 흐름 반영

- 변경 파일: `evaluation-flow.md`, `system-architecture.md`, `WORK_LOG.md`
- 내용: API dispatcher, SQS jobId, 독립 Worker, DB lease와 재발행 복구 흐름 및 배포 단위를 반영했다.
- 검증: Queue 구현·Docker Compose와 문서 흐름 대조
- 리스크: 운영 SQS 장애 복구와 강제 종료 검증 필요

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
## 2026-08-03 - 평가 에너지 흐름 갱신

- 변경 파일: `evaluation-flow.md`
- 내용: 자정 초기화 대신 소비·시간 충전·서버 재조회 흐름을 반영했다.
- 검증: service 및 app 호출 흐름 대조
- 리스크: 없음
