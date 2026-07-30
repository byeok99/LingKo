# 작업 이력

## 2026-07-30 - Practice 표준 발음 상시 노출과 안내 축소

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 표준 발음 확인·숨김 토글을 제거하고 준비 완료 즉시 항상 표시했다. 하드코딩 진행률, 자동 준비 설명, 완료 배지, 번역과 학습 팁을 제거해 문장 입력·표준 발음·듣기·녹음에 집중하도록 정리했다.
- 검증: `flutter analyze`, `flutter test` 전체 65개 통과
- 리스크: 긴 표준 발음 줄바꿈은 실제 iPhone에서 수동 확인 필요

## 2026-07-30 - Practice 문장 준비 흐름 통합

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 추천·직접 입력 내부 탭과 수동 확정 버튼을 제거하고 하나의 편집 가능한 문장 입력으로 통합했다. 입력 중 700ms debounce 후 표준 발음을 자동 준비하며, 현재 입력과 일치하는 최신 응답에만 듣기·녹음을 허용한다.
- 검증: `flutter analyze`, `flutter test` 전체 65개 통과
- 리스크: 실제 한국어 IME 조합 중 debounce 체감과 키보드 노출 화면은 iPhone에서 수동 확인 필요

## 2026-07-30 - Practice 표준 발음 확인 토글 복원

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 상세 음절 가이드는 평가 후에 유지하면서 녹음 전에도 `Check standard pronunciation`으로 표준 발음을 선택해 확인할 수 있게 했다. 문장·모드 변경과 녹음 시작 때는 다시 접힌다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과, 펼친 Practice 402px phone 렌더링 확인
- 리스크: 실제 iPhone에서 긴 표준 발음 줄바꿈은 수동 확인 필요

## 2026-07-30 - 평가 후 표준 발음 가이드 재배치

- 변경 파일: `practice_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 녹음 전 Practice에서 표준 발음과 음절 가이드를 숨기고, Result에 원문·표준 발음만 비교하는 카드와 Normal·Slow 듣기를 추가했다. 사용자 인식 문장 표시는 제거했다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과, Result 프로토타입 402px phone 렌더링 확인
- 리스크: 긴 표준 발음 문장의 실제 iPhone 줄바꿈과 기기 TTS 음질은 수동 확인 필요

## 2026-07-30 - Practice 문장 듣기 활성화

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 비활성 Normal·Slow 버튼을 현재 한국어 원문 TTS에 연결하고 문장 변경·녹음 시작·화면 종료 시 기존 발화를 중지하며 재생 실패 안내를 추가했다.
- 검증: 자유 문장 속도 선택·안전한 오류 안내 widget test, `flutter analyze`, 전체 63개 테스트
- 리스크: 실제 iPhone 스피커·무음 모드·한국어 음성 설정 조합은 수동 확인 필요

## 2026-07-30 - Review 최근 점수 추이 시간축 수정

- 변경 파일: `review_screen.dart`, `WORK_LOG.md`
- 내용: 최신순 API 응답에서 최근 7개 점수만 선택하고 그래프를 오래된 점수에서 최신 점수 방향으로 그리며 각 점수 값을 표시하도록 수정했다.
- 검증: 최근 점수 정렬 단위 테스트, `flutter analyze`
- 리스크: 기록 API 첫 page를 기준으로 하므로 7개보다 오래된 추이는 표시하지 않음

## 2026-07-30 - preview v2 화면 정보 계층 적용

- 변경 파일: `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: Home의 실제 quota 진행률·추천 목록, Practice 단계·녹음 타이머, Result 5열 음절·약점 피드백, Review 추세, Profile 설정을 새 프로토타입 밀도에 맞췄다. 평가 중에는 명시적 백그라운드 이동을 제공한다.
- 검증: iPhone 15 비율 Home·Result PNG 렌더링 확인, `flutter analyze`, `flutter test` 61개 통과
- 리스크: 키보드가 열린 자유 문장 입력과 실제 녹음 권한 sheet는 시뮬레이터 수동 확인 필요

## 2026-07-29 - 자유 문장 특수 기호 자동 제거

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 자유 문장 입력·붙여넣기의 Unicode 문장부호·기호를 즉시 제거하고 제출 직전에도 같은 정규화를 적용해 평가 글자로 전달되지 않게 했다.
- 검증: 자유 문장 widget test 통과
- 리스크: 서버 직접 호출 정규화는 현재 범위에 포함하지 않음

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
