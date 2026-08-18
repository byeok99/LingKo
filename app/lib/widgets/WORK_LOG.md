# 작업 이력

## 2026-08-18 - 공개 공통 widget 주석 보강

- 변경 파일: `evaluation_progress_panel.dart`, `result_tile.dart`, `score_breakdown.dart`, `shared_widgets.dart`, `WORK_LOG.md`
- 내용: CTA·상태·점수·문자 표시 widget과 비동기 평가 panel의 재사용 책임을 Dartdoc에 명시했다.
- 검증: `flutter analyze` 통과, Flutter 전체 테스트 132개 통과
- 리스크: 동작 변경 없음

## 2026-08-07 - 점수 카드와 어절 탐색을 새 Result 시안에 맞춤

- 변경 파일: `score_card.dart`, `word_syllable_explorer.dart`
- 내용: 점수 구간을 low(<60)·medium(60~79)·high(80+)로 중앙화하고 전경·면·테두리·gauge 색 함수를 제공한다. 점수 카드의 padding·막대·숫자·summary 위계를 06 시안에 맞췄고, 어절 pill과 별도 상세 카드를 하나의 세로 목록·선택 행 확장 구조로 합쳤다.
- 검증: `flutter analyze`, `flutter test --coverage` 90개 통과(라인 80.40%)
- 리스크: 음절 점수는 API가 제공하지 않아 선택 어절의 구간색만 상속하며 개별 음절 점수를 만들지 않음

## 2026-08-07 - 문장 카드의 내부 여백 복구

- 변경 파일: `sentence_card.dart`
- 내용: 여러 문장 행을 감싼 카드가 바깥 padding을 제거하면서 텍스트도 테두리에 붙었다. 행 자체에 시안의 좌우 12px 여백을 주어 Home·Saved·Sound detail 추천 목록을 함께 바로잡았다.
- 검증: `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.63%)
- 리스크: 없음

## 2026-08-07 - 진행 중 CTA가 회색으로 떨어지던 문제 수정

- 변경 파일: `shared_widgets.dart`
- 내용: `PrimaryButton`이 채움 여부를 `!isLoading && onPressed != null`로 판단해, 로딩 중에 비활성 회색(neutralFill)으로 떨어졌다. 그 위에 흰 spinner를 그려 대비가 1.12:1이 되고 사용자에게는 빈 회색 버튼으로 보였다. 채움은 "동작이 붙어 있는가"(`onPressed != null`)로만 정하고 전경색이 실제 깔린 면을 따라가게 했다. Practice의 'Retry with this recording'이 실제 노출 경로였다.
- 검증: `flutter analyze`, `flutter test` 89개 통과. 대비 1.12:1 → 3.78~5.34:1
- 리스크: 라이트 모드 CTA의 흰 글자가 그라디언트 밝은 끝(#4387CA)에서 3.78:1로 WCAG AA 본문 기준(4.5:1) 미달. 도안이 정한 브랜드 색이라 임의로 바꾸지 않았다

## 2026-08-06 - 파란 카드형 공통 위젯과 어절·음절 탐색 적용

- 변경 파일: `shared_widgets.dart`, `progress_panel.dart`, `score_card.dart`, `evaluation_progress_panel.dart`, `word_syllable_explorer.dart`
- 내용: 카드 그림자와 gradient CTA를 복구하고 loading 중에도 spinner 대비를 위해 파란 채움을 유지한다. energy capsule을 수량·timer/MAX의 세로 정보 구조로 정리했다. Result/Review는 어절 pill을 선택하면 아래에 해당 음절 guide만 표시하며 가장 낮은 실제 어절 점수를 처음 선택한다.
- 검증: `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.64%)
- 리스크: 채점 API가 실제 백분율을 제공하지 않아 Scoring은 서버 단계만 ring으로 표현함

## 2026-08-06 - 가이드 영상 배속을 0.25 고정으로 변경

- 변경 파일: `guide_sheet.dart`
- 내용: 1x/0.5x/0.25x 순환 버튼을 없애고 항상 0.25배로 재생한다. 이 영상은 감상용이 아니라 따라 하기용이라 원속도로 볼 이유가 없고, 기본값이 너무 빨라 매번 같은 값으로 다시 내리게 된다. `play()` 전에 배속을 잡아 첫 반복이 원속도로 지나가지 않게 했다. 남은 조작은 멈춤/재생 하나다.
- 검증: `flutter analyze`, `flutter test` 87개 통과. 실제 재생 확인은 사용자
- 리스크: 없음

## 2026-08-06 - 07 Sound guide 시트를 화면 하단에 붙임

