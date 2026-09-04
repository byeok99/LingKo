# 작업 이력

## 2026-08-06 - 영상 호환 보정 fallback 테스트 추가

- 변경 파일: `VideoPlaybackNormalizerTest.java`(신규), `FrameInterpolationVideoGeneratorTest.java`, `FrameInterpolationVideoGeneratorCacheTest.java`
- 내용: 보정이 실패해도 원본 경로를 돌려주고 부산물을 남기지 않는지 검증한다. 이 단계에서 예외를 던지면 Replicate 호출까지 끝난 결과가 통째로 버려진다. 기존 두 테스트는 생성자에 추가된 의존성만 반영했다.
- 검증: `./gradlew test` 통과
- 리스크: 없음

## 2026-08-06 - 단어 정렬 폐기 갈래 테스트 보강

- 변경 파일: `AzurePronunciationResultParserTest.java`
- 내용: 개수는 맞고 표기만 다른 경우(`한` vs `1`)와 문장부호만 다른 경우를 추가했다. 전자는 실제 기록에서 확인된 폐기 사례이고, 후자는 표준 발음에 마침표가 남는 흔한 상황이라 정상 통과해야 한다. 진단 로그의 `text-mismatch` 갈래도 이 테스트로 실행된다.
- 검증: `./gradlew test` 통과
- 리스크: 없음

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
