# 작업 이력

## 2026-08-09 - 가이드 생성 비용 admission 강화

- 변경 파일: `GuideGenerationJobService.java`, `GuideGenerationJobTelemetry.java`, `GuideSourceUrlPolicy.java`, `WORK_LOG.md`
- 내용: 모든 source URL을 job 등록 전에 검증하고 중복 제거 뒤 최대 동시 실행 슬롯을 원자적으로 예약·해제했다. admission·완료 결과를 관측 인터페이스로 기록한다.
- 검증: `GuideGenerationJobServiceTest`, Backend 전체 단위·통합 테스트 통과; 변경 서비스 line coverage 83.51%
- 리스크: job 상태와 슬롯은 process memory에 있어 재시작·다중 instance 정합성은 #42 범위

## 2026-08-06 - 음절 상세 조회에 형식 검증 추가

- 변경 파일: `WeakSoundService.java`
- 내용: 조회 대상이 한글 음절 한 글자가 아니면 빈 결과를 돌려준다. 이 값이 `like` 패턴 한가운데로 들어가 `%`나 `_`를 넘기면 사용자의 모든 어절이 "이 음절의 연습 기록"으로 잡혔다. 사용자 본인 자료로 범위가 제한돼 노출 문제는 아니지만 화면이 거짓을 말하게 된다. 이스케이프 대신 형식 검증으로 막은 이유는 음절이 아닌 입력에는 보여줄 결과가 없어 빈 응답이 정확한 답이기 때문이다.
- 검증: `./gradlew test` 통과
- 리스크: 없음

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `WeakWordService.java` → `WeakSoundService.java`
- 내용: 어절 점수를 그 어절의 음절들에 귀속시켜 음절별 가중 평균을 만든다. 음절 자체의 측정 점수는 존재하지 않는다(`AzureSpeechEvaluator.characterScoresAvailable(false)`, `evaluation_syllable.score`는 항상 NULL). 시도 횟수로 가중해 표본이 적은 어절이 평균을 끌고 가지 않게 했고, 한 어절 안의 반복 글자는 한 번만 센다. 문자열을 글자로 쪼개 group by 하는 일은 JPQL로 표현되지 않고 DB 함수는 벤더에 묶여 Java에서 집계한다. 입력은 점수 낮은 순 500개 어절로 상한을 뒀다.
- 검증: `./gradlew test`, `./gradlew integrationTest` 통과
- 리스크: 음절 점수는 측정값이 아닌 통계적 추정이다. 어절 안에서 실제로 틀린 음절이 아닌 이웃 음절도 같은 점수를 받으므로, 표본이 적을 때는 엉뚱한 음절이 상위에 올 수 있다. 최소 2회 조건으로만 완화한 상태

## 2026-08-06 - 결과 판정 문구 축약

- 변경 파일: `EvaluationService.java`
- 내용: 두 문장에 인식된 발음까지 붙던 요약을 한 줄 판정으로 줄였다. 이 문구는 점수와 어절 목록 사이에 놓여, 길면 사용자가 무엇이 틀렸는지 보러 가는 길을 가로막는다. 인식된 발음은 보정을 거친 값이라 사용자가 실제로 낸 소리로 오해할 수 있어 노출을 끊었다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 없음

## 2026-08-06 - 취약 어절 집계 서비스 추가

- 변경 파일: `WeakWordService.java`, `EvaluationService.java`, `EvaluationHistoryService.java`
- 내용: 반복해서 틀리는 어절을 평균 점수 오름차순으로 집계하고, 어절 하나의 과거 시도와 다음 후보를 한 응답으로 제공한다. 음절이 아니라 어절 단위인 이유는 신뢰할 수 있는 점수의 최소 단위가 어절이기 때문이다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 취약 목록은 어절 단위다. 디자인의 음절 단위 표기는 화면에서 함께 조정해야 함

## 2026-08-05 - 준비·기록 응답 로마자 파생

- 변경 파일: `EvaluationService.java`, `EvaluationHistoryService.java`, `WORK_LOG.md`
- 내용: 자유 문장 준비와 과거 평가 기록에서 표준 발음 기반 로마자 가이드를 반환한다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-04 - 무의미한 안내 문구 생성 제거

