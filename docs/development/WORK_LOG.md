# 작업 이력

## 2026-07-23 - 로컬 Backend 요구사항을 Java 21로 갱신

- 변경 파일: `local-development.md`, `WORK_LOG.md`
- 내용: 로컬 Backend 빌드와 실행에 필요한 JDK 버전을 21로 변경했다.
- 검증: Gradle toolchain과 문서의 Java 버전 일치, Java 21 환경의 Backend 테스트 통과
- 리스크: 기존 JDK 17 개발 환경은 JDK 21 설치가 필요함
