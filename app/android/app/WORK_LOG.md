# 작업 이력

## 2026-08-08 - Android 최소 SDK 호환 범위 갱신

- 변경 파일: `build.gradle.kts`, `WORK_LOG.md`
- 내용: 현재 `flutter_tts` Android 구현 요구사항에 맞춰 minSdk를 24로 갱신했다.
- 검증: Android debug APK 빌드 성공
- 리스크: Android 6(API 23) 지원은 중단된다

## 2026-07-30 - Android TTS service 탐색 허용

- 변경 파일: `src/main/AndroidManifest.xml`, `WORK_LOG.md`
- 내용: Android 11 이상에서 기기 TTS engine을 조회할 수 있도록 package visibility `TTS_SERVICE` query를 추가했다.
- 검증: `flutter build apk --debug`로 APK 생성 확인
- 리스크: 기기에 한국어 TTS engine이 없으면 앱의 안전한 재생 실패 안내가 표시됨

## 2026-07-27 - Android 정식 패키지명 적용

- 변경 파일: `build.gradle.kts`, `WORK_LOG.md`
- 내용: Google OAuth Android Client 등록값과 일치하도록 namespace와 applicationId를 `com.byeok.lingko`로 변경했다.
- 검증: `flutter build apk --debug` 통과, 병합 manifest의 package `com.byeok.lingko` 확인
- 리스크: 기존 임시 패키지 앱과 별도 앱으로 설치되므로 기존 로컬 데이터는 승계되지 않음

## 2026-07-27 - Android SDK 36과 core library desugaring 적용

- 변경 파일: `build.gradle.kts`, `WORK_LOG.md`
- 내용: `flutter_secure_storage` 10.x 요구사항에 맞춰 compileSdk 36과 D8 core library desugaring을 적용해 Android Debug dex 빌드를 복구했다.
- 검증: `flutter build apk --debug`
- 리스크: targetSdk는 Flutter 기본값을 유지하며 출시 전 SDK 정책을 재확인해야 함

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `build.gradle.kts`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음, 네이티브 실기기 빌드는 미실행


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
