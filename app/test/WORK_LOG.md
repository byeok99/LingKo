# 작업 이력

## 2026-08-06 - 로그인 배치 회귀 테스트

- 변경 파일: `widget_test.dart`
- 내용: 워드마크가 상단 20% 안, 로그인 버튼이 하단 절반에 있는지 좌표로 고정했다. 눈으로만 확인하면 다시 중앙 정렬로 돌아가도 알아채지 못한다.
- 검증: `flutter analyze`, `flutter test` 85개 통과
- 리스크: 없음

## 2026-08-06 - 리디자인 반영 테스트 갱신

- 변경 파일: `widget_test.dart`
- 내용: 버튼 문구와 카테고리 표기 변경, 캡슐 크기 기준을 새 레이아웃에 맞췄다. 최대치에서 MAX 라벨을 없앤 것도 계약으로 고정했다.
- 검증: `flutter analyze`, `flutter test` 84개 통과
- 리스크: 06 Result·03 Practice·09 Profile 리디자인과 신규 화면(10·11)은 후속 작업

## 2026-08-06 - 디자인 토큰 회귀 테스트 갱신

- 변경 파일: `design_system_test.dart`, `widget_test.dart`
- 내용: 기준을 구 preview.html에서 Direction A 핸드오프로 옮기고, 허용 굵기 4단계와 버튼 무그림자 규칙을 계약으로 고정했다. 버튼이 커지며 테스트 뷰포트(800x600) 밖으로 밀린 하단 CTA는 레이아웃을 줄이지 않고 스크롤 후 탭하도록 고쳤다.
- 검증: `flutter analyze`, `flutter test` 84개 통과. 밝은 테마 12개 색 조합과 어두운 테마 8개 조합의 WCAG AA 본문 기준 충족을 계산으로 확인
- 리스크: 화면별 위젯 리디자인은 후속 작업이다. 지금은 토큰만 교체돼 기존 레이아웃에 새 값이 적용된 상태다

## 2026-08-05 - 로마자 API·화면 회귀 테스트

- 변경 파일: `pronunciation_api_test.dart`, `sentence_api_test.dart`, `evaluation_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: API 파싱과 Practice·Recording·Result·Review 상세 표시 계약을 검증했다.
- 검증: `flutter test --coverage` 81개 통과
- 리스크: 없음

## 2026-08-04 - 언어 설정 부재 계약 고정

- 변경 파일: `widget_test.dart`, `user_preferences_api_test.dart` (삭제)
- 내용: Profile에 언어 설정이 다시 나타나지 않는다는 것과 계정 조작은 남아 있다는 것을 테스트로 고정했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 표시 언어 제거 반영

- 변경 파일: `widget_test.dart`, `user_preferences_api_test.dart`
- 내용: 표시 언어 행이 더 이상 존재하지 않는다는 계약을 단언으로 고정하고, 요청·응답 본문에서 해당 필드를 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-04 - 다크 테마 회귀 테스트

- 변경 파일: `design_system_test.dart`
- 내용: 두 밝기 테마가 모두 팔레트를 싣고 있는지, 어두운 팔레트가 밝은 값을 그대로 물려받지 않았는지 검증한다. 팔레트가 빠지면 밝기 전환이 조용히 무시되므로 계약으로 고정했다.
- 검증: `flutter analyze`, `flutter test` 83개 통과, 어두운 팔레트 8개 색 조합 WCAG AA 본문 기준 충족 확인
- 리스크: 실제 기기의 다크 모드 렌더링과 이미지 가이드 대비는 수동 확인이 필요함

## 2026-08-04 - 녹음 피드백 회귀 테스트

- 변경 파일: `widget_test.dart`
- 내용: 진행 링이 시작 시 0이고 경과에 따라 커진다는 것, 무음과 입력 상태의 안내가 달라진다는 것, 표시한 상한에서 실제로 녹음이 멈춘다는 것을 고정했다.
- 검증: `flutter analyze`, `flutter test` 81개 통과
- 리스크: 촉각 피드백과 실제 마이크 레벨은 시뮬레이터가 아닌 실기기 확인이 필요함

## 2026-08-04 - 가이드 표시 계약 회귀 테스트

- 변경 파일: `widget_test.dart`, `design_system_test.dart`
- 내용: 가이드를 한 번에 하나만 그리고 탭으로 전환한다는 계약, 자동 생성 note를 감추고 실제 조음 힌트는 노출한다는 계약을 테스트로 고정했다.
- 검증: `flutter analyze`, `flutter test` 80개 통과, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-04 - fail-closed 상태 해석 회귀 테스트

- 변경 파일: `score_status_test.dart`, `widget_test.dart`, `evaluation_api_test.dart`
- 내용: 미지의 상태 문자열과 상태값 누락이 모두 unavailable로 처리되는지 고정하고, 기존 테스트 단언을 enum으로 옮겼다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: 없음

## 2026-08-04 - 단어 중심 평가 회귀 테스트

- 변경 파일: `evaluation_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: API 단어 계층 매핑, Result 단어 전환, Review 상세, guide-only 음절 계약을 검증했다.
- 검증: 개별 RED→GREEN, 전체 `flutter test --coverage` 74개 통과, line coverage 85.4%
- 리스크: 없음

