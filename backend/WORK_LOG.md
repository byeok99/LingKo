## 2026-07-27 - 평가 Worker 운영 설정 예시 추가

- 변경 파일: `application.example.yaml`, `WORK_LOG.md`
- 내용: presigned URL 만료와 DB Worker polling·lease·retry 설정 예시를 추가했다.
- 검증: Backend 테스트 및 integrationTest 통과
- 리스크: 운영 환경에서 Azure 처리시간에 맞춘 lease 조정 필요

## 2026-07-24 - Refresh Token 백엔드 문서 정합화

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 갱신·폐기 API가 미구현이라는 과거 주의사항을 현재 구현 상태로 교체하고 운영 전 동시 갱신 부하 검증을 후속 리스크로 분리했다.
- 검증: Refresh Token endpoint·서비스·테스트와 README 대조, `git diff --check`
- 리스크: 실제 MySQL 환경의 동시 DB refresh 부하 테스트 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `Dockerfile`, `application.example.yaml`, `build.gradle`, `docker-compose.yml`, `settings.gradle`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과, `git diff --check` 통과
- 리스크: 동작 변경 없음

## 2026-07-23 - Java 21 toolchain과 Docker 런타임 전환

- 변경 파일: `build.gradle`, `Dockerfile`, `README.md`, `WORK_LOG.md`
- 내용: Gradle Java toolchain을 21로 올리고 Docker 빌드·실행 이미지를 JDK/JRE 21로 통일했다. Virtual Thread 등 Java 21 기능은 별도로 활성화하지 않았다.
- 검증: Java 21 환경의 `./gradlew test integrationTest`와 Docker 이미지 빌드 통과, 런타임 OpenJDK 21.0.11 확인
- 리스크: 로컬 JDK 21이 없는 개발자는 설치하거나 Docker 기반 검증을 사용해야 함

## 2026-07-20 - Google OAuth backend env 설정

- 변경 파일: `.env`, `WORK_LOG.md`
- 내용: 백엔드 Google ID token 검증에 사용할 Web OAuth Client ID를 `GOOGLE_CLIENT_ID`에 설정했다. 현재 앱 로그인 방식에서는 `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`를 사용하지 않는다.
- 검증: 값 노출 없이 `.env`의 `GOOGLE_CLIENT_ID` 설정 여부를 확인함
- 리스크: `JWT_SECRET_KEY`가 아직 비어 있어 실제 로그인 테스트 전에 32자 이상의 secret을 설정해야 함
