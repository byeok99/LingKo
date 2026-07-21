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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
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

- 상태: `[x]`
- 변경 파일 후보:
  - backend history controller/service
- 할 일:
  - `GET /api/evaluations/me`
  - pagination
  - 최고 점수/최근 기록 후보
- 검증:
  - controller/service 테스트
- 남은 리스크:
  - `[해결됨: Phase 7 후속]` `GET /api/evaluations/me`의 임시 `userId` query parameter를 제거하고 `Authorization: Bearer <accessToken>` 기준 사용자 ID 해석으로 교체했다.
  - 현재 형태는 내부 MVP 검증용이며, 운영 환경에서는 다른 사용자 기록 조회가 불가능한지 실제 로그인 세션으로 수동 확인이 필요하다.
- 커밋 기준:
  - `feat: expose practice history endpoint`

### 6.4 Flutter 학습 기록 UI 추가

- 상태: `[x]`
- 변경 파일 후보:
  - `app/lib/screens/profile_screen.dart`
  - `app/lib/models/practice_history.dart`
  - `app/lib/api/evaluation_api.dart`
- 할 일:
  - 최근 연습 결과
  - 점수 표시
  - 재연습 진입
- 검증:
  - `cd app && flutter test`
- 남은 리스크:
  - `[해결됨: Phase 7 후속]` 앱 history 조회의 `userId=1` 기본값을 제거하고 로그인 세션 access token 기반 호출로 교체했다.
- 커밋 기준:
  - `feat: add practice history UI`

## Phase 7. 인증과 사용자 설정

목표: 학습 기록과 quota를 사용자 계정에 연결한다. Phase 6.3의 임시 `userId` query parameter 기반 history 조회를 인증 principal 기반 조회로 교체한다.

### 7.1 Google OAuth 우선 구현

- 상태: `[x]`
- 변경 파일 후보:
  - backend auth controller/service
  - security config
  - app auth screen/service
- 할 일:
  - `[x]` Google id token 검증
  - `[x]` 사용자 생성/조회
  - `[x]` JWT access/refresh 발급
  - `[x]` 앱 secure storage
- 검증:
  - `cd backend && ./gradlew test`
  - 실제 로그인 수동 확인
- 남은 리스크:
  - 실제 Google 로그인 수동 확인은 `GOOGLE_CLIENT_ID`, `JWT_SECRET_KEY`, 앱의 `GOOGLE_SERVER_CLIENT_ID` 설정 후 수행해야 한다.
  - history API의 임시 `userId` query parameter는 제거했고 `Authorization: Bearer <accessToken>` 기준으로 사용자 ID를 해석한다.
  - quota API는 Phase 8에서 추가할 때 처음부터 인증 사용자 기준으로 설계해야 한다.
- 커밋 기준:
  - `feat: add Google OAuth login`

### 7.2 Apple Sign in 구현 여부 결정

- 상태: `[x]`
- 변경 파일 후보:
  - `docs/api.md`
  - `docs/priorities.md`
- 결정 필요:
  - `[x]` MVP 동시 출시는 보류한다.
  - `[x]` iOS 배포 준비와 Apple developer 설정이 완료된 뒤 별도 phase로 구현한다.
  - `[x]` Phase 7에서는 Google OAuth + LingKo JWT를 기준 인증 흐름으로 고정한다.
- 검증:
  - 제품/배포 조건 확인
- 남은 리스크:
  - Apple Sign in은 코드 구현 전에 Apple Developer Program 계정, iOS Bundle ID/App ID, Sign in with Apple capability, provisioning profile 갱신이 필요하다.
  - 서버 검증을 위해 Apple Team ID, Client ID 또는 Bundle ID, Key ID, private key 등 환경변수 설계와 비밀값 관리가 필요하다.
  - 웹/서버 OAuth 흐름을 쓰는 경우 Services ID, 도메인, return URL 설정까지 별도 준비해야 한다.
- 커밋 기준:
  - `docs: decide Apple sign in rollout`

### 7.3 사용자 언어/레벨 설정 API

- 상태: `[x]`
- 변경 파일 후보:
  - backend user controller/service/entity
  - app profile screen
- 할 일:
  - `[x]` display language
  - `[x]` native language
  - `[x]` target level
- 검증:
  - `cd backend && ./gradlew test --tests "*UserPreferencesControllerTest" --tests "*UserPreferencesServiceTest"`
  - `cd app && flutter test test/user_preferences_api_test.dart test/widget_test.dart --reporter compact`
