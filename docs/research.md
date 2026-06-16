# LingKo 프로젝트 리서치

확인일: 2026-06-16

## 1. 목적

이 문서는 LingKo의 제품 의도, 현재 코드 구현 상태, MVP 대비 갭, 기술 리스크, 다음 구현 우선순위를 한곳에 정리한다.

분석 기준 파일:

- `plan.md`
- `.codex/AGENTS.md`
- `backend/build.gradle`
- `backend/src/main/java/com/lingko/lingko/**`
- `backend/src/test/java/com/lingko/lingko/**`
- `backend/src/integrationTest/java/com/lingko/lingko/**`
- `app/pubspec.yaml`
- `app/lib/**`
- `app/test/widget_test.dart`

## 2. 제품 정의

LingKo는 외국인 학습자가 한국어 문장을 듣고, 표준 발음을 이해하고, 직접 말한 뒤, 점수와 입/혀 모양 가이드를 받아 교정하는 발음 특화 한국어 학습 서비스다.

핵심 흐름은 다음과 같다.

1. 사용자가 추천 문장 또는 직접 입력 문장을 선택한다.
2. 시스템이 원문을 표준 발음으로 변환한다.
3. 시스템이 번역, 문장 음성, 글자별 조음 가이드를 제공한다.
4. 사용자가 문장을 녹음한다.
5. 시스템이 발음 점수와 취약 발음 피드백을 제공한다.
6. 사용자가 입/혀 가이드를 보며 반복 연습한다.

MVP는 "작은 범위지만 끝까지 되는 경험"에 집중해야 한다. 현재 코드 기준으로는 UI 프로토타입과 표준 발음 변환 API가 가장 앞서 있으며, 인증, 녹음 업로드, 발음 평가, 학습 기록 저장은 아직 제품 흐름으로 연결되지 않았다.

## 3. 현재 저장소 구조

```text
LingKo/
├── plan.md
├── docs/
│   └── research.md
├── backend/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── application.example.yaml
│   ├── .env.example
│   └── src/
│       ├── main/java/com/lingko/lingko/
│       ├── main/resources/
│       ├── test/java/com/lingko/lingko/
│       └── integrationTest/java/com/lingko/lingko/
└── app/
    ├── pubspec.yaml
    ├── lib/
    └── test/
```

역할별 관점:

- 백엔드: Spring Boot 기반 API, 표준 발음 변환, 평가/조음/저장 도메인 골격.
- 앱: Flutter 기반 추천 문장, 직접 입력, 연습, 결과, 프로필 화면 프로토타입.
- 인프라: MySQL, S3, Azure Speech, Replicate, ffmpeg 사용을 전제로 한 설정과 일부 구현.

## 4. 백엔드 분석

### 4.1 기술 스택

`backend/build.gradle` 기준:

- Java 17
- Spring Boot 3.4.1
- Spring Web
- Spring Validation
- Spring Data JPA
- Spring WebFlux
- MySQL Connector/J
- Lombok
- Azure Cognitive Services Speech SDK
- AWS SDK S3
- H2 테스트 의존성
- 별도 `integrationTest` source set

### 4.2 현재 공개 API

현재 실제로 노출된 API는 표준 발음 변환 API 1개다.

```http
POST /api/pronunciation/convert
Content-Type: application/json

{
  "text": "밥을 먹었어요."
}
```

응답:

```json
{
  "originalText": "밥을 먹었어요.",
  "standardPronunciation": "바블 머거써요."
}
```

구현 근거:

- `EvaluationController`
- `StandardPronunciationRequest`
- `StandardPronunciationResponse`
- `EvaluationService`
- `KoreanPhonemeUtil`

제약:

- `text`는 blank 금지.
- `text`는 1자 이상 30자 이하.
- 현재 응답은 원문과 표준 발음만 포함한다.
- 번역, 음성 URL, 글자별 가이드, 발음 점수는 포함하지 않는다.

### 4.3 표준 발음 변환

`KoreanPhonemeUtil`은 한글 음절을 초성/중성/종성으로 분해한 뒤 다음 규칙을 적용한다.

- 연음화
- 비음화
- 경음화
- 구개음화
- 유음화
- 격음화
- 종성 7음 규칙

테스트 근거:

