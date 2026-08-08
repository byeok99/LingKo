# 작업 이력

## 2026-08-07 - 법무 동의 요청·상태 DTO 추가

- 변경 파일: `LegalConsentRequest.java`, `LegalConsentStatusResponse.java`
- 내용: 필수 동의 true, 선택값 명시, 날짜 형식 문서 버전과 클라이언트 동의 시각을 validation하고 현재 재동의 필요 여부를 반환한다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음
