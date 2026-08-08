## 2026-08-07 - 브라우저 열기 의존성 추가

- 변경 파일: `pubspec.yaml`
- 내용: 약관·처리방침 공개 URL을 외부 브라우저로 열기 위해 `url_launcher`를 추가했다.
- 검증: `flutter pub get`, `flutter analyze`, `flutter test --coverage` 106개 통과
- 리스크: iOS는 `LSApplicationQueriesSchemes`, Android는 `<queries>` 설정이 필요할 수 있어 실기기 확인이 남아 있다

## 2026-07-30 - 모든 평가 음절 영상 계약 반영

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 점수 제공 여부와 관계없이 Result의 모든 다중 프레임 음절은 영상 URL을 재생하고 실패·단일 프레임만 PNG로 표시한다고 명시했다.
- 검증: Backend 단위 201개·통합 11개와 기존 Flutter 영상 재생 테스트 대조
- 리스크: 앱 코드 동작 변경 없음

## 2026-07-30 - 평가 영상 생성 대기 계약 문서화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 취약 음절 MP4와 PNG fallback 계약, 최초 cache miss를 위한 최대 600초 polling을 현재 앱 동작에 맞게 안내했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 실제 외부 영상 생성 시간은 실기기·운영 연동에서 확인 필요

## 2026-07-30 - 입력 정규화·영상 가이드 의존성 문서화

- 변경 파일: `README.md`, `pubspec.yaml`, `pubspec.lock`, `WORK_LOG.md`
- 내용: Unicode 문장부호·기호 제거와 서버 음운 규칙 사용을 안내하고, URL 기반 영상 가이드 재생을 위해 `video_player` 의존성을 추가했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 현재 백엔드 기본 가이드 mapping은 PNG이며 실제 영상 URL 제공은 별도 필요

## 2026-07-30 - 기기 TTS 의존성 추가

- 변경 파일: `pubspec.yaml`, `pubspec.lock`, `WORK_LOG.md`
- 내용: 추천·자유 문장을 별도 음원 저장 없이 재생하도록 `flutter_tts` 4.2.5를 앱 의존성에 추가했다.
- 검증: `flutter pub get`, `flutter analyze`, `flutter test`, iOS simulator build, Android Debug APK build
- 리스크: 실제 iPhone의 한국어 음성 설치 상태와 볼륨에 따른 재생은 실기기 확인 필요

## 2026-07-27 - Android OAuth 패키지 안내 갱신

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: Android 앱의 정식 package 적용에 맞춰 Google OAuth Client 등록 package 안내를 `com.byeok.lingko`로 변경했다.
- 검증: Android Gradle 설정과 문서 대조
- 리스크: Google Cloud 설정 반영 후 에뮬레이터 로그인 수동 확인 필요

## 2026-07-27 - 플랫폼별 Google 로그인 실행 방법 추가

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 공통 실행 스크립트의 iOS·Android 사용법, emulator Backend 주소와 Android OAuth package·SHA-1 등록 절차를 문서화했다.
- 검증: 스크립트 구문·앱 정적 분석 및 Android Debug 빌드
- 리스크: Google Cloud Android OAuth Client 등록은 저장소 밖 운영 설정임

## 2026-07-24 - Refresh Token 앱 문서 정합화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 자동 갱신이 미구현이라는 과거 설명을 제거하고 401 후 회전, 1회 재시도, 동시 갱신 단일화와 실패 시 재로그인 동작을 현재 구현에 맞게 기록했다.
- 검증: Refresh Token 관련 활성 문서 상태 검색, `git diff --check`
- 리스크: 실제 Access Token 만료를 기다리는 Android/iOS 실기기 E2E는 출시 전 확인 필요

## 2026-07-23 - Flutter 커버리지 산출물 제외

- 변경 파일: `.gitignore`, `WORK_LOG.md`
- 내용: `flutter test --coverage`가 생성하는 `coverage/` 디렉터리를 Git 추적 대상에서 제외했다.
- 검증: `git check-ignore app/coverage/lcov.info`
- 리스크: 없음

## 2026-07-20 - 기존 앱 이력의 최소 경로별 분배