## 2026-08-04 - Review 기록 매핑·상세 회귀 테스트

- 변경 파일: `evaluation_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: history DTO의 `text/feedback/score` 매핑과 독립 기록 카드, 표준 발음·일시·세부 점수·음절 점수 상세 표시를 검증한다. 320px·1.8배 글꼴에서도 detail이 overflow 없이 표시되는지 확인한다.
- 검증: 구현 전 빈 음절과 카드 key로 실패 확인, 확대 글꼴 overflow 2건 추가 발견·수정, 대상 API·widget test, `flutter analyze`, `flutter test --coverage` 전체 73개 통과, line coverage 85.20%
- 리스크: 실제 네트워크 가이드 미디어 재생은 widget test 범위 밖

## 2026-08-03 - ProgressPanel 우측 프레임 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 166px 정렬 프레임과 실제 capsule의 오른쪽 끝이 일치하는지 검증한다.
- 검증: 구현 전 정렬 프레임 부재로 실패 후 전체 72개 test 통과, line coverage 83.86%
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 내부 여백 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: MAX 텍스트와 capsule 오른쪽 테두리 사이 간격을 8~12px 범위로 검증한다.
- 검증: 구현 전 기존 7px 간격으로 실패 확인 후 전체 72개 test 통과
- 리스크: 없음

## 2026-08-03 - MAX 캡슐 빈 공간 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: MAX 전환 후 capsule 폭이 충전 중보다 작고 115px 이하이면서 우측 끝 정렬을 유지하는지 검증한다.
- 검증: 구현 전 고정 166px로 실패 후 전체 72개 test 통과, line coverage 83.86%
- 리스크: 없음

## 2026-08-03 - 공백 없는 에너지 횟수 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: `3/5`, `4/5`, `5/5` 표시 계약으로 갱신했다.
- 검증: 구현 전 기존 공백 표시로 실패 확인 후 전체 72개 test 통과
- 리스크: 없음

## 2026-08-03 - 축소·우측 정렬 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: capsule 높이 42px 이하, 폭 174px 이하, Home 우측 콘텐츠 경계 정렬을 검증한다.
- 검증: 구현 전 48px 높이에서 실패 후 전체 72개 test 통과, line coverage 83.87%
- 리스크: 없음

## 2026-08-03 - 소형 에너지 배치 테스트 재복원

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 캡슐 높이와 LingKo 수직 정렬 계약을 직전 소형 디자인 기준으로 되돌렸다.
- 검증: 구현 전 68px 배치에서 실패 확인 후 전체 72개 test 통과, line coverage 83.81%
- 리스크: 없음

## 2026-08-03 - 에너지 디자인 롤백 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 전체 폭 68px 이상 캡슐이 LingKo 아래에 배치되면서 기존 countdown과 광고 callback이 유지되는지 검증한다.
- 검증: 구현 전 48px 소형 배치에서 실패 확인 후 대상 test 통과
- 리스크: 없음

## 2026-08-03 - Home 상황별 추천 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 50개 추천 요청, Daily 기본 선택, Food 전환, 3개 미리보기와 전체 보기, 추천 문장 선택, 빈 직접 입력 진입 계약을 widget test로 고정했다.
- 검증: 구현 전 기존 제목과 요청 상한 20으로 실패 확인, `flutter test --coverage` 전체 71개 통과, line coverage 83.33%
- 리스크: 실제 API category 데이터 분포와 가로 스크롤 제스처는 통합·실기기 확인 필요

## 2026-08-03 - 목표 레벨 제거 회귀 테스트

- 변경 파일: `user_preferences_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: Preferences 요청·응답에 목표 레벨이 없고 Profile에도 설정 행과 learner 배지가 노출되지 않으면서 언어 설정 변경은 유지되는 계약으로 갱신했다.
- 검증: 구현 전 model 생성자 compile 실패 확인, `flutter test --coverage` 전체 70개 통과, line coverage 83.20%
- 리스크: 실제 기기의 Profile 픽셀 배치는 widget test 범위 밖임

