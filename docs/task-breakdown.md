# LingKo Task Breakdown

작성 기준: 2026-06-16

이 문서는 LingKo의 다음 작업을 실제 커밋 단위로 추적하기 위한 목록이다. 현재 기준 문서는 [research.md](research.md), [api.md](api.md), [database.md](database.md), [priorities.md](priorities.md)다.

2026-06-16 아키텍처/코드 리뷰 결과, 지금 가장 중요한 판단은 다음과 같다.

- MVP 필수 흐름인 인증, 녹음 평가, 결과 저장이 아직 하나의 수직 흐름으로 연결되어 있지 않다.
- 현재 백엔드 공개 API는 `POST /api/pronunciation/convert` 중심이다.
- Flutter 앱은 mock 데이터 기반 프로토타입이며 API client, 녹음, 오디오, OAuth 의존성이 없다.
- 평가 저장 모델, 조음 guide URL 흐름, 외부 URL 다운로드, Replicate key 로그, 공통 에러 응답, DB migration이 우선 보강 대상이다.

## 상태 표기

- `[ ]`: 시작 전
- `[~]`: 진행 중
- `[x]`: 완료
- `[!]`: 막힘 또는 의사결정 필요

## 커밋 운영 규칙

커밋은 작은 단위로 나눈다.

예:

```text
docs: align task breakdown with architecture review
fix: keep evaluation syllables in aggregate
fix: stop logging replicate api key prefix
feat: add standard API error response
feat: add pronunciation prepare endpoint
feat: connect custom practice input to API
test: cover pronunciation validation errors
```

각 작업 완료 보고에는 아래를 남긴다.

```text
브랜치:
커밋:
변경 파일:
검증:
남은 이슈:
다음 작업:
```

## Phase 0. 리뷰 반영 문서 정리

목표: 후속 작업자가 현재 우선순위와 리스크를 문서만 보고 이해할 수 있게 한다.

### 0.1 task breakdown 재정렬

- 상태: `[x]`
- 변경 파일:
  - `docs/task-breakdown.md`
- 할 일:
  - 오래된 Phase 정리
  - 리뷰 Findings를 작업 단위로 변환
  - Critical/High 리스크를 앞단으로 이동
- 검증:
  - `git diff --check -- docs/task-breakdown.md`
- 커밋 기준:
  - `docs: align task breakdown with architecture review`

### 0.2 research 경로 표기 수정

- 상태: `[x]`
- 변경 파일:
  - `docs/research.md`
- 할 일:
  - 저장소 구조 예시에서 루트 `research.md` 표기를 `docs/research.md`로 수정
  - 테스트 실행 메타데이터 보강 여부 결정
- 검증:
  - 문서 링크와 경로 확인
- 커밋 기준:
  - `docs: fix research document path`

## Phase 1. API 계약과 공통 응답 확정

목표: 앱과 백엔드가 같은 요청/응답/에러 언어를 쓰게 만든다.

### 1.1 MVP API 계약 문서화

- 상태: `[ ]`
- 변경 파일:
  - `docs/api.md`
- 할 일:
  - `POST /api/pronunciation/prepare`
  - `POST /api/evaluations`
  - `GET /api/evaluations/me`
  - `POST /api/auth/oauth/login`
  - `GET /api/quota/today`
  - 인증 필요 여부 명시
  - 상태 코드 명시
- 검증:
  - 앱 모델과 백엔드 DTO 후보가 같은 필드명을 쓰는지 확인
- 커밋 기준:
  - `docs: define MVP API contracts`

### 1.2 공통 성공/실패 응답 형식 확정

- 상태: `[ ]`
- 변경 파일:
  - `docs/api.md`
  - 백엔드 공통 response/error 패키지 후보
- 할 일:
  - 성공 envelope 사용 여부 결정
  - validation error 형식 정의
  - 인증 실패 `401`, 권한 실패 `403`, quota 초과 `429` 정의
  - 외부 API 실패 응답 정의
- 검증:
  - Flutter에서 로딩/실패/재시도 UX를 구현할 수 있는지 확인
- 커밋 기준:
  - `docs: define API error response format`

### 1.3 기준 문장 정책 결정

