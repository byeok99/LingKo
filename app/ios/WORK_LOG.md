# 작업 이력

## 2026-08-02 - iPhone 15 영상 plugin Pod 연결

- 변경 파일: `Podfile.lock`, `WORK_LOG.md`
- 내용: `video_player`의 iOS 구현인 `video_player_avfoundation`을 CocoaPods 잠금 파일에 반영해 시뮬레이터에서 영상 가이드 renderer를 빌드할 수 있게 했다.
- 검증: UTF-8 locale로 iPhone 15 LingKo Debug 빌드·설치·실행, 화면 캡처와 Hot Reload 연결 확인
- 리스크: 실행 시 `LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8`가 없으면 CocoaPods가 `ASCII-8BIT` 오류로 실패할 수 있음

## 2026-07-30 - flutter_tts iOS Pod 연결

- 변경 파일: `Podfile.lock`, `WORK_LOG.md`
- 내용: iPhone에서 기기 한국어 음성을 사용할 수 있도록 `flutter_tts` iOS plugin을 CocoaPods 잠금 파일에 반영했다.
- 검증: UTF-8 환경에서 `flutter build ios --simulator --no-codesign`, `Runner.app` 생성 확인
- 리스크: CocoaPods 실행 터미널이 UTF-8이 아니면 `LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8` 지정 필요

## 2026-07-28 - iOS 실기기 Development Team 설정

- 변경 파일: `Runner.xcodeproj/project.pbxproj`, `WORK_LOG.md`
- 내용: `com.byeok.lingko`를 실제 iPhone에 자동 서명·설치할 수 있도록 Runner의 Debug, Profile, Release 구성에 Apple Development Team을 설정했다.
- 검증: 실제 iPhone Debug 설치·실행, `flutter build ios --debug --no-codesign` 통과
- 리스크: 다른 개발자는 자신의 Apple Team으로 서명 설정을 변경해야 할 수 있음

## 2026-07-23 - iOS Bundle ID를 Google OAuth 설정과 일치시킴

- 변경 파일: `Runner.xcodeproj/project.pbxproj`, `WORK_LOG.md`
- 내용: Runner의 Debug, Profile, Release Bundle ID를 Google iOS OAuth Client에 등록된 `com.byeok.lingko`로 변경하고 RunnerTests 식별자도 같은 네임스페이스로 맞췄다.
- 검증: Xcode build settings와 설치 앱 Bundle ID 확인, iOS Debug 빌드 및 실제 Google 로그인 통과
- 리스크: 실제 기기 실행 시 Apple Developer의 App ID와 provisioning profile도 `com.byeok.lingko`를 허용해야 함

## 2026-07-20 - iOS 플러그인 프로젝트 반영 이력 이전

- 변경 파일: `Podfile.lock`, `Runner.xcodeproj/project.pbxproj`, `WORK_LOG.md`
- 내용: Google Sign-In과 secure storage 네이티브 의존성 및 Xcode 프로젝트 반영 이력을 가장 가까운 안전한 상위 폴더에 기록했다. `.xcodeproj` 번들 내부에는 로그 파일을 추가하지 않는다.
- 검증: Flutter 테스트와 `plutil -lint` 통과
- 리스크: CocoaPods 재설치 시 lockfile 변경 여부 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
