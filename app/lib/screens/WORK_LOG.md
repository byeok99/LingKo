# 작업 이력

## 2026-07-29 - 프로필 회원 탈퇴 확인 흐름

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 삭제 범위 경고와 명시적 확인 후 계정을 삭제하고, 실패 시 로그인 상태를 유지하며 재시도를 안내한다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 실제 기기 접근성·스토어 심사 문구는 수동 확인 필요

## 2026-07-28 - UI 최종 접근성 검토

- 변경 파일: `auth_gate_screen.dart`, `practice_screen.dart`, `WORK_LOG.md`
- 내용: 로그인 버튼 radius와 녹음 파형 pill radius를 공통 토큰으로 통일하고 작은 화면·확대 글자 흐름을 재검토했다.
- 검증: 320px 화면, 1.8배 글자 확대 widget 테스트 및 전체 Flutter 검증
- 리스크: 실제 기기별 Safe Area와 키보드 노출은 수동 확인 필요

## 2026-07-28 - 전체 앱 화면 구조와 상태 UX 재구성

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: 블루 시안에 맞춰 로그인·홈·연습·녹음·평가·결과·기록·설정을 재구성하고 녹음 종료 후 자동 평가, 전체 음절 결과, 전체 문장 재연습을 연결했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 문장 듣기와 가이드 동영상 재생은 실제 재생 계층이 없어 비활성 안내로 유지

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 프로필 세션 처리 목적 주석 보완

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 오프라인 로그아웃과 인증 만료 시 프로필 상태 삭제 목적을 명시했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 동작 변경 없음

## 2026-07-23 - Profile 보호 API 갱신·로그아웃 연결

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 기록·설정 API의 401 자동 갱신과 서버 로그아웃을 연결하고 refresh 실패 시 로그인 화면으로 전환했다.
- 검증: Flutter widget 테스트 및 전체 테스트
- 리스크: 네트워크 단절 중 로그아웃은 로컬 세션만 즉시 삭제되고 서버 세션은 절대 만료까지 남을 수 있음

## 2026-07-20 - 로그인 게이트, Google 버튼, quota 화면 이력 이전

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `WORK_LOG.md`
- 내용: 세션 복원 중 로고 스플래시와 로그인 필수 게이트를 추가하고 Google 공식 로고 버튼을 적용했다. Home의 잔여 quota 표시, Practice의 quota 소진 시 녹음 제한, Profile 인증 변경 후 quota 갱신을 연결했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 실제 Google 로그인과 quota API 연동은 emulator/device에서 수동 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
