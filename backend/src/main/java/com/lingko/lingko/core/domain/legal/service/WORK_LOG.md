# 작업 이력

## 2026-08-07 - 법무 동의 상태·기록 service 추가

- 변경 파일: `LegalConsentService.java`
- 내용: 현재 버전과 필수 항목을 방어적으로 검증하고 사용자 row lock으로 동시 재시도를 직렬화해 최초 기록을 보존한다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음
