# 작업 이력

## 2026-08-04 - Azure 단어 parser 테스트

- 변경 파일: `AzurePronunciationResultParserTest.java`, `WORK_LOG.md`
- 내용: 정확 일치, 단어 불일치, 점수 누락, 잘못된 JSON의 fail-closed 동작을 검증했다.
- 검증: 해당 테스트 통과
- 리스크: 없음

## 2026-08-03 - Replicate retry·cancel 회귀 테스트

- 변경 파일: `ReplicateApiClientTest.java`, `WORK_LOG.md`
- 내용: 첫 429 이후 생성 재시도 성공과 polling timeout 시 원격 cancel endpoint 호출·`Cancel-After` header를 검증했다.
- 검증: 대상 테스트와 Backend 전체 단위 테스트 통과
- 리스크: 실제 429 응답의 대기 문구별 지연 최적화는 미검증

## 2026-07-30 - 영상 cache hit 회귀 테스트

- 변경 파일: `FrameInterpolationVideoGeneratorCacheTest.java`, `WORK_LOG.md`
- 내용: 결정적 S3 MP4가 이미 있으면 Replicate와 FFmpeg를 호출하지 않고 기존 URL을 반환하는 계약을 검증했다.
- 검증: 대상 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 다중 프로세스 동시성은 포함하지 않음

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `FrameInterpolationVideoGeneratorTest.java`, `ReplicateApiClientTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