- `KoreanPhonemeUtilTest`
- `EvaluationServiceTest`

현재 강점:

- 제품의 핵심인 "글자 그대로가 아닌 실제 발음 표기"의 첫 기능이 구현되어 있다.
- 대표 음운 규칙 단위 테스트가 존재한다.
- 공백과 비한글 문자 유지 테스트가 존재한다.

주의점:

- 표준 발음 규칙은 예외가 많으므로, 실제 학습 콘텐츠 품질을 위해 국립국어원식 표준 발음과 비교하는 회귀 테스트 세트가 필요하다.
- 현재 API는 문장 단위이지만 내부 피드백은 글자/음소 단위가 필요하므로 변환 결과와 음절 매핑을 함께 내려주는 계약이 필요하다.

### 4.4 도메인 모델

현재 확인된 주요 엔티티:

- `User`
  - `socialId`
  - `socialType`
  - `email`
  - `name`
  - `profileImageUrl`
  - `createdAt`
  - `lastLoginAt`
- `EvaluationLog`
  - `user`
  - `originalWord`
  - `score`
  - `createdAt`
  - `syllableList`
- `EvaluationSyllable`
  - `evaluationLog`
  - `syllable`
  - `score`
- `Syllable`
  - `syllableChar`
  - `mouthUrl`
  - `tongueUrl`

현재 repository:

- `UserRepository.findBySocialIdAndSocialType`
- `EvaluationLogRepository.findByUser_UserIdxOrderByCreatedAtDesc`
- `EvaluationSyllableRepository`
- `SyllableRepository`

해석:

- 소셜 로그인 기반 사용자와 평가 기록 저장의 기본 모델은 준비되어 있다.
- 다만 인증 API, JWT 발급/검증 필터, 현재 사용자 조회, 평가 저장 API는 아직 공개 API로 연결되어 있지 않다.
- `EvaluationLog.originalWord`는 길이 50으로 제한되어 있어 MVP의 자유 입력 정책과 맞춰야 한다.
- `EvaluationLog.addSyllable`은 현재 리스트에 추가하지 않고 `setEvaluationLog`만 호출하는 형태라 추후 저장 흐름 구현 전 점검이 필요하다.

### 4.5 외부 연동 구성

설정 파일과 구현체 기준으로 다음 외부 연동이 전제되어 있다.

- Azure Speech
  - `AzureSpeechEvaluator`
  - `SpeechEvaluator`
  - `AssessmentResult`
  - 발음 평가 점수: accuracy, fluency, completeness, pronunciation, recognizedText
- Replicate
  - `ReplicateApiClient`
  - frame interpolation prediction 생성/폴링
- ffmpeg
  - `VideoMerger`
  - 여러 영상 세그먼트 병합
- S3
  - `S3Uploader`
  - 생성 이미지/영상 업로드
- MySQL
  - `docker-compose.yml`
  - JPA 기반 영속화

현재 상태:

- 외부 연동 컴포넌트는 존재하지만 사용자 API 흐름과 연결되지 않은 상태다.
- Azure 평가, Replicate 영상 생성, S3 업로드는 통합 테스트 영역에 분리되어 있다.
- 운영하려면 API 키, region, bucket, ffmpeg, MySQL 환경 설정이 모두 필요하다.

보안상 주의:

- `application.example.yaml`과 `.env.example`은 환경변수 기반이며 키 값은 비어 있다.
- 실제 `application.yaml`은 존재하지만 민감정보 포함 가능성이 있어 문서에는 값을 기록하지 않는다.
- `ReplicateApiClient`는 API key 앞 5글자를 로그로 남기는 코드가 있어 운영 전 제거 또는 마스킹 정책 강화가 필요하다.

## 5. 앱 분석

### 5.1 기술 스택

`app/pubspec.yaml` 기준:

- Flutter SDK
- Dart SDK `^3.7.0`
- Material UI
- `cupertino_icons`
- `flutter_lints`
- 별도 HTTP, OAuth, audio, recorder, state management 패키지는 아직 없다.

현재 앱은 외부 네트워크 연동 없이 Flutter 기본 위젯과 `setState` 중심으로 구성된 프로토타입이다.

### 5.2 화면 구조

