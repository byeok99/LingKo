# 작업 이력

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
