# 작업 이력

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
