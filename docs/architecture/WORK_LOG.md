# 작업 이력

## 2026-07-23 - 인증 토큰 회전·폐기 흐름 문서화

- 변경 파일: `authentication-flow.md`, `WORK_LOG.md`
- 내용: Refresh Token 해시 저장, 원자적 회전, 재사용 탐지, 절대 만료와 모바일 자동 갱신 흐름을 문서화했다.
- 검증: API·보안·데이터 모델 문서와 용어 및 endpoint 일치 여부 확인
- 리스크: 전체 기기 로그아웃은 후속 기능

## 2026-07-23 - Backend 런타임을 Java 21로 갱신

- 변경 파일: `system-architecture.md`, `WORK_LOG.md`
- 내용: Backend 배포 단위의 Java 런타임 기준을 17에서 21로 갱신했다.
- 검증: 활성 문서의 Java 버전 참조 검색과 Java 21 Docker 이미지 빌드 통과
- 리스크: 없음
