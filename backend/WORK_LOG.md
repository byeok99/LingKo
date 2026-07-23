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
