# LingKo Task Breakdown

작성 기준: 2026-05-27

이 문서는 큰 우선순위를 실제 커밋 단위로 추적하기 위한 작업 목록이다. 우선순위 요약은 [priorities.md](priorities.md)를 기준으로 본다.

각 작업은 가능하면 아래 기준을 만족해야 한다.

- 한 커밋에서 하나의 의도를 가진다.
- 변경 파일 범위가 작다.
- 검증 방법이 명확하다.
- 다음 작업의 기반이 된다.

## 상태 표기

- `[ ]`: 시작 전
- `[~]`: 진행 중
- `[x]`: 완료
- `[!]`: 막힘 또는 의사결정 필요

## 브랜치/커밋 운영 규칙

브랜치는 기능 축 단위로 나눈다.

예:

```text
chore/backend-config-cleanup
fix/backend-tests
feat/pronunciation-prepare-api
feat/app-pronunciation-api
feat/app-recording-evaluation
```

커밋은 작업 단위로 작게 만든다.

예:

```text
chore: align backend configuration keys
fix: remove obsolete pronunciation demo tests
feat: add pronunciation prepare endpoint
feat: connect practice input to pronunciation API
test: cover custom sentence prepare flow
docs: update MVP task breakdown
```

## Phase 0. 현재 문서와 앱 프로토타입 정리

목표: 지금까지 만든 앱 구조와 문서를 추적 가능한 상태로 만든다.

### 0.2 작업 및 우선순위 추적 문서 추가

- 상태: `[~]`
- 변경 파일:
  - `docs/priorities.md`
  - `docs/task-breakdown.md`
  - `docs/research.md`
- 할 일:
  - 우선순위 목록 작성
  - 세부 작업 목록 작성
  - research 문서에서 priorities/task breakdown 링크
- 검증:
  - 다음 작업을 이 문서만 보고 시작할 수 있는지 확인
- 커밋 기준:
  - `docs: add granular project priorities`

## Phase 1. 보안과 설정 정리

목표: 백엔드를 안전하게 실행할 수 있는 설정 기반을 만든다.

## Phase 2. 백엔드 테스트 복구

목표: 외부 API 키 없이도 기본 테스트가 통과하게 만든다.

## Phase 3. 앱-백엔드 API 계약 정의

목표: Flutter 모델과 백엔드 DTO가 같은 언어를 쓰게 만든다.

### 3.3 비동기 가이드 생성 정책 문서화

- 상태: `[ ]`
- 변경 파일 후보:
  - `docs/api.md`
  - `docs/research.md`
- 할 일:
  - `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` 상태 정의
  - polling API 정의
  - MVP에서는 정적 이미지 우선 사용 명시
- 검증:
  - 사용자가 재녹음 없이 같은 session id로 결과를 조회할 수 있는지 확인
- 커밋 기준:
  - `docs: define async guide generation flow`

## Phase 4. 표준 발음 API 확장

목표: 앱의 직접 입력 문장이 실제 백엔드 표준 발음으로 바뀌게 한다.

### 4.1 prepare DTO 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/dto/...`
- 할 일:
  - `PronunciationPrepareRequest`
  - `PronunciationPrepareResponse`
  - `GuideCharacterResponse`
- 검증:
  - DTO validation 테스트 또는 controller 테스트
- 커밋 기준:
  - `feat: add pronunciation prepare DTOs`

### 4.2 prepare 서비스 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/core/domain/evaluation/service/EvaluationService.java`
  - 필요 시 신규 service
- 할 일:
  - 원문 입력
  - 표준 발음 변환
  - 글자별 guide item 생성
  - 추천/직접 입력 공통 응답 구성
- 검증:
  - 단위 테스트
- 커밋 기준:
  - `feat: prepare pronunciation practice data`

### 4.3 prepare API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/EvaluationController.java`
- 할 일:
  - `POST /api/pronunciation/prepare`
  - request validation
  - error response 확인
- 검증:
  - controller 테스트
  - curl 또는 HTTP client로 수동 확인
- 커밋 기준:
  - `feat: expose pronunciation prepare endpoint`

## Phase 5. Flutter 앱 API 연결

목표: 더미 `Custom sentence`를 실제 서버 응답으로 교체한다.

### 5.1 Flutter HTTP client 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/pubspec.yaml`
  - `app/lib/api/api_client.dart`
- 할 일:
  - HTTP 패키지 선택
  - base URL 설정
  - Android emulator의 localhost 주소 처리