- 변경 파일: `EvaluationService.java`
- 내용: guideType을 그대로 문장에 끼워 넣어 가이드 없는 글자에 'Focus on none placement'가 노출되던 문제를 고쳤다. 가이드가 있을 때만 부위별 안내를 만들고 없으면 빈 문자열을 반환한다.
- 검증: `flutter analyze`, `flutter test` 80개 통과, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-04 - 신뢰 불가 단어 행 저장 생략과 상태 enum 적용

- 변경 파일: `EvaluationPersistenceService.java`, `EvaluationService.java`, `EvaluationHistoryService.java`
- 내용: 점수를 신뢰할 수 있을 때만 단어 행을 저장해 standard_pronunciation에서 파생 가능한 정보의 중복 저장과 word_text 길이 초과를 함께 없앴고, 상태 문자열을 ScoreStatus·GuideStatus enum으로 교체했다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: 저장된 단어 행이 없는 기록은 조회 시 standard_pronunciation 기준 복원에 의존함

## 2026-08-04 - 단어 중심 평가 조립·저장·조회

- 변경 파일: `EvaluationService.java`, `EvaluationPersistenceService.java`, `EvaluationHistoryService.java`, `WORK_LOG.md`
- 내용: 신뢰 가능한 단어 점수만 노출·저장하고 음절에는 점수를 복제하지 않으며 과거 기록도 점수 없는 단어 그룹으로 복원한다.
- 검증: 관련 서비스 RED→GREEN 및 백엔드 전체 단위 테스트 통과
- 리스크: 실제 Azure 한국어 detailed JSON 운영 검증 필요

## 2026-08-03 - 생성 가이드 URL DB 재사용

- 변경 파일: `GuideMediaResolver.java`, `WORK_LOG.md`
- 내용: 기존 `syllables`의 MP4를 평가 영상 생성 전에 재사용하고 새 생성 URL은 입·혀 칼럼을 보존하며 upsert하도록 했다.
- 검증: DB cache hit·신규 저장 단위 테스트와 Backend 전체 단위·내부 통합 테스트 통과
- 리스크: 같은 프레임이어도 음절이 다르면 현재 S3 hash가 달라질 수 있음

## 2026-07-30 - 점수 없는 영상 가이드·고착 실패 종결

- 변경 파일: `EvaluationService.java`, `EvaluationJobProcessingService.java`, `WORK_LOG.md`
- 내용: Result의 모든 음절을 전환 영상 resolver로 처리하고, 최종 실패 시 쿼터 예약이 이미 없어도 오류를 기록한 뒤 작업을 `FAILED`로 commit한다.
- 검증: Backend 단위 201개·통합 11개 통과, 배포 후 고착 작업 4건 모두 `FAILED` 수렴
- 리스크: 누락된 예약의 발생 원인은 운영 로그·metric으로 별도 감시 필요

## 2026-07-30 - 취약 음절 영상 가이드 해석

- 변경 파일: `EvaluationService.java`, `GuideMediaResolver.java`, `WORK_LOG.md`
- 내용: 평가 완료 시 신뢰 가능한 글자 점수가 80점 미만인 음절만 프레임 전환 영상을 요청하고 단일 프레임·생성 실패는 첫 PNG로 fallback한다.
- 검증: 대상 회귀 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: 점수 신뢰도가 없거나 80점 이상인 글자는 정적 가이드 유지

## 2026-07-30 - 모든 평가 대상의 현재 발음 규칙 적용

- 변경 파일: `EvaluationService.java`, `EvaluationApplicationService.java`, `EvaluationJobService.java`, `WORK_LOG.md`
- 내용: 추천·자유 문장의 원문을 정규화하고 추천 조회·legacy 평가·비동기 작업 모두 DB 발음값 없이 `KoreanPhonemeUtil` 결과를 사용하게 했다.
- 검증: Backend 단위 190개·통합 11개 통과
- 리스크: 기존 완료 평가의 발음 snapshot은 역사 재현을 위해 유지됨

## 2026-07-29 - 사용자별 원격 음성 삭제 계약

