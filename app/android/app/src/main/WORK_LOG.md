# 작업 이력

## 2026-08-11 - Android LingKo AdMob App ID 적용

- 변경 파일: `AndroidManifest.xml`, `WORK_LOG.md`
- 내용: Google sample App ID를 LingKo Android 앱에 발급된 AdMob App ID로 교체하고 주석을 현재 운영 설정에 맞췄다.
- 검증: Android manifest 처리 및 비밀값 노출 점검
- 리스크: 실제 운영 광고 노출 전 AdMob 앱·광고 단위 승인 상태 확인 필요

## 2026-08-08 - Android AdMob 테스트 App ID 등록

- 변경 파일: `AndroidManifest.xml`, `WORK_LOG.md`
- 내용: Google 공식 sample App ID를 Mobile Ads manifest metadata로 등록하고 release build에도 API·광고 network 권한이 포함되게 했다.
- 검증: Android debug APK 빌드 및 `:app:processReleaseMainManifest` 성공, merged manifest에서 App ID·INTERNET 권한 확인
- 리스크: 배포 전 실제 AdMob App ID로 교체해야 한다

## 2026-08-07 - 브라우저 열기를 막던 package visibility 선언 추가

- 변경 파일: `AndroidManifest.xml`
- 내용: `<queries>`에 http/https `VIEW` intent를 추가했다. Android 11(API 30)부터는 선언한 intent에 응답하는 앱만 조회할 수 있어, 선언이 없으면 설치된 브라우저를 찾지 못해 약관·처리방침 열기가 실패한다. 기존에는 TTS와 PROCESS_TEXT만 선언되어 있었다.
- 검증: `flutter analyze`, `flutter test` 통과. 실기기에서 Profile > Terms of Service 열기는 사용자 확인 필요
- 리스크: 실기기 확인 전이다

## 2026-07-28 - Android 앱 이름·아이콘 적용

- 변경 파일: `AndroidManifest.xml`, `res/mipmap-*/ic_launcher.png`, `WORK_LOG.md`
- 내용: 런처 표시명을 `LingKo`로 변경하고 기존 LingKo 로고 기반 아이콘을 Android density별 크기로 교체했다.
- 검증: Manifest 값, mipmap 48·72·96·144·192px 규격, Android Debug 빌드 확인
- 리스크: Adaptive Icon foreground/background 분리는 전용 심볼 확정 후 후속 적용 권장

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