- 검증:
  - `flutter analyze`
  - `flutter test`
- 커밋 기준:
  - `feat: add Flutter API client`

### 5.2 pronunciation API client 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/api/pronunciation_api.dart`
  - `app/lib/models/practice_sentence.dart`
- 할 일:
  - prepare request
  - response parsing
  - error handling
- 검증:
  - unit/widget test
- 커밋 기준:
  - `feat: add pronunciation API client`

### 5.3 Practice 입력 제출 API 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/practice_screen.dart`
  - `app/lib/app/lingko_app.dart`
- 할 일:
  - 입력 제출 시 API 호출
  - loading 상태
  - 실패 메시지
  - 성공 시 `selectedSentence` 교체
- 검증:
  - `flutter test`
  - 에뮬레이터 수동 확인
- 커밋 기준:
  - `feat: connect custom practice input to API`

### 5.4 추천 문장 선택 prepare API 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/home_screen.dart`
  - `app/lib/app/lingko_app.dart`
- 할 일:
  - 추천 카드 선택 시 서버 prepare 호출
  - 실패 시 기존 더미 fallback 여부 결정
- 검증:
  - `flutter test`
  - 에뮬레이터 수동 확인
- 커밋 기준:
  - `feat: prepare recommended sentence via API`

## Phase 6. 녹음과 발음 평가

목표: 실제 녹음 파일을 서버로 보내고 실제 점수를 받는다.

### 6.1 Flutter 녹음 패키지 도입

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/pubspec.yaml`
  - `app/android/app/src/main/AndroidManifest.xml`
  - `app/ios/Runner/Info.plist`
- 할 일:
  - 녹음 패키지 선택
  - Android/iOS 마이크 권한 추가
  - 권한 요청 UX 정리
- 검증:
  - Android emulator 수동 확인
  - iOS simulator 또는 build 확인
- 커밋 기준:
  - `feat: add recording permissions and package`

### 6.2 녹음 UI 상태 구현

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/practice_screen.dart`
  - 필요 시 `app/lib/services/recording_service.dart`
- 할 일:
  - 녹음 전
  - 녹음 중
  - 녹음 완료
  - 재녹음
- 검증:
  - widget test
  - emulator 수동 확인
- 커밋 기준:
  - `feat: add practice recording states`

### 6.3 백엔드 evaluate API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/java/com/lingko/lingko/api/evaluation/EvaluationController.java`
  - DTO/service 파일
- 할 일:
  - multipart upload
  - 기준 문장 전달
  - `AzureSpeechEvaluator` 호출
  - 결과 DTO 반환
- 검증:
  - mock 기반 service/controller 테스트
- 커밋 기준:
  - `feat: expose pronunciation evaluation endpoint`

### 6.4 앱 Result 화면 실제 결과 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/models/practice_result.dart`
  - `app/lib/screens/result_screen.dart`
  - `app/lib/app/lingko_app.dart`
- 할 일:
  - `PracticeResult` 모델 추가
  - 더미 score breakdown 제거
  - API 결과 기반 UI 표시
- 검증:
  - `flutter test`
- 커밋 기준:
  - `feat: render pronunciation evaluation result`

## Phase 7. 추천 문장 서버화

목표: 홈 추천 문장을 앱 더미 데이터가 아니라 서버 콘텐츠로 제공한다.

### 7.1 추천 문장 seed 정의

- 상태: `[ ]`
- 변경 파일 후보:
  - `backend/src/main/resources/...`
  - 또는 DB migration 파일
- 할 일:
  - MVP 문장 20개 선정
  - 영어 번역
  - 학습 포인트
  - 난이도/카테고리
- 검증:
  - seed 로딩 확인
- 커밋 기준:
  - `data: add MVP recommended sentence seed`

### 7.2 추천 문장 조회 API 추가

- 상태: `[ ]`
- 변경 파일 후보:
  - backend controller/service/repository
- 할 일:
  - `GET /api/sentences/recommended`
  - locale 파라미터 고려
  - active 문장만 반환
- 검증:
  - controller/service 테스트
- 커밋 기준:
  - `feat: expose recommended sentences endpoint`

### 7.3 Flutter Home API 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `app/lib/screens/home_screen.dart`
  - `app/lib/api/...`
  - `app/lib/data/mock_sentences.dart`
- 할 일:
  - 홈 진입 시 추천 문장 로딩
  - 로딩/에러/빈 상태
  - 더미 데이터 fallback 여부 결정
