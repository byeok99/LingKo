# 작업 이력

## 2026-08-06 - LingKo Blue 공통 디자인 토큰 복구

- 변경 파일: `app_theme.dart`, `app_palette.dart`
- 내용: 흰 배경·파란 ink/line, 18px 카드, 15px·52px 버튼, 900 굵기 정보 계층을 design-repair 시안에 맞췄다. dark theme 구조는 유지했다.
- 검증: `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.64%)
- 리스크: 실제 기기의 dark mode 대비는 수동 확인 필요

## 2026-08-06 - radiusTile 토큰 추가

- 변경 파일: `app_theme.dart`
- 내용: 도안이 취약 음절 타일과 Review 점수 배지에 쓰는 반경 14를 토큰으로 올렸다. 기존 토큰(11·12·16·22)에 없어 두 곳 모두 리터럴로 흩어져 있었다.
- 검증: `flutter analyze`, `flutter test` 87개 통과
- 리스크: 없음

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `lingko_app.dart`
- 내용: shell 상태 `weakWords`/`openWordDetail`을 `weakSounds`/`openSoundDetail`로 바꾸고 `SoundDetailScreen`을 연결했다. 취약 음절 조회 실패는 여전히 보조 정보 취급이라 타일만 비운다.
- 검증: `flutter analyze`, `flutter test` 87개 통과
- 리스크: 없음

## 2026-08-06 - 저장 상태를 shell이 소유

- 변경 파일: `lingko_app.dart`
- 내용: 저장한 문장 식별자를 shell이 하나만 들고 Home·Result에 내려준다. 화면마다 조회하면 같은 저장 상태가 화면마다 다르게 보인다. 토글은 화면을 먼저 바꾸고 서버에 알린 뒤 응답으로 맞춘다. 왕복을 기다리면 북마크가 한 박자 늦게 반응해 눌리지 않은 것처럼 느껴진다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 저장 상태 조회 실패 시 북마크가 꺼진 것처럼 보인다. 연습 자체는 막지 않는다

## 2026-08-06 - 겹쳐 여는 화면 상태 추가

- 변경 파일: `lingko_app.dart`
- 내용: Word detail과 Saved sentences를 탭 위에 얹는 방식으로 연결했다. 탭바를 유지해 사용자가 어디에 있는지 잃지 않게 하고, 탭을 누르면 겹친 화면을 닫는다. 취약 어절 조회는 실패해도 화면에 오류를 띄우지 않는다. 보조 정보라 Home의 문장 목록은 그대로 쓸 수 있어야 한다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 북마크를 켜는 진입점(Home·Result)은 아직 서버와 연결되지 않아 목록이 비어 보일 수 있음

## 2026-08-06 - Direction A 디자인 토큰 적용

- 변경 파일: `app_theme.dart`, `app_palette.dart`
- 내용: 색·타이포·형태 토큰을 Direction A 핸드오프 값으로 교체했다. 폰트 굵기를 400~700 네 단계로 줄이고(기존 800~900 혼용 폐지), 버튼 그림자와 강조 테두리를 없애 위계를 채움→선→글자로만 표현한다. 버튼 높이 54·반경 12, 카드 반경 16으로 고정하고 line·lineSubtle·borderStrong·neutralFill·recordAccent 토큰을 추가했다.
- 검증: `flutter analyze`, `flutter test` 84개 통과. 밝은 테마 12개 색 조합과 어두운 테마 8개 조합의 WCAG AA 본문 기준 충족을 계산으로 확인
- 리스크: 화면별 위젯 리디자인은 후속 작업이다. 지금은 토큰만 교체돼 기존 레이아웃에 새 값이 적용된 상태다

## 2026-08-04 - preferences 의존성 제거

- 변경 파일: `lingko_app.dart`
- 내용: shell에서 UserPreferencesApi 주입 경로를 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 다크 모드 지원

- 변경 파일: `app_palette.dart`, `app_theme.dart`, `lingko_app.dart`
- 내용: 정적 상수로 고정돼 밝기에 따라 바뀔 수 없던 색을 ThemeExtension인 AppPalette로 옮기고 밝은·어두운 두 팔레트를 정의했다. AppTheme을 밝기별 빌더로 바꾸고 ThemeMode.system을 연결했다. 어두운 팔레트는 밝은 값을 반전하지 않고 강조색을 밝은 쪽으로, 본문은 순백 대신 회백색으로 잡아 눈부심을 줄였다.
- 검증: `flutter analyze`, `flutter test` 83개 통과, 어두운 팔레트 8개 색 조합 WCAG AA 본문 기준 충족 확인
- 리스크: 실제 기기의 다크 모드 렌더링과 이미지 가이드 대비는 수동 확인이 필요함

## 2026-08-04 - 평가 완료·실패 촉각 알림

- 변경 파일: `lingko_app.dart`
- 내용: 평가가 수 분 걸릴 수 있어 사용자가 화면을 보지 않는 경우가 많으므로, 결과 도착과 실패를 세기가 다른 촉각 피드백으로 구분해 알린다.
- 검증: `flutter analyze`, `flutter test` 81개 통과
- 리스크: 촉각 피드백과 실제 마이크 레벨은 시뮬레이터가 아닌 실기기 확인이 필요함

## 2026-08-04 - 본문 색상 대비 WCAG AA 충족

- 변경 파일: `app_theme.dart`
- 내용: textSecondary(4.28:1)와 textMuted(2.89:1)가 본문 기준 4.5:1에 미달해 각각 #5C7386(4.94:1), #627585(4.77:1)로 낮췄다. success·warning도 연한 배경 위에서 기준을 넘도록 조정했다.
- 검증: `flutter analyze`, `flutter test` 80개 통과, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-03 - Home 추천 전체 로드와 직접 입력 진입

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 여섯 상황 카테고리를 빠짐없이 구성할 수 있도록 추천 문장 요청 상한을 50개로 늘리고, Home의 직접 입력 버튼이 이전 추천 문장을 재사용하지 않는 빈 Practice로 이동하게 했다.
- 검증: `flutter analyze`, `flutter test --coverage` 전체 71개 통과, line coverage 83.33%
- 리스크: 추천 데이터의 `category` 영문 label이 계약과 다르면 해당 카테고리가 빈 상태로 표시됨

## 2026-07-30 - 최초 영상 생성 평가 polling 연장

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: Replicate 보간과 FFmpeg 병합이 필요한 최초 cache miss에도 완료 결과를 받을 수 있도록 평가 polling 상한을 120초에서 600초로 늘렸다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 장시간 처리 중 별도 진행률은 제공하지 않음

## 2026-07-30 - 현재 발음 규칙 기반 재연습

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: Review 재연습에서 기록의 과거 표준 발음 snapshot을 재사용하지 않고 추천 단건 또는 자유 문장 준비 API를 다시 호출한다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 재준비 네트워크 실패 시 Review에 머물며 SnackBar로 재시도를 안내함

## 2026-07-30 - Practice 자동 준비 문장 반영 책임 명시

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: Practice의 통합 입력에서 자동 준비가 끝난 최신 문장을 shell의 현재 연습 대상으로 반영하는 콜백 책임을 주석에 맞췄다.
- 검증: `flutter analyze`, `flutter test` 전체 65개 통과
- 리스크: 동작 변경 없음

## 2026-07-30 - Result 표준 발음 TTS 주입

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 평가 결과의 표준 발음 카드도 기존 문장 TTS 서비스를 재사용하도록 Result 화면에 동일한 서비스 경계를 주입했다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과
- 리스크: 없음

## 2026-07-30 - 문장 TTS 서비스 앱 주입

- 변경 파일: `lingko_app.dart`, `WORK_LOG.md`
- 내용: 기본 기기 TTS 구현을 shell에서 생성·종료하고 Practice 화면에 인터페이스로 주입해 플랫폼 기능을 테스트 대역으로 교체할 수 있게 했다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과
- 리스크: 없음

## 2026-07-30 - preview v2 디자인 토큰과 몰입형 shell 적용

- 변경 파일: `app_theme.dart`, `lingko_app.dart`, `WORK_LOG.md`
- 내용: `docs/design/preview.html`의 블루·잉크 색상, 18px 카드, 52px CTA, 76px 하단 내비게이션을 전역 토큰으로 옮기고 녹음·평가 중 탭을 숨기는 shell 상태를 연결했다.
- 검증: `flutter analyze`, `flutter test` 61개 통과
- 리스크: 실제 iPhone Safe Area와 시스템 글꼴 렌더링은 수동 확인 필요

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
## 2026-08-03 - 발음 평가 에너지 상태 동기화

- 변경 파일: `lingko_app.dart`
- 내용: 광고 보상 콜백 경계와 평가 차감 직후 서버 쿼터 재조회 흐름을 연결했다.
- 검증: `flutter analyze`, `flutter test --coverage` 통과
- 리스크: 광고 SDK 및 서버 보상 지급 구현은 후속 작업
