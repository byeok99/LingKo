# 작업 이력

## 2026-08-07 - 법무 문서 공개 endpoint 통합 테스트

- 변경 파일: `LegalDocumentControllerIntegrationTest.java`
- 내용: `/legal/terms`와 `/legal/privacy`가 Authorization 헤더 없이 HTML로 열리는지, `lang=en`이 영문을 반환하는지, 지원하지 않는 언어가 오류 대신 기본 언어로 열리는지, 정의되지 않은 경로가 404인지를 검증한다.
- 검증: `./gradlew integrationTest` 통과
- 리스크: 없음
