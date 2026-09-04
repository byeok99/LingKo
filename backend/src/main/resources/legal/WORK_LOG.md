# 작업 이력

## 2026-09-04 - Profile 메뉴 제거 법무 사본 동기화

- 변경 파일: `privacy-policy.ko.md`, `privacy-policy.en.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`, `WORK_LOG.md`
- 내용: Profile에서 제거한 광고 개인정보 설정·문의 경로를 CMP·기기 OS 설정·이메일로 교체한 원본과 서빙 사본을 동기화했다.
- 검증: `LegalDocumentSourceSyncTest`, `LegalConsentServiceTest` 통과
- 리스크: 기존 동의 사용자는 개정 문서 재동의 대상이다.

## 2026-08-12 - Azure 리전 확정본으로 사본 동기화

- 변경 파일: `privacy-policy.ko.md`, `privacy-policy.en.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`
- 내용: `docs/legal/`의 자리표시자 제거 확정본을 복사했다.
- 검증: `./gradlew test --tests '*Legal*'` 통과
- 리스크: 없음

## 2026-08-12 - 운영자 정보 확정본으로 사본 동기화

- 변경 파일: `privacy-policy.ko.md`, `privacy-policy.en.md`, `terms-of-service.ko.md`, `terms-of-service.en.md`
- 내용: `docs/legal/`에서 개인 운영자 기준으로 확정한 내용을 그대로 복사했다.
- 검증: `./gradlew test --tests '*Legal*'` 통과 (`LegalDocumentSourceSyncTest` 포함)
- 리스크: 없음

## 2026-08-07 - 약관·처리방침 서빙용 사본 배치

- 변경 파일: `terms-of-service.ko.md`, `terms-of-service.en.md`, `privacy-policy.ko.md`, `privacy-policy.en.md`
- 내용: `docs/legal/`의 문서 4종을 그대로 복사했다. Docker 빌드 컨텍스트가 `backend/`만 포함해 빌드 시점에 `docs/`를 참조할 수 없어 사본을 둔다. **원본을 고치면 여기로 복사해야 하며**, 어긋나면 `LegalDocumentSourceSyncTest`가 실패한다.
- 검증: `./gradlew test` 통과
- 리스크: 원본과 이중 관리다. 자동 복사는 Docker 컨텍스트 제약으로 도입하지 않았다