## 2026-07-30 - 평가 정보형 UI 회귀 테스트 복원

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 아이콘 전용 UI 기대값을 제거하고 평가 단계 문구, Result 발음 레이블·범례·상세 feedback, GuideSheet note·매체 안내가 유지되는 직전 계약으로 복원했다.
- 검증: `flutter test` 전체 70개 통과
- 리스크: 실제 기기 픽셀과 영상 decode는 Widget test 범위 밖임

## 2026-07-30 - 정규화·동적 발음·영상 회귀 테스트

- 변경 파일: `practice_sentence_normalizer_test.dart`, `evaluation_api_test.dart`, `pronunciation_api_test.dart`, `sentence_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 기호 제거, 준비 응답 재정규화, `마싣껟따` 추천 응답, 현재 규칙 기반 Review 재연습과 MP4 가이드 재생 계약을 고정했다.
- 검증: 구현 전 실패 확인, `flutter test` 전체 70개 통과
- 리스크: 실제 네트워크 영상 decode는 widget test 범위 밖임

## 2026-07-30 - 점수와 입·혀 가이드 독립 노출 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 문자 점수가 `UNAVAILABLE`이어도 URL이 있는 음절 타일이 표시되고, 선택 시 입·혀 가이드가 모두 열리는 회귀 조건을 추가했다.
- 검증: 구현 전 Result grid 부재 실패 확인, `flutter test` 전체 66개 통과
- 리스크: 실제 S3 네트워크 이미지는 widget test에서 다운로드하지 않음

## 2026-07-30 - Practice 표준 발음 상시 노출 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 추천·직접 입력·stale 응답·Review 재연습에서 표준 발음이 토글 없이 즉시 보이고, 진행률·설명·완료 배지·번역·학습 팁이 Practice에 노출되지 않는 계약을 검증했다.
- 검증: 구현 전 실패 확인, `flutter test` 전체 65개 통과
- 리스크: 실제 기기 픽셀·줄바꿈은 테스트 범위 밖임

## 2026-07-30 - Practice 자동 문장 준비 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 내부 추천·직접 입력 탭과 수동 제출 버튼이 사라지고, 특수기호 제거 후 700ms에 표준 발음을 자동 준비하며 늦은 이전 API 응답은 무시하는 계약을 검증했다. 같은 추천 재선택 시 미완성 draft를 교체하는 흐름도 고정했다.
- 검증: 구현 전 실패 확인, `flutter test` 전체 65개 통과
- 리스크: 실제 네트워크 취소는 지원하지 않으며 UI 상태에서 오래된 응답 적용만 차단함

## 2026-07-30 - 표준 발음 확인 토글 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 추천·자유 문장에서 표준 발음은 기본 숨김이고 사용자가 직접 펼칠 수 있으며 녹음 시작 시 다시 숨겨지는 계약을 검증했다.
- 검증: 구현 전 widget test 실패 확인, `flutter test` 전체 63개 통과
- 리스크: 없음

## 2026-07-30 - 평가 후 발음 가이드 노출 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: Practice에서는 표준 발음·가이드가 숨겨지고 Result에서 원문·표준 발음만 표시되며, 인식 문장은 숨긴 채 Normal·Slow가 표준 발음을 재생하는 계약을 검증했다.
- 검증: 구현 전 compile 실패 확인, `flutter test` 전체 63개 통과
- 리스크: 실제 기기 TTS와 픽셀 golden 비교는 테스트 범위 밖임

## 2026-07-30 - 자유 문장 TTS 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 특수 기호가 제거된 자유 문장 원문과 Normal·Slow 속도가 TTS 서비스에 전달되고 플랫폼 오류 상세는 사용자에게 노출되지 않는 계약을 검증했다.
- 검증: 구현 전 compile 실패 확인, `flutter test` 전체 63개 통과
- 리스크: 실제 음질·볼륨은 플랫폼 채널을 대체한 widget test 범위 밖임

## 2026-07-30 - Review 최근 점수 순서 회귀 테스트

- 변경 파일: `review_trend_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 최신순 기록 8개에서 최근 7개만 선택하고 오래된 점수부터 최신 점수로 반환하는 계약과 Latest score 표시를 검증했다.
- 검증: `flutter test test/review_trend_test.dart`, `flutter analyze`
- 리스크: 실제 Canvas 픽셀 golden 비교는 미구축