- 상태: `[!]`
- 변경 파일:
  - `docs/api.md`
  - `docs/research.md`
- 결정 필요:
  - Azure 평가 기준을 원문으로 할지 표준 발음으로 할지
  - 앱 화면에는 원문/표준 발음/인식 결과를 어떻게 함께 보여줄지
- 검증:
  - `맛있겠다`, `밥을 먹었어요`, `같이` 같은 샘플로 평가 기준을 설명할 수 있는지 확인
- 커밋 기준:
  - `docs: decide pronunciation evaluation reference text`

## Phase 2. 백엔드 안전장치와 저장 모델 보강

목표: 이후 기능을 붙이기 전에 저장/보안/운영 리스크를 줄인다.

### 2.1 `EvaluationLog.addSyllable()` 수정

- 상태: `[ ]`
- 변경 파일:
  - `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/entity/EvaluationLog.java`
  - 관련 테스트
- 할 일:
  - `syllableList.add(syllable)` 추가
  - null 방어 여부 결정
  - 양방향 연관관계 일관성 테스트 추가
- 검증:
  - `cd backend && ./gradlew test`
- 커밋 기준:
  - `fix: keep evaluation syllables in aggregate`

### 2.2 Replicate API key 로그 제거

- 상태: `[ ]`
- 변경 파일:
  - `backend/src/main/java/com/lingko/lingko/infra/pronunciation/ReplicateApiClient.java`
- 할 일:
  - API key prefix 로그 제거
  - 설정 누락 시 명확한 예외 처리
  - key 길이 짧을 때 `substring` 예외 제거
- 검증:
  - 단위 테스트 또는 설정 누락 테스트
  - `cd backend && ./gradlew test`
- 커밋 기준:
  - `fix: stop logging replicate api key prefix`

### 2.3 외부 URL 다운로드 방어 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/infra/storage/S3Uploader.java`
  - `backend/src/main/java/com/lingko/lingko/infra/pronunciation/FrameInterpolationVideoGenerator.java`
  - 신규 URL validator
- 할 일:
  - allowlist 도메인 정책 정의
  - connection/read timeout 설정
  - 최대 다운로드 크기 제한
  - 내부망/localhost/private IP 차단
- 검증:
  - validator 단위 테스트
  - 잘못된 URL 거부 테스트
- 커밋 기준:
  - `fix: validate external media download URLs`

### 2.4 조음 guide URL 생성 흐름 통일

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/core/util/SyllableMappingUtil.java`
  - `backend/src/main/java/com/lingko/lingko/core/util/VisemeExtractorUtil.java`
  - `backend/src/main/java/com/lingko/lingko/infra/pronunciation/FrameInterpolationVideoGenerator.java`
- 할 일:
  - `VisemeExtractorUtil`이 파일명이 아닌 절대 URL을 반환할지 결정
  - `SyllableMappingUtil.getImageUrl()` 재사용
  - `FrameInterpolationVideoGenerator.validateUrl()`와 입력 계약 일치
- 검증:
  - `VisemeExtractorUtilTest`
  - `SyllableMappingUtilTest`
  - `cd backend && ./gradlew test`
- 커밋 기준:
  - `fix: normalize pronunciation guide asset URLs`

### 2.5 DB migration 도구 도입

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/build.gradle`
  - `backend/src/main/resources/db/migration/...`
  - `docs/database.md`
- 할 일:
  - Flyway 또는 Liquibase 선택
  - 현재 JPA 엔티티 기준 초기 schema 작성
  - `ddl-auto=none` 유지 전제 정리
- 검증:
  - 로컬 MySQL 또는 H2 migration 실행 확인
  - `cd backend && ./gradlew test`
- 커밋 기준:
  - `feat: add database migration baseline`

### 2.6 공통 예외 처리 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/api/common/...`
  - `backend/src/main/java/com/lingko/lingko/api/.../GlobalExceptionHandler.java`
- 할 일:
  - validation error 응답
  - domain error 응답
  - 외부 연동 실패 응답
  - request id 또는 error code 정책
- 검증:
  - controller test
  - validation 실패 테스트
- 커밋 기준:
  - `feat: add global API exception handling`