- 변경 파일: `guide_sheet.dart`
- 내용: `FractionallySizedBox(heightFactor: 0.9)`로 높이를 비율 고정하던 것을 걷어내고 내용 높이만큼만 차지하게 했다. 비율을 강제하면 도해가 시트 위쪽에 붙고 아래에 빈 공간이 남아, 손이 닿는 자리가 아무것도 없는 여백이 된다. 상한만 화면의 90%로 두어 작은 화면·큰 글자에서는 시트 안에서 스크롤한다.
- 검증: `flutter analyze`, `flutter test` 86개 통과
- 리스크: 없음

## 2026-08-06 - 07 Sound guide를 세로 스택으로 되돌림

- 변경 파일: `guide_sheet.dart`
- 내용: 입·혀 도해를 탭으로 전환하던 것을 핸드오프대로 세로 스택 2장(각 214px)으로 바꿨다. 입 모양과 혀 위치는 같은 소리를 설명하는 두 단면이라, 번갈아 보면 둘의 관계를 머릿속에서 맞춰야 하지만 위아래로 쌓으면 눈만 옮기면 된다. 헤더를 음절 44px + 로마자 24px로 맞추고 자모 분해는 두지 않았다. 도해가 세로로 쌓여 넘칠 수 있어 시트를 스크롤 가능하게 했다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 탭 전환 기준으로 쓰인 위젯 테스트 3건이 실패한다

## 2026-08-06 - 사용하지 않는 ActionButton 제거

- 변경 파일: `shared_widgets.dart`
- 내용: SecondaryButton의 아이콘 강제를 없앤 뒤 ActionButton은 얇은 위임만 남아 제거했다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 그라디언트·그림자 0건, 버튼 아이콘 0건 확인
- 리스크: 없음

## 2026-08-06 - 버튼·카드에서 그라디언트와 그림자 제거

- 변경 파일: `shared_widgets.dart`
- 내용: PrimaryButton이 테마를 우회해 그라디언트와 그림자를 직접 그리고 있어 토큰 교체가 화면에 반영되지 않았다. 평면 채움으로 바꿔 위계를 재질이 아니라 채움으로만 표현한다. SecondaryButton의 기본 화살표 아이콘도 없앴다. 아이콘을 강제하면 동등한 선택지 둘을 나란히 뒀을 때 한쪽이 더 무거워 보인다. AppCard의 그림자도 제거했다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 없음

## 2026-08-06 - 05 Scoring 이탈 경로 추가

- 변경 파일: `evaluation_progress_panel.dart`
- 내용: 하단 'Continue in background' 버튼을 좌상단 뒤로가기로 옮겼다. 채점 중에도 빠져나갈 길이 보여야 하고, 하단 CTA를 두면 이 화면의 목적이 '기다리기'처럼 보인다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 위젯 테스트는 이전 레이아웃·문구 기준이라 갱신이 필요하다

## 2026-08-06 - 점수 카드와 어절 아코디언

- 변경 파일: `score_card.dart`, `word_syllable_explorer.dart`, `shared_widgets.dart`
- 내용: ScoreCard를 새로 만들고 80 기준 2단계 색을 kPassingScore 한곳에서 정의했다. 어절 탐색을 가로 버튼 나열에서 세로 아코디언으로 바꿔 어절이 상위이고 음절이 그 안이라는 관계가 드러나게 했다. 음절 칩에는 점수를 표시하지 않는다. EyebrowLabel을 공용으로 뽑았다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 위젯 테스트는 이전 레이아웃 기준이라 갱신이 필요할 수 있음

## 2026-08-06 - 문장 행·에너지 캡슐·공용 위젯

- 변경 파일: `sentence_card.dart`, `progress_panel.dart`, `shared_widgets.dart`
- 내용: 문장 행을 [한국어 → 로마자 → 번역] 세로 구조로 바꾸고 북마크 토글과 취약 어절 강조를 넣었다. 재생 버튼은 제거했다(소리는 Practice에서만). 에너지 캡슐을 선으로만 묶은 pill로 바꾸고 좁은 화면에서는 내용을 숨기는 대신 통째로 축소하게 했다. Wordmark와 RomanizationText를 공용 위젯으로 뽑았다.
- 검증: `flutter analyze`, `flutter test` 84개 통과
- 리스크: 06 Result·03 Practice·09 Profile 리디자인과 신규 화면(10·11)은 후속 작업

## 2026-08-05 - 공용 로마자 발음 위젯 추가

- 변경 파일: `romanized_pronunciation.dart`, `WORK_LOG.md`
- 내용: 서버 값이 없으면 공간을 만들지 않고, 있으면 테마 보조색으로 일관되게 표시하는 위젯을 추가했다.
- 검증: `flutter analyze`, `flutter test --coverage` 통과
- 리스크: 없음