`LingKoShell` 기준 하단 탭 3개:

- Home
- Practice
- Profile

주요 화면:

- `HomeScreen`
  - LingKo 타이틀
  - 오늘 남은 무료 연습 횟수 문구
  - 진행 패널
  - 추천 문장 목록
- `PracticeScreen`
  - 직접 문장 입력
  - 선택 문장의 원문/표준 발음/번역 표시
  - 일반/느리게 듣기 버튼
  - 글자별 pronunciation guide chip
  - 녹음 및 점수 버튼
- `ResultScreen`
  - 더미 점수
  - 취약 발음 목록
  - 다시 시도 버튼
- `ProfileScreen`
  - 표시 언어
  - 모국어
  - 목표 레벨

### 5.3 데이터와 상태관리

현재 데이터:

- `mock_sentences.dart`에 추천 문장 3개가 하드코딩되어 있다.
- `PracticeSentence`와 `CharacterResult`가 앱 내부 임시 모델 역할을 한다.
- 직접 입력 문장은 `PracticeSentence.custom(text)`로 임시 객체를 만든다.

현재 상태관리:

- `LingKoShell`의 `selectedTab`, `selectedSentence`, `hasResult`.
- `PracticeScreen`의 `TextEditingController`, `FocusNode`, `canSubmitCustomSentence`.

현재 UX 구현 수준:

- 추천 문장 선택 후 Practice 탭 이동 가능.
- 직접 입력 문장 사용 가능.
- 녹음 버튼을 누르면 실제 녹음 없이 Result 화면으로 전환.
- 글자별 가이드는 `CustomPainter` 임시 그림으로 표현.

아직 없는 것:

- 로그인/회원가입 화면
- Google OAuth / Apple Sign in
- API client
- 로딩/성공/실패 상태
- 실제 녹음 권한 요청
- 음성 녹음/파일 업로드
- 오디오 재생
- 실제 발음 평가 결과 매핑
- 학습 기록 조회
- 광고 시청 후 추가 연습권
- 다국어 리소스 관리

### 5.4 앱 테스트

`app/test/widget_test.dart`는 다음 플로우를 검증한다.

- 앱 실행 후 추천 문장 노출.
- 추천 문장 선택 후 Practice 화면 이동.
- Record and score 버튼 후 Result 화면 이동.
- Practice 탭에서 직접 문장 입력.

테스트는 현재 프로토타입 UI 흐름에 맞춰져 있으며, API/녹음/인증 검증은 포함하지 않는다.

## 6. MVP 대비 갭

### 6.1 구현됨

- Flutter 앱 기본 화면 구조.
- 추천 문장 기반 연습 UI.
- 직접 문장 입력 UI.
- 결과 화면 UI.
- 프로필 설정 표시 UI.
- 백엔드 표준 발음 변환 API.
- 한국어 표준 발음 변환 유틸리티와 단위 테스트.
- 사용자/평가 로그/음절/가이드 URL 관련 JPA 모델 초안.
- Azure Speech, Replicate, S3, ffmpeg 연동 컴포넌트 초안.

### 6.2 부분 구현 또는 내부 준비

- 소셜 로그인 도메인 모델은 있으나 OAuth API와 앱 로그인 화면은 없다.
- 평가 결과 저장 엔티티와 repository는 있으나 저장 API는 없다.
- Azure 발음 평가 구현체는 있으나 음성 업로드 API와 연결되지 않았다.
- 조음 가이드 URL 추출과 영상 생성 인프라는 있으나 사용자 화면/API와 연결되지 않았다.
- 무료 연습 횟수는 UI 문구만 있고 서버 정책/카운터가 없다.

### 6.3 미구현

- Google OAuth 로그인.
- Apple Sign in.
- JWT 발급/갱신/검증.
- 현재 사용자 프로필 API.
- 추천 문장 API.
- 자유 입력 문장 학습 준비 API.
- 문장 번역.
- TTS 또는 문장 음성 제공.
- 앱 오디오 재생.
- 앱 녹음.
- 음성 파일 업로드.
- 발음 평가 실행 API.
- 글자별 점수/피드백 생성.
- 학습 기록 저장/조회.
- 광고 시청 후 추가 연습권.
- 다국어 UI/피드백 리소스.
- 운영용 에러 응답 표준화.

