# Work Log

## 2026-08-24 - 공통 Spring 설정을 추적 가능한 단일 기준으로 전환

- 변경 파일: `application.yaml`, `WORK_LOG.md`
- 내용: 최신 평가·심사 설정과 기존 AdMob SSV 설정을 환경변수 placeholder 기반의 정식 설정 한 파일로 통합했다. 이전 로컬 파일은 Git 제외 경로에 백업했다.
- 검증: YAML resource 포함 여부, 민감 설정의 환경변수 참조, Backend 전체 테스트
- 리스크: 백업 파일은 새 설정 검증 후 로컬에서 삭제 가능

## 2026-08-12 - 로컬 Apple client ID binding 추가

- 변경 파일: `application.yaml`, `WORK_LOG.md`
- 내용: git-ignored 로컬 실행 설정에 `APPLE_CLIENT_ID`를 `apple.client-id`로 연결했다.
- 검증: Spring ApplicationContext를 포함한 Backend 전체 테스트 통과
- 리스크: 각 실행 환경에 실제 App ID 값 주입 필요

## 2026-08-12 - AdMob SSV 환경 설정 추가

- 변경 파일: `application.yaml`, `WORK_LOG.md`
- 내용: 광고 단위 allowlist는 기본 빈 값으로 fail-closed 되게 구성했다.
- 검증: Spring 설정 load·대상 테스트 통과
- 리스크: 배포 환경변수 필수

## 2026-08-09 - 가이드 생성 HTTP API 기본 비활성화

- 변경 파일: `application.yaml`, `WORK_LOG.md`
- 내용: guide-jobs 활성화, 내부 Secret, 분당 요청 수와 동시 실행 수를 환경변수에 연결하고 안전한 기본값으로 닫았다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 활성화 시 32자 이상 Secret 주입 필요

## 2026-08-03 - Replicate 기본 대기·재시도 설정 보강

- 변경 파일: `application.yaml`, `WORK_LOG.md`
- 내용: cold start를 수용하도록 polling 기본 상한을 300초로 늘리고 429·5xx 재시도 설정 기본값을 추가했다.
- 검증: 설정 binding을 포함한 Backend 전체 단위·내부 통합 테스트 통과
- 리스크: 운영 비용과 지연 분포에 따른 값 조정 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
