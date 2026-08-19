# 작업 이력

## 2026-08-19 - App Review 접근 요구사항 추가

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 외부 소셜 credential·2FA 없이 전용 계정으로 심사하는 FR-AUTH-009와 완료 기준을 추가했다.
- 검증: 앱·Backend 구현 및 Review Notes Runbook과 대조
- 리스크: 운영 Secret·review 계정 수명주기는 제출마다 확인 필요

## 2026-08-12 - Apple 로그인 기능 요구사항 갱신

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: iOS Apple 로그인 코드 구현과 외부 capability·revocation 후속 상태를 분리했다.
- 검증: 구현·테스트·출시 체크리스트 대조
- 리스크: 외부 설정 완료 전 요구사항은 부분 완료

## 2026-08-12 - 광고 보상 요구사항 구현 완료 반영

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-QUOTA-004를 Google SSV 검증 구현 완료와 운영 callback 설정 후속으로 갱신했다.
- 검증: 구현·테스트 대조
- 리스크: 실제 운영 광고 E2E 필요

## 2026-08-12 - 저장 문장·설정·법무 요구사항 추가

- 변경 파일: `functional-requirements.md`
- 내용: 비어 있던 `8.8 사용자 설정` 표를 채우고(FR-SET-001~004), `8.9 법무 동의와 정책`을 신설했다(FR-LEGAL-001~006). 구현됐지만 요구사항이 없던 문장 저장·저장 목록을 FR-SENT-007·008로 추가했다. 기존 오류 절은 8.10으로 밀렸다.
- 검증: 절 번호 연속성과 다른 문서의 8.x 참조 확인(참조 없음)
- 리스크: 없음

## 2026-08-05 - 로마자 읽기 요구사항 추가

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 표준 발음 기반 로마자 파생과 Practice·Recording·Result·Review 상세 표시 완료 기준을 추가했다.
- 검증: API·Flutter 구현 및 테스트와 대조
- 리스크: 없음

## 2026-08-04 - FR-PREF 요구사항 제거

- 변경 파일: `functional-requirements.md`
- 내용: 언어 설정 기능과 FR-PREF 항목을 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 설정 요구사항 범위 축소

- 변경 파일: `functional-requirements.md`
- 내용: FR-PREF 항목에서 표시 언어를 빼고 모국어 설정으로 범위를 좁혔다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-04 - 단어 중심 피드백 요구사항 구현 완료

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 신뢰 가능한 단어 점수, guide-only 음절, 과거 기록 호환 항목을 구현 완료로 갱신했다.
- 검증: 백엔드·Flutter 테스트 계약과 대조
- 리스크: 실제 Azure 한국어 응답 운영 E2E 필요

## 2026-08-04 - 단어 점수→음절 가이드 capability 계약

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: Practice 결과와 Review에서 신뢰 가능한 단어 점수만 표시하고, 선택한 단어의 음절은 입·혀 가이드 진입점으로 사용하며 점수를 복제하지 않는 계약을 추가했다.
- 검증: 구현 전 capability 계약 확정, 코드·API·DB 검증 예정
- 리스크: Azure `ko-KR` 단어 token이 기준 공백 단위와 다르면 해당 평가의 단어 점수는 `UNAVAILABLE`로 fallback

## 2026-08-03 - 상황별 추천 문장 요구사항 구체화

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-SENT-001에 최대 50개 추천 요청, 여섯 상황 선택, 카테고리별 3개 미리보기와 전체 보기 기준을 현재 구현과 맞췄다.
- 검증: Flutter 전체 71개 테스트 및 Home 프로토타입과 대조
- 리스크: 운영 추천 데이터에서 모든 카테고리가 제공되는지 확인 필요

## 2026-07-30 - 전체 평가 음절 영상 기준 확정

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-GUIDE-004를 취약 점수가 아닌 점수 독립적인 다중 프레임 전환 기준으로 보완했다.
- 검증: Backend 단위 201개·통합 11개 테스트 대조
- 리스크: 외부 서비스 운영 E2E 필요

## 2026-07-30 - 취약 음절 영상 가이드 구현 반영

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-GUIDE-004를 취약 음절 다중 프레임 MP4 생성·cache 재사용과 PNG fallback 기준으로 완료 상태에 맞췄다.
- 검증: Backend 단위 199개·통합 11개와 Flutter 70개 테스트 대조
- 리스크: Replicate·S3·FFmpeg 운영 E2E 필요

## 2026-07-30 - 동적 발음·정규화·영상 상태 반영

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 추천 기준 발음을 현재 서버 규칙으로 계산하고 앱·서버 기호 정규화를 완료 처리했으며 영상 가이드는 앱 지원·서버 PNG 상태로 구분했다.
- 검증: Backend·Flutter 구현과 전체 테스트 대조
- 리스크: 실제 영상 URL 공급과 MySQL V12 적용은 운영 검증 필요

