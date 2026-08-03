# 작업 이력

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
