# 작업 이력

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
