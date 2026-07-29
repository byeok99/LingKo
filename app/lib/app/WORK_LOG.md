# 작업 이력

## 2026-07-28 - UI 최종 검토 토큰 정리

- 변경 파일: `app_theme.dart`, `WORK_LOG.md`
- 내용: shadow와 pill radius를 공통 토큰으로 올리고 사용이 끝난 구형 색상 별칭을 제거했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: Google 로그인 버튼 색상은 제공자 브랜드 가이드에 따른 의도적 예외

## 2026-07-28 - 4탭 UI shell과 평가 상태 보존

- 변경 파일: `app_theme.dart`, `lingko_app.dart`, `WORK_LOG.md`
- 내용: 블루 Material 3 토큰을 정리하고 Home·Practice·Review·Profile 4탭과 shell 수준 평가 `jobId`·진행 상태를 연결했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 앱 재시작 이후 진행 중 작업 복구는 아직 지원하지 않음

## 2026-07-27 - 비동기 음성 평가 흐름 적용

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 녹음 파일 업로드 티켓 발급, S3 PUT, Idempotency 작업 생성, 완료 상태 폴링 순서로 평가 흐름을 변경했다.
- 검증: `flutter analyze`, 대상 위젯 테스트 통과
- 리스크: 네트워크 단절 시 작업 재조회 UX는 후속 보완 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `app_theme.dart`, `lingko_app.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 만료 처리 목적 주석 보완

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: quota 요청이 refresh-aware 인증 경계를 통하고 실패 시 로그인 게이트로 복귀하는 목적을 명시했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 동작 변경 없음

## 2026-07-23 - Access Token 자동 갱신 연결

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 보호된 quota 요청을 인증 실행기로 감싸고 refresh 실패 시 세션을 제거해 로그인 화면으로 전환하도록 연결했다.
- 검증: Flutter widget 테스트 및 전체 테스트
- 리스크: 평가 API JWT 연결은 Issue #36 범위로 남음

## 2026-07-20 - 인증 게이트와 quota 상태 연결 이력 이전

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 앱 시작 시 저장 세션을 복원하고 비로그인 사용자를 로그인 화면으로 제한했다. 로그인 세션과 quota 조회 상태를 앱 화면 흐름에 연결했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 실제 기기의 secure storage 세션 복원 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
