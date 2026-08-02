# 작업 이력

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
