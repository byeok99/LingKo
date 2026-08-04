# 작업 이력

## 2026-08-04 - Azure 단어 점수 신뢰 검증

- 변경 파일: `AzureSpeechEvaluator.java`, `AzurePronunciationResultParser.java`, `WORK_LOG.md`
- 내용: detailed JSON의 단어 수·정규화 텍스트·위치가 기준 문장과 모두 일치할 때만 AccuracyScore를 채택한다.
- 검증: parser 단위 테스트와 백엔드 전체 단위 테스트 통과
- 리스크: 실제 Azure 한국어 응답 운영 E2E 필요

## 2026-08-03 - Replicate timeout·429 자원 보호

- 변경 파일: `ReplicateApiClient.java`, `WORK_LOG.md`
- 내용: 429·5xx에 제한된 지수 backoff를 적용하고 Prediction 생성 시 `Cancel-After`, polling timeout·interrupt 시 원격 취소를 추가했다.
- 검증: retry·cancel 단위 테스트와 실제 Replicate 직접 호출 성공
- 리스크: `Retry-After`·Jitter와 공급자 장애 메트릭은 #44 후속 범위

## 2026-07-30 - 결정적 영상 cache 재사용

- 변경 파일: `FrameInterpolationVideoGenerator.java`, `WORK_LOG.md`
- 내용: 음절·가이드 종류·프레임 조합의 hash 기반 S3 key를 조회해 기존 MP4를 재사용하고 고정 stripe lock으로 같은 프로세스의 동시 최초 생성을 직렬화했다.
- 검증: cache hit 회귀 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 서로 다른 Worker 프로세스 간 최초 cache miss 중복 생성 가능

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `AzureSpeechEvaluator.java`, `ReplicateApiClient.java`, `VideoMerger.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