- 변경 파일: `EvaluationAudioStorage.java`, `WORK_LOG.md`
- 내용: 회원 탈퇴 시 제출 여부와 무관하게 사용자 prefix의 원격 음성을 일괄 삭제하는 저장소 경계를 추가했다.
- 검증: S3 구현 단위 테스트와 Backend 전체 테스트 통과
- 리스크: 실제 AWS 권한·Versioning 동작은 운영 환경 검증 필요

## 2026-07-29 - Queue 없는 독립 DB Worker로 단순화

- 변경 파일: `EvaluationJobExecutor.java`, `EvaluationJobProcessingService.java`, `EvaluationJobQueue.java`, `EvaluationJobQueueDispatcher.java`, `EvaluationJobQueueWorker.java`, `EvaluationJobWorker.java`, `WORK_LOG.md`
- 내용: SQS 계약·dispatcher·consumer claim을 제거하고 별도 프로세스의 DB polling Worker가 공통 평가 실행기를 호출하도록 유지했다.
- 검증: Worker 단위·배포 조건·단일 Worker 40건 통합 테스트 통과
- 리스크: 단일 Worker 처리량과 강제 종료 lease 복구는 실제 MySQL에서 확인 필요

## 2026-07-29 - SQS 독립 평가 Worker와 공통 실행 경계

- 변경 파일: `EvaluationJobExecutor.java`, `EvaluationJobProcessingService.java`, `EvaluationJobQueue.java`, `EvaluationJobQueueDispatcher.java`, `EvaluationJobQueueWorker.java`, `EvaluationJobWorker.java`, `WORK_LOG.md`
- 내용: SQS jobId 전달·DB lease·ACK/visibility·재발행 복구를 추가하고 DB fallback과 Queue Worker가 같은 평가·보상 절차를 사용하게 했다. 쿼터 native UPDATE 전 작업 상태를 변경해 detached 상태 유실도 수정했다.
- 검증: Queue 단위 테스트, 4 Worker/40 작업과 중복 전달 통합 테스트 통과
- 리스크: 실제 SQS 장애·Worker 강제 종료와 Azure 장시간 호출은 운영 검증 필요

## 2026-07-29 - 평가 Idempotency 만료 정리 구현

- 변경 파일: `EvaluationJobCleanupService.java`, `EvaluationJobCleanupWorker.java`, `WORK_LOG.md`
- 내용: 완료 후 기본 7일이 지난 성공·실패 작업만 매시간 제한된 batch로 삭제하고 진행 중 작업은 보존한다.
- 검증: 정리 대상·보존 대상 통합 테스트 및 `./gradlew test integrationTest` 통과
- 리스크: 정리 속도가 완료 작업 증가량보다 느린지 운영 모니터링 필요

## 2026-07-27 - DB 기반 음성 평가 Worker 구현

- 변경 파일: `EvaluationAudioStorage.java`, `EvaluationJobCreationService.java`, `EvaluationJobProcessingService.java`, `EvaluationJobService.java`, `EvaluationJobWorker.java`, `EvaluationService.java`, `WORK_LOG.md`
- 내용: 쿼터 예약과 Idempotency 작업 생성, lease claim, S3 음성 평가·재시도·원자적 성공 저장·실패 복구를 구현했다.
- 검증: Service·Worker·전체 Backend 테스트 통과
- 리스크: Azure 호출 timeout은 후속 운영 과제

## 2026-07-24 - 평가·쿼터·결과 저장 유스케이스 통합

- 변경 파일: `EvaluationApplicationService.java`, `EvaluationCompletionService.java`, `EvaluationService.java`, `WORK_LOG.md`
- 내용: 문장 메타데이터 확정, 쿼터 예약, 외부 평가, 결과 저장·예약 확정과 실패 보상을 조율하는 application flow를 추가했다.
- 검증: 핵심 단위 테스트와 Spring/JPA 통합 테스트 통과
- 리스크: 프로세스 비정상 종료 시 장기 예약 회수 정책과 요청 멱등성은 후속 작업

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationHistoryService.java`, `EvaluationPersistenceService.java`, `EvaluationService.java`, `GuideGenerationJobService.java`, `SpeechEvaluator.java`, `VideoGenerator.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