## 2026-08-04 - 위젯 색 참조를 테마 기반으로 전환

- 변경 파일: `shared_widgets.dart`, `guide_sheet.dart`, `guide_painter.dart`, `result_tile.dart`, `progress_panel.dart`, `evaluation_progress_panel.dart`, `score_breakdown.dart`, `sentence_card.dart`, `settings_row.dart`, `word_syllable_explorer.dart`
- 내용: AppCard의 배경 기본값을 nullable로 바꿔 테마에서 해석하게 하고, GuidePainter는 팔레트를 인자로 받도록 했다. Google 로그인 버튼 색은 제공자 브랜드 규정이라 테마와 무관하게 유지했다.
- 검증: `flutter analyze`, `flutter test` 83개 통과, 어두운 팔레트 8개 색 조합 WCAG AA 본문 기준 충족 확인
- 리스크: 실제 기기의 다크 모드 렌더링과 이미지 가이드 대비는 수동 확인이 필요함

## 2026-08-04 - 가이드 시트 확대와 설명 텍스트 정리

- 변경 파일: `guide_sheet.dart`, `word_syllable_explorer.dart`, `result_tile.dart`, `evaluation_progress_panel.dart`
- 내용: 입·혀 가이드를 나란히 놓아 각 가이드가 화면 폭 절반도 못 쓰던 구조를 탭 전환 단일 표시로 바꾸고, 시트 높이를 88%로 고정해 미디어 면적을 약 4배로 늘렸다. 이미지 핀치 줌과 영상 0.5x·0.25x 배속을 추가하고, 미디어 형식을 설명하던 카드와 중복 안내 문구를 제거했다.
- 검증: `flutter analyze`, `flutter test` 80개 통과, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-04 - 상태 비교를 enum으로 교체

- 변경 파일: `word_syllable_explorer.dart`, `result_tile.dart`
- 내용: 문자열 비교를 ScoreStatus.isAvailable로 바꿔 오타로 점수가 노출되는 경로를 없앴다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - 단어 선택 초기화 의도 주석

- 변경 파일: `word_syllable_explorer.dart`
- 내용: 결과 교체 시 이전 선택 위치가 다른 문장을 가리키지 않도록 첫 단어로 되돌리는 이유를 남겼다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - 단어·음절 탐색 공용 위젯

- 변경 파일: `word_syllable_explorer.dart`, `WORK_LOG.md`
- 내용: 단어 점수 chip, 선택 상태, 점수 없는 음절 guide 버튼을 Result와 Review가 공유하도록 구현했다.
- 검증: 단어 전환·가이드 진입 widget test 통과
- 리스크: 없음

## 2026-08-03 - ProgressPanel 우측 정렬 프레임 명시

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: 166px 투명 프레임 안에서 실제 capsule을 오른쪽에 붙이고 capsule 자체의 8px padding과 내용 기반 폭을 분리했다.
- 검증: 정렬 프레임·capsule 우측 좌표 포함 전체 Flutter test 72개 통과
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 내부 여백 조정

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: 가변 폭은 유지하면서 테두리와 내용 사이 좌우 padding을 6px에서 8px로 늘렸다.
- 검증: MAX trailing inset 포함 전체 Flutter test 72개 통과
- 리스크: 없음

## 2026-08-03 - MAX 상태 우측 빈 공간 제거

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: 고정 폭을 유발하던 Expanded를 제거해 `+`가 없는 MAX capsule이 실제 내용 너비만 사용한다.
- 검증: MAX 축소 및 충전 중 callback 포함 전체 Flutter test 통과
- 리스크: 없음

## 2026-08-03 - 에너지 횟수 공백 제거

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: 현재/최대 횟수를 `5/5`처럼 slash 양옆 공백 없이 표시한다.
- 검증: 표시 test 포함 전체 Flutter test 72개 통과
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 시각 밀도 개선

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: 40px 높이, 흰 카드 표면, 얇은 테두리와 soft blue icon으로 기존 UI 결에 맞췄다.
- 검증: countdown·광고 callback 포함 전체 Flutter test 통과
- 리스크: compact 요구로 터치 영역은 40px이며 전역 48px 권장값보다 작음

## 2026-08-03 - 48px 에너지 캡슐 재복원

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: 전체 폭 68px 디자인을 취소하고 직전의 202px·48px compact capsule 수치로 되돌렸다.
- 검증: 대상 및 전체 Flutter test 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - 에너지 캡슐 크기 디자인 롤백

