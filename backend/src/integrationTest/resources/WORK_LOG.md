# 작업 이력

## 2026-08-03 - 외부 통합 실제 credential·Replicate 설정 연결

- 변경 파일: `application-integration.yaml`, `WORK_LOG.md`
- 내용: 외부 실행 시 `.env` AWS credential placeholder를 사용하고 Replicate retry 설정의 내부 통합 기본값을 추가했다.
- 검증: 실제 S3 업로드와 Replicate 직접 호출 외부 테스트 통과
- 리스크: `.env`는 shell에서 명시적으로 export해야 함

## 2026-07-27 - 통합 테스트 평가 실행 모드 고정

- 변경 파일: `application-integration.yaml`, `WORK_LOG.md`
- 내용: 통합 테스트에서는 Worker를 끄고 기존 multipart 호환 endpoint만 명시적으로 활성화했다.
- 검증: `./gradlew integrationTest` 통과
- 리스크: Worker 통합 테스트는 단위·마이그레이션 테스트로 분리됨

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