## 2026-07-30 - preview 디자인과 몰입형 녹음 회귀 테스트

- 변경 파일: `design_system_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 프로토타입 색상·radius·CTA·내비게이션 토큰을 고정하고 녹음·평가 중 탭 숨김, 백그라운드 이동, 작은 화면·큰 글자 흐름을 검증했다.
- 검증: `flutter test` 전체 61개 통과
- 리스크: 실제 기기 golden 비교는 미구축

## 2026-07-29 - 자유 문장 특수 기호 정규화 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 문장부호·통화기호·이모지가 포함된 입력에서 한글·영문·숫자·공백만 화면과 자유 문장 준비 API에 전달되는 회귀 조건을 추가했다.
- 검증: 대상 widget test 통과
- 리스크: 실제 한국어 IME 붙여넣기·조합 입력은 실기기 확인 필요

## 2026-07-29 - 회원 탈퇴 앱 회귀 테스트

- 변경 파일: `auth_api_test.dart`, `app_auth_service_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 두 token 전송, 성공 시 세션 삭제, 실패 시 세션 보존과 확인 dialog 흐름을 검증했다.
- 검증: `flutter test` 전체 통과
- 리스크: 실제 S3·Backend·앱 통합 E2E는 미실행

## 2026-07-28 - UI 최종 검토 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 평가 중 탭 왕복 상태 보존, 점수 4단계와 데이터 없음 문구, 작은 화면·큰 글자 Result 및 4탭 smoke 테스트를 추가했다.
- 검증: `flutter test`
- 리스크: golden 기반 픽셀 비교는 현재 테스트 체계에 없음

## 2026-07-28 - 4탭·자동 평가·전체 음절 회귀 테스트

- 변경 파일: `widget_test.dart`, `WORK_LOG.md`
- 내용: 추천 문장 이동, quota 소진, 녹음·평가 진행, 전체 음절 표시, 전체 문장 재연습, Review 기록, 권한·실패·점수 없음 상태를 검증하도록 테스트를 갱신했다.
- 검증: `flutter test`
- 리스크: 실제 기기 마이크 권한과 네트워크 중단 E2E는 수동 확인 필요

## 2026-07-27 - 직접 업로드·폴링 회귀 테스트

- 변경 파일: `evaluation_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: S3 PUT 헤더와 작업 API 직렬화, 앱의 새 평가 호출 계약을 검증하도록 테스트를 갱신했다.
- 검증: 대상 Flutter 테스트 통과
- 리스크: 실제 S3·백엔드 연계 E2E는 별도 환경 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `app_auth_service_test.dart`, `auth_api_test.dart`, `auth_session_store_test.dart`, `evaluation_api_test.dart`, `practice_quota_api_test.dart`, `pronunciation_api_test.dart`, `sentence_api_test.dart`, `user_preferences_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 테스트 목적 주석 보완

- 변경 파일: `app_auth_service_test.dart`, `auth_api_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: 각 테스트 파일이 보장하는 인증 계약과 경쟁 조건 범위를 명시했다.
- 검증: `flutter test`
- 리스크: 동작 변경 없음

## 2026-07-23 - Refresh Token 회전·자동 갱신 테스트

- 변경 파일: `auth_api_test.dart`, `app_auth_service_test.dart`, `widget_test.dart`, `WORK_LOG.md`
- 내용: refresh/logout 계약, 401 재시도, 동시 refresh 단일화, 실패 시 세션 삭제와 로그인 화면 전환, 로그아웃 중 늦은 refresh 응답 차단을 검증했다.
- 검증: `flutter test --reporter compact`
- 리스크: 실제 Access Token 만료 대기는 자동화하지 않고 401 응답으로 재현함

## 2026-07-20 - 인증 및 quota 테스트 이력 이전

- 변경 파일: `widget_test.dart`, `practice_quota_api_test.dart`, `WORK_LOG.md`
- 내용: 로그인 필수 진입, 로고 스플래시, Google 로그인 버튼 및 quota API/UI 동작을 검증하는 테스트를 추가·수정했다.
- 검증: `cd app && flutter test --reporter compact` 통과
- 리스크: 실제 OAuth 네이티브 콜백은 emulator/device 수동 검증 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - 발음 평가 에너지 회귀 테스트

- 변경 파일: `practice_quota_api_test.dart`, `widget_test.dart`
- 내용: 서버 시간 매핑, 카운트다운, MAX, 광고 callback, 즉시 갱신과 반응형 배치를 검증한다.
- 검증: 72개 test 통과, 라인 커버리지 83.81%
- 리스크: 없음