- 변경 파일: `progress_panel.dart`, `WORK_LOG.md`
- 내용: timer·MAX·광고 callback은 유지하고 아이콘, 숫자, 버튼과 capsule 높이를 이전의 넓은 형태로 복원했다.
- 검증: 에너지 widget test와 전체 Flutter test 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-07-30 - 평가 단계·가이드 설명 롤백

- 변경 파일: `evaluation_progress_panel.dart`, `guide_sheet.dart`, `WORK_LOG.md`
- 내용: 아이콘 레일로 바꿨던 평가 4단계를 제목·설명·상태 행으로 되돌리고 GuideSheet의 note와 매체 안내를 복원했다. URL별 이미지·영상 렌더링은 유지했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 실제 네트워크 영상 재생과 긴 안내 문구는 실기기 확인 필요

## 2026-07-30 - GuideSheet URL 매체 분기

- 변경 파일: `guide_sheet.dart`, `WORK_LOG.md`
- 내용: 지원 영상 확장자는 음소거 반복 영상으로 재생하고 PNG 등 정적 URL은 이미지로 유지해 서버가 제공한 매체 형식을 보존했다.
- 검증: `flutter analyze`, MP4·이미지 widget 회귀 포함 `flutter test` 전체 70개 통과
- 리스크: 현재 서버 mapping이 PNG라 운영에서 영상 URL을 공급하기 전까지 이미지만 표시됨

## 2026-07-30 - GuideSheet 실제 자산 제목 보정

- 변경 파일: `guide_sheet.dart`, `WORK_LOG.md`
- 내용: DTO의 우선 `guideType`만 제목으로 쓰지 않고 실제 mouth·tongue URL 조합에 따라 `Mouth`, `Tongue`, `Mouth & tongue` 가이드 제목을 표시하도록 수정했다.
- 검증: `flutter analyze`, `flutter test` 전체 66개 통과
- 리스크: 이미지 URL 자체가 실패하면 기존 `Guide unavailable` 대체 UI를 표시함

## 2026-07-30 - preview v2 공통 컴포넌트 재정렬

- 변경 파일: `evaluation_progress_panel.dart`, `progress_panel.dart`, `result_tile.dart`, `score_breakdown.dart`, `sentence_card.dart`, `settings_row.dart`, `shared_widgets.dart`, `WORK_LOG.md`
- 내용: 카드 shadow, gradient CTA, compact 추천 행, 실제 quota 카드, 평가 단계 목록, 5열 음절 셀과 설정 행을 프로토타입 규격으로 변경했다.
- 검증: `flutter analyze`, `flutter test` 61개 통과
- 리스크: golden 픽셀 기준선은 추가하지 않아 플랫폼별 미세 렌더링 차이는 수동 확인 필요

## 2026-07-28 - 점수 상태·큰 글자·중복 토큰 보완

- 변경 파일: `guide_painter.dart`, `guide_sheet.dart`, `result_tile.dart`, `score_breakdown.dart`, `settings_row.dart`, `shared_widgets.dart`, `WORK_LOG.md`
- 내용: 중간·낮은 점수를 문구로 구분하고 ScoreRing 확대 글자 overflow를 방지했으며 미사용 `MetaPill`과 하드코딩 shadow·pill radius를 제거했다.
- 검증: 점수 4단계·데이터 없음, 320px/2배 글자 Result 테스트 및 전체 Flutter 검증
- 리스크: 없음

## 2026-07-28 - 블루 디자인 시스템 공통 컴포넌트 적용

- 변경 파일: `evaluation_progress_panel.dart`, `guide_sheet.dart`, `progress_panel.dart`, `result_tile.dart`, `score_breakdown.dart`, `sentence_card.dart`, `shared_widgets.dart`, `WORK_LOG.md`
- 내용: 카드·버튼·배지·점수 링·상태 패널·평가 단계·음절 점수 셀을 공통화하고 동작하지 않는 가짜 재생 버튼을 제거했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 가이드 URL은 현재 정적 이미지 표시만 지원

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `guide_painter.dart`, `guide_sheet.dart`, `progress_panel.dart`, `result_tile.dart`, `score_breakdown.dart`, `sentence_card.dart`, `settings_row.dart`, `shared_widgets.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 마이크 에너지 캡슐 구현

- 변경 파일: `progress_panel.dart`
- 내용: 현재/최대 횟수, 서버 기반 카운트다운 또는 MAX, 조건부 광고 버튼을 표시한다.
- 검증: 카운트다운·충전 만료·광고 콜백 widget test 통과
- 리스크: 광고 callback은 외부 연동 전까지 비활성
