# 작업 이력

## 2026-08-19 - Review 접근 환경변수 문서화

- 변경 파일: `local-development.md`, `WORK_LOG.md`
- 내용: 기본 비활성화, code hash·사용자 ID·시도 한도 환경변수의 의미와 안전한 기본값을 추가했다.
- 검증: `.env.example`, `application.example.yaml`, 설정 class와 대조
- 리스크: 운영 값은 저장소 밖 Secret Manager에서 별도 주입 필요

## 2026-08-12 - Apple 로그인 개발·검증 절차 추가

- 변경 파일: `local-development.md`, `testing-and-troubleshooting.md`, `WORK_LOG.md`
- 내용: `APPLE_CLIENT_ID`, App ID capability, provisioning과 실기기 점검·오류 진단 절차를 추가했다.
- 검증: 설정 파일·entitlement·iOS simulator build와 대조
- 리스크: Apple Developer portal 작업은 수동 필요

## 2026-08-11 - Flutter 로컬 환경변수 자동 로딩 절차 반영

- 변경 파일: `local-development.md`, `WORK_LOG.md`
- 내용: 매 실행마다 환경변수를 나열하던 절차를 Git에서 제외된 `app/.env.local` 1회 설정과 플랫폼별 단일 실행 명령으로 변경하고, iOS Simulator·Android AVD 자동 부팅을 명시했다.
- 검증: `app/scripts/run-local.sh` 및 자동화 테스트와 문서 예시 대조
- 리스크: 운영 광고 ID 사용 시 플랫폼별 native App ID 일치와 Google SSV 연결이 별도로 필요하다

## 2026-08-09 - 가이드 작업 로컬 설정 절차 추가

- 변경 파일: `local-development.md`, `WORK_LOG.md`
- 내용: ignore된 application 설정 생성 절차와 guide-jobs 활성화·Secret·요청·동시 실행 환경변수를 개발 가이드에 추가했다.
- 검증: `.env.example`, `application.example.yaml`, Docker 설정과 대조
- 리스크: 활성화 시 Secret을 저장소에 커밋하지 않아야 함

## 2026-08-08 - 로컬 AdMob 테스트 실행 절차 추가

- 변경 파일: `local-development.md`, `WORK_LOG.md`
- 내용: Google 공식 플랫폼별 Rewarded test Ad Unit ID와 native plugin 반영을 위한 full rebuild 절차를 추가하고 Android 최소 SDK 문서를 24로 동기화했다.
- 검증: 공식 Google sample ID로 Android/iOS debug build 성공
- 리스크: 운영 ID로 바꿀 때 native App ID도 함께 교체해야 한다

## 2026-08-03 - 외부 테스트와 가이드 seed 절차 보강

- 변경 파일: `testing-and-troubleshooting.md`, `WORK_LOG.md`
- 내용: `.env` export 실행법, Replicate 429·timeout 처리와 출시 전 repeatable guide seed 누적 절차를 추가했다.
- 검증: Backend 설정·migration·외부 테스트 동작과 문서 대조
- 리스크: 신규 S3 MP4를 seed에 반영하는 작업은 출시 전 수동 확인 필요

## 2026-07-27 - Android OAuth 패키지 문서 정합화

- 변경 파일: `local-development.md`, `testing-and-troubleshooting.md`, `WORK_LOG.md`
- 내용: Android 앱의 정식 package 적용에 맞춰 Google OAuth 등록 및 로그인 진단 기준을 `com.byeok.lingko`로 갱신했다.
- 검증: Android Gradle 설정 및 Kotlin package와 문서 대조
- 리스크: Google Cloud에 등록한 Debug SHA-1이 실제 서명 인증서와 일치해야 함

## 2026-07-27 - Android 로컬 Google 로그인 실행 절차 추가

- 변경 파일: `local-development.md`, `testing-and-troubleshooting.md`, `WORK_LOG.md`
- 내용: iOS·Android 공통 실행 스크립트, 플랫폼별 Backend 주소와 Android OAuth package·SHA-1 확인 및 계정 선택 실패 진단 절차를 추가했다.
- 검증: 앱 설정·실행 스크립트와 문서 대조
- 리스크: Android OAuth Client 등록과 실기기 네트워크는 환경별 확인 필요

## 2026-07-23 - 로컬 Backend 요구사항을 Java 21로 갱신

- 변경 파일: `local-development.md`, `WORK_LOG.md`
- 내용: 로컬 Backend 빌드와 실행에 필요한 JDK 버전을 21로 변경했다.
- 검증: Gradle toolchain과 문서의 Java 버전 일치, Java 21 환경의 Backend 테스트 통과
- 리스크: 기존 JDK 17 개발 환경은 JDK 21 설치가 필요함
