# 작업 이력

## 2026-07-23 - 운영 Docker 런타임을 Java 21로 갱신

- 변경 파일: `operations-runbook.md`, `WORK_LOG.md`
- 내용: Backend 운영 이미지가 Java 21 JRE와 FFmpeg를 포함하도록 런타임 설명을 갱신했다.
- 검증: Docker 이미지 빌드 통과, 런타임 OpenJDK 21.0.11 확인
- 리스크: 배포 전 Java 21 이미지의 FFmpeg 설치와 애플리케이션 기동 확인 필요
