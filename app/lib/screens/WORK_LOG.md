# 작업 이력

## 2026-08-04 - 화면 문자열 ARB 전환

- 변경 파일: `home_screen.dart`, `practice_screen.dart`, `result_screen.dart`, `review_screen.dart`, `profile_screen.dart`, `auth_gate_screen.dart`
- 내용: 하드코딩 영어 문자열을 AppL10n 참조로 바꾸고, 값이 끼어드는 문장은 placeholder로 정의했다. 연습 기록 개수는 복수형 규칙이 언어마다 다르므로 ICU plural로 옮겼다.
- 검증: `flutter analyze`, `flutter test` 86개 통과
- 리스크: 한국어·일본어 번역이 아직 비어 있어 두 언어를 골라도 영어로 표시됨

## 2026-08-04 - 화면 색 참조를 테마 기반으로 전환

- 변경 파일: `home_screen.dart`, `practice_screen.dart`, `result_screen.dart`, `review_screen.dart`, `profile_screen.dart`, `auth_gate_screen.dart`
- 내용: AppColors 상수 직접 참조를 context.palette로 바꿔 밝기에 따라 색이 따라오게 했다. BuildContext가 없는 CustomPainter에는 팔레트를 생성자로 주입했다.
- 검증: `flutter analyze`, `flutter test` 83개 통과, 어두운 팔레트 8개 색 조합 WCAG AA 본문 기준 충족 확인
- 리스크: 실제 기기의 다크 모드 렌더링과 이미지 가이드 대비는 수동 확인이 필요함

## 2026-08-04 - 녹음 화면 피드백 실데이터 연결과 햅틱

- 변경 파일: `practice_screen.dart`
- 내용: 고정값 0.38이던 진행 링을 실제 경과 비율로, 정지 그림이던 파형을 마이크 레벨 반응형으로 바꿨다. 표시만 하고 지켜지지 않던 10초 상한을 실제 종료로 강제하고, 60초를 넘기면 깨지던 타이머 표기를 고쳤다. 녹음 시작·종료에 촉각 피드백을 추가했다.
- 검증: `flutter analyze`, `flutter test` 81개 통과
- 리스크: 촉각 피드백과 실제 마이크 레벨은 시뮬레이터가 아닌 실기기 확인이 필요함

## 2026-08-04 - Result 화면 중복 안내 제거

- 변경 파일: `result_screen.dart`
- 내용: 버튼처럼 보이는 요소에 대한 조작 안내 문장을 제거해 단어별 발음 영역이 먼저 보이게 했다.
- 검증: `flutter analyze`, `flutter test` 80개 통과, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-04 - 상태 비교를 enum으로 교체

- 변경 파일: `result_screen.dart`
- 내용: 결과 화면의 점수 노출 판단을 ScoreStatus.isAvailable로 바꿨다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - Practice Result·Review 단어 피드백 전환

- 변경 파일: `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: 전체 음절 점수 그리드를 제거하고 두 화면 모두 단어 선택 후 해당 음절 가이드만 보여주도록 변경했다.
- 검증: `flutter analyze`, 전체 `flutter test` 통과
- 리스크: 실제 기기 scroll·bottom sheet 동작 확인 필요

## 2026-08-04 - Review 목록·상세 정보 계층 개선

- 변경 파일: `review_screen.dart`, `WORK_LOG.md`
- 내용: 최근 연습을 독립 카드로 분리하고 점수·문장·표준 발음·등급·일시를 구분했다. 상세 sheet에 표준 발음, 평가 일시, accuracy·fluency·completeness와 실제 음절 점수를 구조화했다.
- 검증: Review widget 회귀 테스트, 320px·1.8배 글꼴 응답형 테스트, `flutter analyze`, `flutter test --coverage` 전체 73개 통과, line coverage 85.20%
- 리스크: 실제 기기의 긴 문장·확대 글꼴 배치는 수동 확인 필요

## 2026-08-03 - MAX 캡슐 가변 폭 허용

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 우측 정렬과 최대 166px 제한은 유지하면서 자식 capsule이 내용 너비로 축소될 수 있게 했다.
- 검증: MAX·충전 중 우측 정렬 포함 전체 Flutter test 72개 통과
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 우측 정렬 강화

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 상단 capsule 폭을 166px로 고정하고 Home 콘텐츠의 가장 오른쪽에 정렬했다.
- 검증: 크기·우측 좌표 포함 전체 Flutter test 72개 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - Home 에너지 소형 배치 재복원

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 전체 폭 배치 변경을 취소하고 LingKo 우측 같은 줄의 소형 capsule로 되돌렸다.
- 검증: 대상·반응형 포함 전체 Flutter test 72개 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - Home 에너지 배치 디자인 롤백

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 충전 기능은 유지하고 소형 우측 캡슐을 인사말 아래의 독립된 전체 폭 영역으로 복원했다.
- 검증: 에너지 callback 및 작은 화면·큰 글씨 widget test 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - Home 상황별 추천 탐색 구성

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 추천 문장을 Daily·Food·Travel·Study·Work·Health 칩으로 구분하고 카테고리별 3개 미리보기, 전체 보기, 빈 직접 입력 진입을 추가했다. 진행 중인 평가는 추천 영역보다 위에 유지했다.
- 검증: `flutter analyze`, `flutter test --coverage` 전체 71개 통과, line coverage 83.33%, 500×924 Chrome 렌더링 확인
- 리스크: 실제 iPhone의 글꼴·가로 칩 스크롤 감각은 실기기 확인 필요

## 2026-08-03 - Profile 목표 레벨 UI 제거

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 실제 기능에 연결되지 않은 Target level 설정과 learner 배지를 제거하고 설정 영역을 Language preferences로 정리했다.
- 검증: `flutter analyze`, `flutter test --coverage` 전체 70개 통과, line coverage 83.20%
- 리스크: 없음

## 2026-07-30 - 평가·결과 화면 과도한 간소화 롤백

- 변경 파일: `home_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 아이콘 위주 구성으로 축소했던 Home 평가 카드와 Result를 직전 정보형 카드·summary·범례·상세 feedback 구성으로 복원했다. 점수 없는 가이드 진입과 표준 발음 기능은 유지했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 설명 밀도와 긴 문장 줄바꿈은 실제 iPhone에서 수동 확인 필요

## 2026-07-30 - Practice 정규화와 Result 가이드 유지

- 변경 파일: `practice_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 추천 선택·직접 입력·준비 응답의 기호 제거를 동일 경계에 적용하고, 점수 없는 음절도 URL이 있으면 Result 가이드 진입을 유지했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 실제 IME 조합과 작은 iPhone 화면은 실기기 확인 필요

## 2026-07-30 - 점수 없는 Result 가이드 진입 복구

- 변경 파일: `result_screen.dart`, `WORK_LOG.md`
- 내용: 문자 점수 `UNAVAILABLE`과 가이드 존재 여부를 분리해, `characters`가 있으면 점수 대신 `—`를 표시하면서도 음절 타일과 입·혀 가이드 진입을 유지하도록 수정했다.
- 검증: `flutter analyze`, `flutter test` 전체 66개 통과
- 리스크: 실제 평가 응답의 가이드 URL과 긴 이미지 비율은 실기기 확인 필요

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
## 2026-08-03 - Home 상단 에너지 배치

- 변경 파일: `home_screen.dart`, `practice_screen.dart`
- 내용: LingKo 제목 반대편에 작은 에너지 캡슐을 배치하고 0회 안내 문구를 갱신했다.
- 검증: 작은 화면·큰 글씨 및 전체 widget test 통과
- 리스크: 실제 기기에서 최종 시각 확인 필요