## 2026-07-29 - 자유 문장 특수 기호 정규화 반영

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-SENT-005에 앱 자유 문장 입력의 Unicode 문장부호·기호 즉시 제거 범위를 반영했다.
- 검증: Flutter 구현·widget test와 대조
- 리스크: 연속 공백과 서버 정규화는 부분 구현 상태

## 2026-07-29 - 회원 탈퇴·음성 보존 요구 구현 반영

- 변경 파일: `functional-requirements.md`, `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-AUTH-006과 개인정보 삭제 경로를 완료하고 음성 1일 Lifecycle·S3 우선 삭제 정책을 명시했다.
- 검증: 앱·Backend·Lifecycle 구현과 대조
- 리스크: 실제 AWS 적용과 실기기 E2E는 #71에서 추적

## 2026-07-29 - Queue 없는 독립 Worker 요구 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: SQS·4 replica 완료 표현을 제거하고 API와 독립 DB Worker 한 개 운영을 현재 완료 기준으로 갱신했다.
- 검증: Compose·Worker 배포 조건·통합 테스트 대조
- 리스크: 다중 Worker 또는 Queue 요구는 #52 측정 후 재정의 필요

## 2026-07-29 - 평가 Worker 독립 확장 요구 구현 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: NFR-PERF-009와 NFR-SCALE-005에 SQS jobId 전달, DB 원본과 API/Worker 독립 replica 구현·검증 상태를 반영했다.
- 검증: Docker 구성과 4 Worker 통합 테스트 대조
- 리스크: 운영 안전 처리량은 #52에서 확정 필요

## 2026-07-29 - Idempotency 중복 요청 검증 완료

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: 동일 평가 요청 10개의 작업·쿼터 단일화 통합 테스트 통과를 MVP 체크리스트에 반영했다.
- 검증: H2 MySQL mode 통합 테스트와 전체 Backend 테스트 통과
- 리스크: 실제 MySQL 동시 부하는 미검증

## 2026-07-27 - 음성 저장·Worker 비기능 요구 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: 비공개 직접 업로드, 영속 작업·재시도, 임시 파일 정리와 운영 확장 요구를 추가했다.
- 검증: ADR·보안·운영 문서와 교차 확인
- 리스크: 수치 SLO는 부하 측정 후 확정 필요

## 2026-07-26 - 쿼터 DB 정합성 요구사항 구현 반영

- 변경 파일: `non-functional-requirements.md`, `WORK_LOG.md`
- 내용: NFR-DB-005 원자 차감과 NFR-DB-006 사용자·날짜별 단일 행 요구사항을 #38 구현 및 동시성 검증 완료 상태로 갱신했다.
- 검증: 동시 예약 10개와 신규 행 동시 생성 반복 테스트 결과 대조
- 리스크: 운영 MySQL 부하와 lock wait은 별도 측정 필요

## 2026-07-24 - 평가 통합 요구사항 구현 상태 반영

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 인증 평가, 쿼터 확인·차감, 결과 저장, 실패 보상과 기록 내용 요구사항을 구현 완료로 갱신했다.
- 검증: 단위·Controller·Spring/JPA 통합 테스트와 대조
- 리스크: 당시 남은 쿼터 동시성은 2026-07-26 보완, 요청 멱등성 #39는 미구현

## 2026-07-24 - 전체 기기 로그아웃 Issue 연결

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: FR-AUTH-007의 후속 작업을 새 GitHub Issue #61에 연결했다.
- 검증: 생성된 GitHub Issue 제목·완료 기준과 요구사항 대조, `git diff --check`
- 리스크: 전체 기기 로그아웃 API와 UI는 미구현

## 2026-07-24 - 전체 기기 로그아웃 후속 범위 분리

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 현재 기기 세션 갱신·폐기를 완료한 Issue #40과 미구현인 전체 기기 로그아웃 요구사항을 분리하고 후속 Issue가 필요함을 명시했다.
- 검증: AuthService 로그아웃 범위와 FR-AUTH-005·007 대조, `git diff --check`
- 리스크: 전체 기기 로그아웃 API와 UI는 별도 Issue에서 구현 필요

## 2026-07-23 - 인증 세션 요구사항 구현 상태 갱신

- 변경 파일: `functional-requirements.md`, `WORK_LOG.md`
- 내용: 자동 갱신, 회전, 재사용 탐지와 현재 기기 로그아웃 요구사항을 구현 상태로 갱신했다.
- 검증: Backend·Flutter 테스트 항목과 요구사항 대조
- 리스크: 전체 기기 로그아웃 요구사항은 미구현
## 2026-08-03 - 발음 평가 에너지 요구사항 반영

- 변경 파일: `functional-requirements.md`, `non-functional-requirements.md`
- 내용: 1시간 충전, 최대 5회, MAX, 조건부 광고 버튼과 즉시 갱신을 명시했다.
- 검증: backend/app 테스트 계약 대조
- 리스크: 광고 보상 지급은 미구현