- 남은 리스크:
  - 실제 Google 로그인 세션으로 설정 조회/저장 수동 확인이 필요하다.
  - 지원 언어 목록은 현재 MVP 후보(`en`, `ko`, `ja`)로 제한했고, 제품 출시 전 지원 언어 정책 확정이 필요하다.
- 커밋 기준:
  - `feat: add user learning preferences`

## Phase 8. Quota와 광고 보상

목표: 하루 무료 연습 횟수와 광고 보상 정책을 서버 기준으로 관리한다.

### 8.1 Quota 도메인/API 추가

- 상태: `[x]`
- 변경 파일 후보:
  - backend quota entity/service/controller
  - DB migration
- 할 일:
  - `[x]` 하루 5회 무료 정책
  - `[x]` reset 기준 timezone: `Asia/Seoul`
  - `[x]` 사용 차감 service: 무료 quota 우선, 보상 quota 후순위
  - `[x]` `GET /api/quota/today`
- 검증:
  - `cd backend && ./gradlew test --tests "*PracticeQuotaControllerTest" --tests "*PracticeQuotaServiceTest"`
- 남은 리스크:
  - 현재 `POST /api/evaluations`는 내부 프로토타입 호환을 위해 익명 호출을 유지하므로 quota 차감 service를 실제 평가 성공 흐름에 아직 연결하지 않았다.
  - 공개 MVP 전에는 `POST /api/evaluations`를 인증 사용자 기준으로 전환하고 평가 성공 transaction 안에서 `consumePractice`를 호출해야 한다.
  - 광고 보상 quota는 MVP 후순위로 보류한다. 우선은 무료 일일 quota와 로그인 사용자 기준 차감만 닫는다.
- 커밋 기준:
  - `feat: add daily practice quota`

### 8.2 광고 보상 정책 결정

- 상태: `[x]`
- 변경 파일 후보:
  - `docs/api.md`
  - `docs/priorities.md`
- 결정 필요:
  - `[x]` 광고 SDK는 MVP 이후로 보류한다.
  - `[x]` 보상 검증 방식은 광고 기능 착수 시점에 다시 결정한다.
  - `[x]` 부정 사용 방지는 광고 기능과 함께 별도 설계한다.
- 검증:
  - 제품 우선순위 결정
- 남은 리스크:
  - 현재 quota 도메인에는 보상 quota 필드가 있지만, 광고 SDK/검증/지급 API는 구현하지 않는다.
  - 8.3 Flutter quota UI에서는 광고 보상 진입점을 노출하지 않는다.
- 커밋 기준:
  - `docs: define ad reward quota policy`

### 8.3 Flutter quota UI 연결

- 상태: `[x]`
- 변경 파일:
  - `app/lib/models/practice_quota.dart`
  - `app/lib/api/practice_quota_api.dart`
  - `app/lib/screens/home_screen.dart`
  - `app/lib/app/lingko_app.dart`
  - `app/lib/screens/practice_screen.dart`
  - `app/lib/screens/profile_screen.dart`
  - `app/test/practice_quota_api_test.dart`
  - `app/test/widget_test.dart`
- 할 일:
  - `[x]` 남은 연습 횟수 API 표시
  - `[x]` quota 초과 시 녹음 버튼 제한
  - `[보류]` 광고 보상 진입점
- 검증:
  - `cd app && flutter test --reporter compact`
  - `cd app && flutter analyze`
- 남은 리스크:
  - 실제 로그인 세션과 백엔드 `GET /api/quota/today` 연동은 emulator/device에서 수동 확인이 필요하다.
  - 현재 백엔드 평가 API는 내부 프로토타입 호환으로 quota 차감과 아직 완전히 연결되지 않았으므로, 평가 성공 후 남은 횟수 갱신은 백엔드 차감 연결 이후 최종 확인해야 한다.
- 커밋 기준:
  - `feat: connect practice quota UI`

## Phase 9. 조음 가이드 고도화

목표: 글자별 입/혀 가이드를 실제 asset 기반으로 제공한다.

### 9.1 MVP 정적 asset 우선 정책 적용

- 상태: `[x]`
- 변경 파일 후보:
  - `docs/api.md`
  - `SyllableMappingUtil`
  - Flutter guide sheet
- 할 일:
  - `[x]` 실시간 영상 생성은 MVP 요청 경로에서 제외
  - `[x]` 정적 image/video URL 우선 반환
  - `[x]` asset 누락 fallback 정의
- 검증:
  - `cd backend && ./gradlew test --tests "*SyllableMappingUtilTest"`
  - `cd app && flutter test test/widget_test.dart --plain-name "guide sheet renders available static guide assets first" --reporter compact`
  - 앱 이미지 로딩 수동 확인
