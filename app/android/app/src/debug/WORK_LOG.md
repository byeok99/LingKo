# 작업 이력

## 2026-07-27 - Android Debug 로컬 HTTP 허용

- 변경 파일: `AndroidManifest.xml`, `WORK_LOG.md`
- 내용: Android emulator가 `10.0.2.2`의 로컬 Backend HTTP endpoint에 접근할 수 있도록 Debug 빌드에만 cleartext 통신을 허용했다.
- 검증: Android Debug manifest merge·빌드 검증
- 리스크: Release 빌드에는 적용되지 않으며 운영은 HTTPS 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
