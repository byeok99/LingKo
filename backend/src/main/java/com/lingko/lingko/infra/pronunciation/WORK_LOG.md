# 작업 이력

## 2026-08-06 - 가이드 영상을 iOS가 디코딩 가능한 형식으로 정규화

- 변경 파일: `VideoPlaybackNormalizer.java`(신규), `FrameInterpolationVideoGenerator.java`
- 내용: 홀수 해상도 영상이 `yuv444p`(H.264 High 4:4:4 Predictive)로 인코딩되어 iOS에서 오류 없이 흰 화면으로만 재생됐다. `yuv420p`는 짝수 해상도를 요구하는데 Replicate 결과가 원본 PNG 크기(예: 309x157)를 그대로 따르기 때문이다. 업로드 직전에 짝수 보정 + `yuv420p` 재인코딩을 항상 수행한다. 세그먼트 1개는 병합을 건너뛰고 병합도 `-c copy`라 기존 경로에는 바로잡을 지점이 없었다. 보정 실패는 예외 대신 원본 반환으로 처리해 이미 만든 영상을 잃지 않는다.
- 검증: `./gradlew test` 통과. 실제 파일(`써-tongue` 309x157 yuv444p)을 같은 명령으로 재인코딩해 308x156 yuv420p, 프레임 9 보존 확인
- 리스크: 이미 S3에 있는 깨진 영상은 캐시로 재사용되어 자동으로 고쳐지지 않는다. 기존 객체 삭제가 필요

## 2026-08-06 - 단어 점수 폐기 이유를 로그로 남김

- 변경 파일: `AzurePronunciationResultParser.java`
- 내용: all-or-nothing 검증이 응답을 버릴 때 아무 흔적도 남기지 않아, 화면에서 어절 점수가 사라진 것만 보이고 원인을 되짚을 수 없었다. 실측 결과 로컬 기록 23건 중 5건(21.7%)만 통과하는 상태였다. 폐기 갈래를 `count-mismatch`/`text-mismatch`/`missing-score`/`missing-words-array`/`malformed-json`으로 나눠 WARN으로 남긴다. 제어 흐름은 바꾸지 않았다. 개인정보를 고려해 기준 문장 전체가 아니라 어긋난 토큰 한 쌍만 남긴다.
- 검증: `./gradlew test` 통과. `--tests '*AzurePronunciationResultParserTest' -i`로 WARN 4종 출력 확인
- 리스크: 현재 폐기율이 높아 WARN이 자주 찍힌다. 원인을 좁힌 뒤 근본 해결이 되면 자연히 줄어든다. 근본 원인은 미확정이라 트러블슈팅 노트 상태는 `조사 중`

## 2026-08-04 - all-or-nothing 파싱 계약 주석

- 변경 파일: `AzurePronunciationResultParser.java`
- 내용: 빈 목록이 '점수 없음'이 아니라 '이번 응답을 쓰지 말 것'이라는 뜻임을 method Javadoc에 남겼다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 동작 변경 없음

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