## 7. API 계약 후보

아래 계약은 현재 코드와 MVP 목표를 연결하기 위한 후보이며, 구현 전 아키텍트 확정이 필요하다.

### 7.1 인증

```http
POST /api/auth/oauth/login
```

요청 후보:

```json
{
  "provider": "GOOGLE",
  "idToken": "provider-id-token",
  "displayLanguage": "en",
  "nativeLanguage": "en"
}
```

응답 후보:

```json
{
  "accessToken": "jwt",
  "refreshToken": "jwt",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "User",
    "profileImageUrl": "https://example.com/image.png",
    "displayLanguage": "en",
    "nativeLanguage": "en"
  }
}
```

### 7.2 추천 문장 목록

```http
GET /api/practice/sentences?level=BEGINNER_1&limit=20
```

응답 후보:

```json
{
  "items": [
    {
      "id": 1,
      "text": "맛있겠다.",
      "standardPronunciation": "마싯게따.",
      "translation": "It looks delicious.",
      "level": "BEGINNER_2",
      "category": "FOOD",
      "learningPoint": "Final consonant linking and tense sound"
    }
  ]
}
```

### 7.3 문장 학습 준비

현재 `/api/pronunciation/convert`는 유지하되 MVP에는 더 풍부한 응답이 필요하다.

```http
POST /api/practice/prepare
```

요청 후보:

```json
{
  "text": "맛있겠다.",
  "displayLanguage": "en"
}
```

응답 후보:

```json
{
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싯게따.",
  "translation": "It looks delicious.",
  "audioUrl": "https://...",
  "syllables": [
    {
      "text": "마",
      "mouthGuideUrl": "https://...",
      "tongueGuideUrl": "https://...",
      "learningPoint": "Stable vowel shape"
    }
  ]
}
```

### 7.4 발음 평가

```http
POST /api/evaluations
Content-Type: multipart/form-data
Authorization: Bearer <accessToken>
```

요청 후보:

- `referenceText`: 원문 또는 표준 발음 기준 문장.
- `audio`: 사용자 녹음 파일.
- `practiceSource`: `RECOMMENDED` 또는 `CUSTOM`.
- `sentenceId`: 추천 문장인 경우 optional.

응답 후보:

```json
{
  "evaluationId": 100,
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싯게따.",
  "recognizedText": "마싯게따",
  "score": 82,
  "scores": {
    "accuracy": 80.4,
    "fluency": 84.0,
    "completeness": 90.0,
    "pronunciation": 82.0
  },
  "syllables": [
    {
      "text": "싯",
      "score": 68,
      "feedback": "Keep the tongue closer for the sibilant sound.",
      "mouthGuideUrl": "https://...",
      "tongueGuideUrl": "https://..."
    }
  ],
  "remainingFreePractices": 4
}
```

### 7.5 학습 기록

```http
GET /api/evaluations/me?page=0&size=20
```

응답 후보:

```json
{
  "items": [
    {
      "evaluationId": 100,
      "originalText": "맛있겠다.",
      "score": 82,
      "createdAt": "2026-06-16T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

## 8. 데이터 모델 보완 후보

현재 모델에 추가 검토가 필요한 필드:

- `users`
  - `display_language`
  - `native_language`
  - `target_level`
  - `refresh_token_hash` 또는 별도 token/session 테이블
- `practice_sentences`
  - 추천 문장 원문
  - 표준 발음
  - 번역
  - 레벨
  - 카테고리
  - 학습 포인트
  - 활성 여부
- `evaluation_log`
  - `standard_pronunciation`
  - `recognized_text`
  - `audio_url`
  - `accuracy_score`
  - `fluency_score`
  - `completeness_score`
  - `pronunciation_score`
  - `practice_source`
  - `sentence_id`
- `evaluation_syllable`
  - `syllable_text`
  - `feedback`
  - `mouth_guide_url`
  - `tongue_guide_url`
  - `position`
- `practice_quota`
  - 사용자별 일일 무료 횟수
  - 광고 보상 횟수
  - reset 기준일

## 9. 우선순위 제안

### 9.1 1단계: 계약 정리

목표: 앱과 백엔드가 함께 볼 API 계약을 먼저 고정한다.

작업:

- 표준 발음 변환 API 유지 여부 결정.
- `practice/prepare`와 `evaluations` API 응답 모델 확정.
- 공통 에러 응답 형식 정의.
- 인증 필요 API와 공개 API 구분.

완료 기준:

- 앱 모델과 백엔드 DTO가 같은 필드명을 사용한다.
- 실패 응답과 로딩/재시도 UX가 정의된다.

### 9.2 2단계: 표준 발음 기반 학습 준비 API

목표: 직접 입력 문장 하나에 대해 백엔드 결과를 앱에 표시한다.

작업:

- 현재 `/api/pronunciation/convert`를 앱에서 호출하거나, MVP용 `/api/practice/prepare`를 만든다.
- 앱에 API client를 추가한다.
- Practice 화면에 loading/error/success 상태를 추가한다.
- mock sentence와 API sentence 모델을 분리한다.

완료 기준:

- 사용자가 직접 입력한 문장에 대해 서버 표준 발음이 앱 화면에 표시된다.

### 9.3 3단계: 녹음 없는 평가 저장 또는 녹음 평가 중 택일

MVP의 핵심은 실제 발음 평가지만, 구현 위험이 높다. 다음 둘 중 하나를 선택해야 한다.

- 빠른 데모: 표준 발음 변환과 더미 점수 저장으로 기록 흐름 완성.
- 실제 MVP: 앱 녹음, multipart 업로드, Azure Speech 평가, 결과 저장까지 연결.

권장:

- 제품 검증용이면 빠른 데모 흐름을 먼저 닫는다.
- 기술 검증용이면 Azure Speech 평가 API를 먼저 수직 구현한다.

### 9.4 4단계: 인증과 기록

목표: 사용자별 학습 기록을 누적한다.

작업:

- Google OAuth 우선 구현.
- Apple Sign in은 iOS 배포/앱 식별자 준비가 필요하므로 후속.
- JWT 발급/검증.
- 평가 로그 저장/조회.
- 앱 secure storage 도입.

### 9.5 5단계: 조음 가이드 품질화

목표: 글자별 입/혀 가이드를 실제 학습 가치가 있는 콘텐츠로 만든다.

작업:

- 현재 `GuidePainter` 임시 그림을 이미지/영상 URL 기반 위젯으로 교체.
- `syllable_mapping.json`의 asset coverage 점검.
- S3 URL 하드코딩 정책 정리.
- 영상 생성이 실시간인지 사전 생성인지 결정.

권장:

- MVP에서는 실시간 영상 생성보다 사전 생성된 가이드 asset을 쓰는 편이 안정적이다.
- Replicate frame interpolation은 운영 비용과 지연 시간이 크므로, 관리자/배치 생성 도구로 분리하는 편이 좋다.

## 10. 주요 리스크

### 10.1 제품 리스크

- 발음 평가 점수가 학습자가 납득할 만큼 정확하지 않으면 핵심 가치가 약해진다.
- 글자별 피드백이 실제 음성 평가 결과와 정렬되지 않으면 "좋아 보이는 UI"에 그칠 수 있다.
- 다국어 설명과 번역 품질은 MVP 범위를 쉽게 초과할 수 있다.

### 10.2 백엔드 리스크

- Azure Speech 평가 결과를 한국어 글자별 피드백으로 매핑하는 로직이 아직 없다.
- 외부 연동이 많아 테스트와 운영 안정성이 어렵다.
- Replicate polling과 ffmpeg 병합은 응답 시간이 길 수 있어 사용자 요청-응답 API에 직접 묶기 어렵다.
- S3/Replicate/Azure key가 필요한 통합 테스트는 CI에서 별도 프로파일로 분리해야 한다.
- API key 일부를 로그로 남기는 코드는 운영 전 제거해야 한다.

### 10.3 앱 리스크

- 현재 앱에는 API client, 녹음, 오디오 재생, 인증, secure storage가 없다.
- `setState`만으로 MVP 초반은 가능하지만 인증/평가/기록이 붙으면 상태 범위가 커진다.
- 녹음 권한, iOS/Android permission, 파일 포맷, 업로드 실패 UX를 별도로 설계해야 한다.
- 광고 보상형 연습권은 플랫폼 SDK와 정책 검토가 필요하다.

### 10.4 운영 리스크

- MySQL, S3, Azure, Replicate, ffmpeg가 모두 필요한 구조라 로컬 개발과 배포 문서가 중요하다.
- 비용이 발생하는 외부 API는 rate limit, timeout, retry, quota 정책이 필요하다.
- 생성 영상/음성 파일 보관 기간과 삭제 정책이 필요하다.

## 11. 검증 전략

이번 분석에서 실행한 검증:

- `cd backend && ./gradlew test`
  - 결과: 통과.
  - 비고: 일반 샌드박스에서는 `~/.gradle` lock 파일 접근 권한 문제로 실패했고, 권한 상승 후 실제 테스트는 성공했다.
  - Gradle 경고: Gradle 9.0과 호환되지 않는 deprecated feature 사용 경고가 출력됐다.
- `cd app && flutter test`
  - 결과: 통과.
  - 비고: 일반 샌드박스에서는 Flutter SDK cache lockfile 접근 권한 문제로 실패했고, 권한 상승 후 실제 테스트는 성공했다.
  - Flutter 알림: 새 Flutter 버전 사용 가능 안내가 출력됐다.

### 11.1 백엔드

우선 검증:

```bash
cd backend
./gradlew test
```

외부 연동 검증:

```bash
cd backend
./gradlew integrationTest
```

주의:

- `integrationTest`는 Azure, Replicate, AWS, ffmpeg, DB 설정이 필요할 수 있다.
- 민감정보는 `.env` 또는 환경변수로만 주입한다.

필요 테스트:

- 표준 발음 변환 회귀 테스트 확대.
- `/api/pronunciation/convert` controller validation 테스트.
- 인증 성공/실패 테스트.
- 평가 저장 transaction 테스트.
- 외부 API 실패 시 fallback/error response 테스트.

### 11.2 앱

우선 검증:

```bash
cd app
flutter test
```

필요 테스트:

- 직접 문장 입력 후 API 성공/실패 상태.
- 추천 문장 선택 후 학습 준비 API 표시.
- 녹음 권한 거부/허용 상태.
- 평가 결과 화면의 점수/글자별 피드백 렌더링.
- 프로필 언어 설정 변경.

### 11.3 통합

우선 수직 흐름:

1. 앱 직접 입력.
2. 백엔드 표준 발음 변환 호출.
3. 앱 Practice 화면에 표준 발음 표시.
4. 녹음 없이 결과 화면 이동.

다음 수직 흐름:

1. 앱 녹음.
2. multipart 업로드.
3. Azure Speech 평가.
4. 평가 결과 저장.
5. Result 화면 표시.
6. Profile 또는 History에서 기록 조회.

## 12. 다음 액션

가장 현실적인 다음 작업 순서:

1. API 계약 문서 작성: `practice/prepare`, `evaluations`, auth, error response.
2. 백엔드: 표준 발음 변환 controller 테스트와 에러 응답 표준화.
3. 앱: API client 추가 후 직접 입력 문장의 표준 발음 API 연동.
4. 백엔드: 추천 문장 seed/model/API 추가.
5. 앱: 추천 문장 mock 제거 및 API 연동.
6. 백엔드: Google OAuth와 JWT 최소 구현.
7. 앱: 로그인 화면과 토큰 저장.
8. 백엔드/앱: 녹음 업로드와 Azure 평가 수직 구현.
9. QA: 실제 기기 녹음, 네트워크 실패, 권한 거부, 긴 문장, 비한글 입력 검증.

## 13. 결론

LingKo는 제품 방향이 `plan.md`에 명확하고, 현재 코드는 "발음 학습 앱의 UI 프로토타입"과 "표준 발음 변환 중심 백엔드"가 각각 따로 존재하는 단계다.

다음 성공 기준은 기능을 넓히는 것이 아니라 하나의 수직 흐름을 닫는 것이다. 가장 먼저 닫을 흐름은 "사용자 직접 입력 -> 백엔드 표준 발음 변환 -> 앱에 결과 표시"다. 이 흐름이 안정화되면 녹음, Azure 평가, 학습 기록, 인증을 순서대로 붙이는 것이 리스크를 가장 작게 만든다.