## Phase 3. 첫 번째 수직 흐름: 직접 입력 표준 발음 표시

목표: 사용자가 앱에서 직접 입력한 문장을 서버 표준 발음으로 확인한다.

### 3.1 prepare DTO 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/dto/PronunciationPrepareRequest.java`
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/dto/PronunciationPrepareResponse.java`
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/dto/GuideCharacterResponse.java`
- 할 일:
  - 원문
  - 표준 발음
  - 글자/음절 단위 guide item
  - 입력 길이 정책
- 검증:
  - DTO validation 테스트
- 커밋 기준:
  - `feat: add pronunciation prepare DTOs`

### 3.2 prepare service/API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/EvaluationController.java`
  - `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/EvaluationService.java`
- 할 일:
  - `POST /api/pronunciation/prepare`
  - 표준 발음 변환
  - guide item 생성
  - 공통 에러 응답 적용
- 검증:
  - service test
  - controller test
  - curl 수동 확인
- 커밋 기준:
  - `feat: expose pronunciation prepare endpoint`

### 3.3 Flutter API client 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/pubspec.yaml`
  - `app/lib/api/api_client.dart`
  - `app/lib/api/pronunciation_api.dart`
- 할 일:
  - HTTP 패키지 선택
  - base URL 설정
  - emulator localhost 정책
  - timeout/error mapping
- 검증:
  - `cd app && flutter analyze`
  - `cd app && flutter test`
- 커밋 기준:
  - `feat: add Flutter pronunciation API client`

### 3.4 Practice 직접 입력 API 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/app/lingko_app.dart`
  - `app/lib/screens/practice_screen.dart`
  - `app/lib/models/practice_sentence.dart`
- 할 일:
  - `PracticeSentence.custom()` 더미 의존 제거 또는 분리
  - 입력 제출 시 API 호출
  - loading/success/error 상태
  - 30자 제한과 서버 validation 메시지 반영
- 검증:
  - widget test
  - emulator 수동 확인
- 커밋 기준:
  - `feat: connect custom practice input to API`

## Phase 4. 추천 문장 서버화

목표: 홈 추천 문장을 mock 데이터가 아닌 서버 콘텐츠로 제공한다.

### 4.1 추천 문장 schema/seed 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - DB migration 파일
  - `docs/database.md`
- 할 일:
  - MVP 문장 20개 이상 선정
  - 영어 번역
  - 표준 발음
  - level/category/learning point
- 검증:
  - migration 적용 확인
  - seed 조회 확인
- 커밋 기준:
  - `data: add MVP recommended sentence seed`

### 4.2 추천 문장 조회 API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend controller/service/repository
- 할 일:
  - `GET /api/sentences/recommended`
  - locale/display language 파라미터
  - active 문장만 반환
- 검증:
  - controller/service 테스트
- 커밋 기준:
  - `feat: expose recommended sentences endpoint`

### 4.3 Flutter Home API 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/home_screen.dart`
  - `app/lib/data/mock_sentences.dart`
  - `app/lib/api/...`
- 할 일:
  - 홈 진입 시 추천 문장 로딩
  - loading/error/empty 상태
  - mock fallback 제거 여부 결정
- 검증:
  - `cd app && flutter test`
  - emulator 수동 확인
- 커밋 기준:
  - `feat: load recommended sentences from API`

## Phase 5. 녹음과 Azure 발음 평가

목표: 실제 녹음 파일을 서버로 보내고 실제 점수를 받는다.

### 5.1 Flutter 녹음/오디오 패키지 도입

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/pubspec.yaml`
  - `app/android/app/src/main/AndroidManifest.xml`
  - `app/ios/Runner/Info.plist`
- 할 일:
  - 녹음 패키지 선택
  - 오디오 재생 패키지 선택
  - Android/iOS 권한 추가
  - 권한 거부 UX
- 검증:
  - Android emulator 수동 확인
  - iOS build 또는 simulator 확인
- 커밋 기준:
  - `feat: add recording and playback dependencies`

### 5.2 녹음 UI 상태 구현

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/practice_screen.dart`
  - `app/lib/services/recording_service.dart`
- 할 일:
  - 녹음 전
  - 녹음 중
  - 녹음 완료
  - 재녹음
  - 업로드 중
  - 실패 재시도
