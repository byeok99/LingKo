# 작업 이력

## 2026-09-04 - 현재 법무 문서 버전 갱신

- 변경 파일: `LegalConsentPolicy.java`, `WORK_LOG.md`
- 내용: 앱 설정 제공 항목이 바뀐 법무 문서 시행일과 서버의 현재 동의 버전을 `2026-09-04`로 맞췄다.
- 검증: `LegalDocumentSourceSyncTest`, `LegalConsentServiceTest` 통과
- 리스크: 배포 후 기존 동의 사용자는 재동의 대상이다.

## 2026-08-07 - 현재 법무 문서 동의 버전 정책 추가

- 변경 파일: `LegalConsentPolicy.java`
- 내용: 문서 시행일을 서버의 현재 동의 버전으로 정의해 과거 기록을 보존하면서 개정 시 재동의를 판정할 수 있게 했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 문서 시행일 변경 시 앱 상수와 함께 올려야 함

## 2026-08-07 - Markdown 원본을 HTML로 변환하는 문서 서비스 추가

- 변경 파일: `LegalDocument.java`, `LegalLanguage.java`, `LegalDocumentService.java`, `LegalPageTemplate.java`
- 내용: `docs/legal/`의 Markdown을 그대로 리소스에 두고 서빙 시점에 commonmark로 변환한다. HTML 사본을 따로 관리하면 원본과 어긋났을 때 이용자에게 옛 내용이 보이므로 변환을 택했다. 문서에 표가 많아 GFM table 확장을 쓰고, 좁은 화면에서 페이지가 밀리지 않게 표만 가로 스크롤로 감싼다. 원본 맨 앞의 초안 안내 인용문은 저장소 작업자용이라 서빙 시점에만 걷어낸다. 변환 결과는 문서 2종 × 언어 2종으로 상한이 고정되어 만료 없는 캐시에 담는다.
- 검증: `./gradlew test` 통과. `LegalDocumentServiceTest` 7개, `LegalDocumentSourceSyncTest` 1개
- 리스크: 리소스 사본은 원본이 개정될 때 뒤처질 수 있어 sync 테스트로만 막고 있다. Docker 빌드는 테스트를 돌리지 않으므로 CI에서 반드시 실행해야 한다
