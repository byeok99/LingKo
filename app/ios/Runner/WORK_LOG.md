# 작업 이력

## 2026-07-28 - iOS 앱 이름·아이콘 적용

- 변경 파일: `Info.plist`, `Assets.xcassets/AppIcon.appiconset/*.png`, `WORK_LOG.md`
- 내용: 홈 화면 표시명과 bundle name을 `LingKo`로 통일하고 기존 로고 기반 AppIcon 전체 크기를 교체했다.
- 검증: `plutil -lint`, AppIcon 20~1024px 규격과 RGB·비투명 형식 확인
- 리스크: 실기기 홈 화면은 기존 설치 아이콘 캐시 때문에 앱 재설치가 필요할 수 있음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AppDelegate.swift`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음, 네이티브 실기기 빌드는 미실행


## 2026-07-23 - iOS Google OAuth Server Client ID 기본 설정

- 변경 파일: `Info.plist`, `WORK_LOG.md`
- 내용: 일반 `flutter run`에서도 백엔드 검증용 Web OAuth Client ID를 사용하도록 `GIDServerClientID`를 등록했다.
- 검증: `plutil -lint Info.plist`, iOS Debug 빌드, 설치 앱의 OAuth 설정, 실제 Google 로그인과 앱 재실행 후 세션 복원 확인
- 리스크: Google Cloud Console에서 Web Client ID가 같은 프로젝트에 속하고 백엔드 `GOOGLE_CLIENT_ID`와 일치해야 함

## 2026-07-20 - iOS Google OAuth 설정 이력 이전

- 변경 파일: `Info.plist`, `WORK_LOG.md`
- 내용: iOS Google OAuth Client ID를 `GIDClientID`에 설정하고 callback URL scheme을 등록한 뒤 새 발급 값으로 교체했다. 비밀값은 기록하지 않았다.
- 검증: `cd app && plutil -lint ios/Runner/Info.plist`
- 리스크: 실제 iOS Google Sign-In callback은 simulator/device에서 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