- 검증:
  - widget test
  - emulator 수동 확인
- 커밋 기준:
  - `feat: add practice recording states`

### 5.3 평가 API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend evaluation controller/service/DTO
- 할 일:
  - `POST /api/evaluations`
  - multipart upload
  - reference text 정책 반영
  - `AzureSpeechEvaluator` 호출
  - score DTO 반환
  - 외부 연동 실패 처리
- 검증:
  - mock `SpeechEvaluator` 기반 controller/service 테스트
  - 실제 Azure 통합 테스트는 별도 프로파일로 분리
- 커밋 기준:
  - `feat: expose pronunciation evaluation endpoint`

### 5.4 앱 Result 화면 실제 결과 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/models/practice_result.dart`
  - `app/lib/screens/result_screen.dart`
  - `app/lib/app/lingko_app.dart`
- 할 일:
  - 더미 score breakdown 제거
  - API 결과 기반 점수/취약 글자 표시
  - 평가 실패 상태 처리
- 검증:
  - `cd app && flutter test`
- 커밋 기준:
  - `feat: render pronunciation evaluation result`

## Phase 6. 결과 저장과 학습 기록

목표: 사용자별 평가 결과를 저장하고 다시 볼 수 있게 한다.

### 6.1 평가 저장 모델 확장

- 상태: `[ ]`
- 변경 파일 후보:
  - `EvaluationLog`
  - `EvaluationSyllable`
  - DB migration
  - `docs/database.md`
- 할 일:
  - `standardPronunciation`
  - `recognizedText`
  - `accuracy/fluency/completeness/pronunciationScore`
  - `audioUrl`
  - 추천/직접 입력 source
  - 글자별 feedback/position/guide URL
- 검증:
  - JPA repository test
  - migration test
- 커밋 기준:
  - `feat: extend evaluation persistence model`

### 6.2 평가 결과 저장 서비스 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend service/repository
- 할 일:
  - 평가 결과 저장 transaction
  - 글자별 결과 cascade 확인
  - 저장 실패 rollback 확인
- 검증:
  - service integration test
- 커밋 기준:
  - `feat: persist pronunciation evaluation results`

### 6.3 학습 기록 API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend history controller/service
- 할 일:
  - `GET /api/evaluations/me`
  - pagination
  - 최고 점수/최근 기록 후보
- 검증:
  - controller/service 테스트
- 커밋 기준:
  - `feat: expose practice history endpoint`

### 6.4 Flutter 학습 기록 UI 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/profile_screen.dart`
  - 신규 history screen/widget
- 할 일:
  - 최근 연습 결과
  - 점수 표시
  - 재연습 진입
- 검증:
  - widget test
- 커밋 기준:
  - `feat: add practice history UI`

## Phase 7. 인증과 사용자 설정

목표: 학습 기록과 quota를 사용자 계정에 연결한다.

### 7.1 Google OAuth 우선 구현

- 상태: `[ ]`
- 변경 파일 후보:
  - backend auth controller/service
  - security config
  - app auth screen/service
- 할 일:
  - Google id token 검증
  - 사용자 생성/조회
  - JWT access/refresh 발급
  - 앱 secure storage
- 검증:
  - auth service test
  - 실제 로그인 수동 확인
- 커밋 기준:
  - `feat: add Google OAuth login`

### 7.2 Apple Sign in 구현 여부 결정

- 상태: `[!]`
- 변경 파일 후보:
  - `docs/api.md`
  - `docs/priorities.md`
- 결정 필요:
  - MVP 동시 출시 여부
  - iOS 배포 준비 상태
  - Apple developer 설정
- 검증:
  - 제품/배포 조건 확인
- 커밋 기준:
  - `docs: decide Apple sign in rollout`

### 7.3 사용자 언어/레벨 설정 API

- 상태: `[ ]`
- 변경 파일 후보:
  - backend user controller/service/entity
  - app profile screen
- 할 일:
  - display language
  - native language
  - target level
- 검증:
  - API test
  - widget test
- 커밋 기준:
  - `feat: add user learning preferences`

## Phase 8. Quota와 광고 보상