- 변경 파일: `WORK_LOG.md`, 앱 하위 직접 폴더의 `WORK_LOG.md`
- 내용: 기존에 앱 상위 로그에 모여 있던 인증, quota, 이미지, iOS 설정, 테스트 이력을 실제 변경 파일이 위치한 최소 경로별 로그에도 분배했다. 이후에는 `pubspec.yaml`처럼 `app/`에 직접 위치한 파일 변경과 앱 전체 조율 이력만 이 파일에 기록한다.
- 검증: 하위 변경 파일별 직접 폴더 로그 존재 여부 확인
- 리스크: 아래 과거 항목은 기존 작업 맥락 보존을 위해 유지하며, 이후 신규 이력은 직접 폴더 로그를 기준으로 관리함

## 2026-07-20 - Google 공식 로고 로그인 버튼 적용

- 변경 파일: `assets/images/google_g_logo.png`, `pubspec.yaml`, `lib/screens/auth_gate_screen.dart`, `test/widget_test.dart`, `WORK_LOG.md`
- 내용: Google 브랜드 페이지의 full-color Google G PNG를 asset으로 추가하고, 로그인 화면 버튼을 흰색 배경의 Google 로고 포함 버튼으로 교체했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: Google 브랜드 가이드 변경 시 asset 최신성 확인이 필요하다.

## 2026-07-20 - iOS Google OAuth client 값 교체

- 변경 파일: `ios/Runner/Info.plist`, `WORK_LOG.md`
- 내용: iOS Google OAuth Client ID와 URL scheme을 새 Google Cloud 발급 값으로 교체했다.
- 검증: `cd app && plutil -lint ios/Runner/Info.plist`
- 리스크: 백엔드 검증용 Web Client ID는 별도로 `GOOGLE_CLIENT_ID`와 앱 실행 시 `GOOGLE_SERVER_CLIENT_ID`에 설정해야 한다.

## 2026-07-20 - iOS Google Sign-In OAuth 설정 추가

- 변경 파일: `ios/Runner/Info.plist`, `WORK_LOG.md`
- 내용: Google Cloud iOS OAuth Client ID를 `GIDClientID`로 추가하고, iOS URL scheme을 `CFBundleURLTypes`에 등록했다.
- 검증: `cd app && plutil -lint ios/Runner/Info.plist`
- 리스크: 백엔드 검증용 Web Client ID는 별도로 `GOOGLE_CLIENT_ID`와 앱 실행 시 `GOOGLE_SERVER_CLIENT_ID`에 설정해야 한다.

## 2026-07-20 - 로그인 게이트와 로고 스플래시 추가

- 변경 파일: `Logo.png`, `assets/images/logo.png`, `pubspec.yaml`, `lib/app/lingko_app.dart`, `lib/screens/auth_gate_screen.dart`, `lib/screens/profile_screen.dart`, `test/widget_test.dart`
- 내용: 앱 시작 시 세션 복원 중에는 로고 스플래시를 표시하고, 세션이 없으면 Home/Practice/Profile 대신 Google 로그인 화면을 먼저 보여주도록 인증 게이트를 추가했다. 루트에 있던 로고 이미지를 Flutter asset 위치로 옮기고 등록했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 실제 Google Sign-In 설정과 secure storage에 저장된 세션 복원은 emulator/device에서 수동 확인이 필요하다.

## 2026-07-20 - Flutter quota UI 연결

- 변경 파일: `lib/models/practice_quota.dart`, `lib/api/practice_quota_api.dart`, `lib/app/lingko_app.dart`, `lib/screens/home_screen.dart`, `lib/screens/practice_screen.dart`, `lib/screens/profile_screen.dart`, `test/practice_quota_api_test.dart`, `test/widget_test.dart`
- 내용: `GET /api/quota/today` 응답 모델/API를 추가하고, 로그인 세션 기준 남은 연습 횟수를 Home에 표시했다. 남은 횟수가 0이면 Practice 녹음 시작 버튼을 비활성화하고, Profile 로그인/로그아웃 후 quota를 갱신하도록 연결했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 실제 Google 로그인 세션과 백엔드 quota endpoint 연동은 emulator/device에서 수동 확인이 필요하다. 백엔드 평가 성공 시 quota 차감 연결이 완료된 뒤 평가 후 잔여 횟수 갱신도 최종 확인해야 한다.
