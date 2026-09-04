# 작업 이력

## 2026-08-07 - 법무 동의 영속 계약 테스트 추가

- 변경 파일: `LegalConsentServiceTest.java`
- 내용: 미동의 판정, 기록 시각·사용자 귀속, 버전 검증과 사용자 간 격리를 검증한다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-07 - 문서 변환과 원본 동기화 회귀 테스트

- 변경 파일: `LegalDocumentServiceTest.java`, `LegalDocumentSourceSyncTest.java`
- 내용: 문서 4종이 모두 열리는지, 표가 원문 파이프가 아닌 HTML 표로 변환되는지, 저장소 작업자용 초안 안내가 이용자에게 노출되지 않는지, 언어 전환 링크가 상대 언어를 가리키는지를 고정했다. 별도로 리소스 사본이 `docs/legal/` 원본과 바이트 단위로 같은지 검사해, 원본만 고치고 사본을 두고 온 경우 실패하게 했다.
- 검증: `./gradlew test` 통과
- 리스크: sync 테스트는 저장소 checkout 환경에서만 의미가 있다. Docker 빌드는 테스트를 돌리지 않으므로 CI 실행이 전제다