- 남은 리스크:
  - 실제 S3 asset 접근성과 이미지 로딩은 emulator/device에서 수동 확인이 필요하다.
  - 비동기 영상 생성 job은 9.2에서 별도 구현한다.
- 커밋 기준:
  - `feat: serve static pronunciation guide assets`

### 9.2 비동기 영상 생성 job 분리

- 상태: `[x]`
- 변경 파일 후보:
  - backend job/service
  - `docs/api.md`
- 할 일:
  - `[x]` job 상태 `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`
  - `[x]` polling API
  - `[x]` timeout/retry 정책
  - `[x]` 캐시 키 정책
- 검증:
  - `cd backend && ./gradlew test --tests "com.lingko.lingko.core.domain.evaluation.GuideGenerationJobServiceTest" --tests "com.lingko.lingko.api.evaluation.GuideGenerationJobControllerTest"`
- 남은 리스크:
  - 현재 job 저장소는 MVP용 in-memory 방식이라 서버 재시작 시 job 상태가 사라진다.
  - 공개 운영 전에는 관리자 인증/인가, 영속 job store, retry/backoff worker를 별도 보강해야 한다.
- 커밋 기준:
  - `feat: add async guide generation job`

## Phase 10. 품질 게이트와 운영 준비

목표: 테스트/검증/운영 리스크를 반복적으로 줄인다.

### 10.1 백엔드 controller validation 테스트 추가

- 상태: `[x]`
- 변경 파일 후보:
  - backend controller test
- 할 일:
  - `[x]` blank text
  - `[x]` 30자 초과
  - `[x]` malformed JSON
  - `[x]` 공통 에러 응답
- 검증:
  - `cd backend && ./gradlew test --tests "com.lingko.lingko.api.evaluation.EvaluationControllerPrepareTest" --tests "com.lingko.lingko.api.evaluation.EvaluationResultControllerTest"`
- 커밋 기준:
  - `test: cover pronunciation API validation`

### 10.2 앱 실패 상태 테스트 추가

- 상태: `[x]`
- 변경 파일 후보:
  - `app/test/...`
- 할 일:
  - `[x]` API 로딩
  - `[x]` API 실패
  - `[x]` 입력 길이 초과
  - `[x]` quota 초과
  - `[x]` 녹음 권한 거부
- 검증:
  - `cd app && flutter test`
- 커밋 기준:
  - `test: cover practice failure states`

### 10.3 통합 테스트 프로파일 정리

- 상태: `[x]`
- 변경 파일 후보:
  - `backend/build.gradle`
  - integration test 설정
  - docs
- 할 일:
  - `[x]` 외부 서비스가 필요한 테스트를 명확히 분리
  - `[x]` 필요한 환경변수 문서화
  - `[x]` CI에서 기본 제외 여부 결정
- 검증:
  - `cd backend && ./gradlew test compileIntegrationTest`
  - `cd backend && ./gradlew integrationTest`
  - `cd backend && ./gradlew externalIntegrationTest --dry-run`
- 커밋 기준:
  - `test: separate external integration tests`

### 10.4 Gradle deprecation 경고 확인

- 상태: `[x]`
- 변경 파일 후보:
  - `backend/build.gradle`
  - Gradle wrapper 관련 파일
- 할 일:
  - `[x]` `--warning-mode all` 실행
  - `[x]` Gradle 9.0 비호환 경고 원인 정리
- 검증:
  - `cd backend && ./gradlew test --warning-mode all`
- 커밋 기준:
  - `chore: resolve Gradle deprecation warnings`

### 10.5 조음 가이드 운영 리스크 보강

- 상태: `[ ]`
- 변경 파일 후보:
  - backend guide job persistence/worker
  - backend security config
  - app guide sheet manual QA notes
  - `docs/api.md`
- 할 일:
  - 실제 S3 guide asset 접근성과 앱 이미지 로딩을 emulator/device에서 수동 확인
  - guide job 저장소를 in-memory에서 영속 저장소로 교체
  - guide job 생성/polling API에 관리자 인증/인가 적용
  - 영상 생성 실패 retry/backoff worker 정책 구현
- 검증:
  - 실제 S3 asset URL 로딩 수동 확인
  - guide job persistence service test
  - guide job authorization controller test
- 커밋 기준:
  - `feat: harden pronunciation guide jobs`

## 보류 항목

아래 작업은 첫 수직 흐름과 실제 평가 흐름이 안정화된 뒤 진행한다.

- Course 탭 재도입
- 고급 코스 에디터
- 교사 대시보드
- 커뮤니티 기능
- 정교한 게임화
- 3개 이상 표시 언어 동시 지원