목표: 하루 무료 연습 횟수와 광고 보상 정책을 서버 기준으로 관리한다.

### 8.1 Quota 도메인/API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend quota entity/service/controller
  - DB migration
- 할 일:
  - 하루 5회 무료 정책
  - reset 기준 timezone
  - 사용 차감 시점
  - `GET /api/quota/today`
- 검증:
  - quota service test
- 커밋 기준:
  - `feat: add daily practice quota`

### 8.2 광고 보상 정책 결정

- 상태: `[!]`
- 변경 파일 후보:
  - `docs/api.md`
  - `docs/priorities.md`
- 결정 필요:
  - 광고 SDK
  - 보상 검증 방식
  - 부정 사용 방지
- 검증:
  - 플랫폼 정책 확인
- 커밋 기준:
  - `docs: define ad reward quota policy`

### 8.3 Flutter quota UI 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/home_screen.dart`
  - `app/lib/app/lingko_app.dart`
- 할 일:
  - 남은 연습 횟수 API 표시
  - quota 초과 시 녹음 버튼 제한
  - 광고 보상 진입점
- 검증:
  - widget test
  - 수동 확인
- 커밋 기준:
  - `feat: connect practice quota UI`

## Phase 9. 조음 가이드 고도화

목표: 글자별 입/혀 가이드를 실제 asset 기반으로 제공한다.

### 9.1 MVP 정적 asset 우선 정책 적용

- 상태: `[ ]`
- 변경 파일 후보:
  - `docs/api.md`
  - `SyllableMappingUtil`
  - Flutter guide sheet
- 할 일:
  - 실시간 영상 생성은 MVP 요청 경로에서 제외
  - 정적 image/video URL 우선 반환
  - asset 누락 fallback 정의
- 검증:
  - guide URL 테스트
  - 앱 이미지 로딩 수동 확인
- 커밋 기준:
  - `feat: serve static pronunciation guide assets`

### 9.2 비동기 영상 생성 job 분리

- 상태: `[ ]`
- 변경 파일 후보:
  - backend job/service
  - `docs/api.md`
- 할 일:
  - job 상태 `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`
  - polling API
  - timeout/retry 정책
  - 캐시 키 정책
- 검증:
  - service test
- 커밋 기준:
  - `feat: add async guide generation job`

## Phase 10. 품질 게이트와 운영 준비

목표: 테스트/검증/운영 리스크를 반복적으로 줄인다.

### 10.1 백엔드 controller validation 테스트 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend controller test
- 할 일:
  - blank text
  - 30자 초과
  - malformed JSON
  - 공통 에러 응답
- 검증:
  - `cd backend && ./gradlew test`
- 커밋 기준:
  - `test: cover pronunciation API validation`

### 10.2 앱 실패 상태 테스트 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/test/...`
- 할 일:
  - API 로딩
  - API 실패
  - 입력 길이 초과
  - quota 초과
  - 녹음 권한 거부
- 검증:
  - `cd app && flutter test`
- 커밋 기준:
  - `test: cover practice failure states`

### 10.3 통합 테스트 프로파일 정리

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/build.gradle`
  - integration test 설정
  - docs
- 할 일:
  - 외부 서비스가 필요한 테스트를 명확히 분리
  - 필요한 환경변수 문서화
  - CI에서 기본 제외 여부 결정
- 검증:
  - `cd backend && ./gradlew test compileIntegrationTest`
  - 필요 시 `./gradlew integrationTest`
- 커밋 기준:
  - `test: separate external integration tests`

### 10.4 Gradle deprecation 경고 확인

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/build.gradle`
  - Gradle wrapper 관련 파일
- 할 일:
  - `--warning-mode all` 실행
  - Gradle 9.0 비호환 경고 원인 정리
- 검증:
  - `cd backend && ./gradlew test --warning-mode all`
- 커밋 기준:
  - `chore: resolve Gradle deprecation warnings`

## 보류 항목

아래 작업은 첫 수직 흐름과 실제 평가 흐름이 안정화된 뒤 진행한다.

- Course 탭 재도입
- 고급 코스 에디터
- 교사 대시보드
- 커뮤니티 기능
- 정교한 게임화
- 3개 이상 표시 언어 동시 지원