- 검증:
  - `flutter test`
  - emulator 수동 확인
- 커밋 기준:
  - `feat: load recommended sentences from API`

## Phase 8. 결과 저장과 가이드

목표: 연습 결과를 다시 볼 수 있고, 가이드 자산을 실제로 제공한다.

### 8.1 PracticeSession 저장 모델 정리

- 상태: `[ ]`
- 변경 파일 후보:
  - backend entity/repository/service
  - `docs/database.md`
- 할 일:
  - 기존 `EvaluationLog` 유지/개명 결정
  - 자유 입력과 추천 문장 모두 저장 가능하게 설계
- 검증:
  - JPA repository test
- 커밋 기준:
  - `feat: add practice session persistence`

### 8.2 글자별 결과 저장

- 상태: `[ ]`
- 변경 파일 후보:
  - backend entity/repository/service
- 할 일:
  - 점수
  - guide URLs
  - 피드백 코드
- 검증:
  - JPA repository test
- 커밋 기준:
  - `feat: persist practice character results`

### 8.3 정적 가이드 자산 연결

- 상태: `[ ]`
- 변경 파일 후보:
  - `SyllableMappingUtil`
  - result DTO
  - Flutter guide sheet
- 할 일:
  - URL 계약 통일
  - 앱 이미지 로딩
  - 로딩 실패 fallback
- 검증:
  - 단위 테스트
  - emulator 수동 확인
- 커밋 기준:
  - `feat: show pronunciation guide assets`

### 8.4 비동기 비디오 생성 job 설계

- 상태: `[ ]`
- 변경 파일 후보:
  - backend service/job
  - `docs/api.md`
- 할 일:
  - job 상태 정의
  - polling API
  - 실패 재시도 정책
- 검증:
  - service test
- 커밋 기준:
  - `feat: add async guide generation job`

## Phase 9. 로그인, 기록, 광고

목표: 실제 제품 운영 기능을 붙인다.

### 9.1 OAuth 로그인

- 상태: `[ ]`
- 변경 파일 후보:
  - backend auth controller/service
  - app auth screen/service
- 할 일:
  - Google 우선
  - Apple 후속
  - JWT 발급
- 검증:
  - auth flow 수동 확인
- 커밋 기준:
  - `feat: add Google OAuth login`

### 9.2 학습 기록 화면

- 상태: `[ ]`
- 변경 파일 후보:
  - app profile/history screens
  - backend practice history API
- 할 일:
  - 최근 연습 결과
  - 최고 점수
  - 재연습 진입
- 검증:
  - widget/API 테스트
- 커밋 기준:
  - `feat: add practice history`

### 9.3 무료 횟수와 광고 보상

- 상태: `[ ]`
- 변경 파일 후보:
  - backend quota/ad reward domain
  - app quota UI
- 할 일:
  - 하루 5회 무료 정책
  - 광고 보상 검증
  - 앱 사용 가능 횟수 표시
- 검증:
  - quota service test
- 커밋 기준:
  - `feat: add daily practice quota`

## Phase 10. 코스 기능 재도입

목표: MVP 핵심 연습 흐름이 안정화된 뒤 코스를 다시 붙인다.

### 10.1 코스 정보 구조 확정

- 상태: `[ ]`
- 변경 파일 후보:
  - `docs/database.md`
  - `docs/api.md`
- 할 일:
  - Course
  - CourseSentence
  - UserCourseProgress
- 검증:
  - 현재 Practice 흐름과 충돌 없는지 확인
- 커밋 기준:
  - `docs: define course information architecture`

### 10.2 Course 탭 또는 Home 섹션 결정

- 상태: `[ ]`
- 변경 파일 후보:
  - app navigation/screen files
- 할 일:
  - 하단 탭 재도입 여부 결정
  - 홈 섹션으로 시작할지 결정
- 검증:
  - UX 흐름 리뷰
- 커밋 기준:
  - `feat: add course entry point`

## 추적 원칙

매 작업 후 아래를 남긴다.

```text
브랜치:
커밋:
변경 파일:
검증:
남은 이슈:
다음 작업:
```

예:

```text
브랜치: feat/pronunciation-prepare-api
커밋: abc1234 feat: expose pronunciation prepare endpoint
변경 파일: EvaluationController.java, PronunciationPrepareResponse.java
검증: ./gradlew test
남은 이슈: guide URL은 아직 더미
다음 작업: Flutter에서 prepare API 호출
```
