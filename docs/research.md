# LingKo 프로젝트 리서치

작성 기준: 2026-05-27

## 1. 한 줄 결론

LingKo는 "한국어 문장을 선택하거나 직접 입력하고, 표준 발음과 조음 가이드를 확인한 뒤, 직접 말해 점수와 교정 피드백을 받는 외국인 대상 한국어 발음 학습 앱"이다.

현재 저장소는 제품 아이디어를 검증할 수 있는 Flutter 앱 프로토타입과, 발음 변환/평가/가이드 생성 실험 코드가 있는 Spring Boot 백엔드로 나뉘어 있다.

가장 중요한 판단은 다음과 같다.

- 앱은 MVP 화면 흐름을 확인할 수 있는 수준까지 왔다.
- 백엔드는 표준 발음 변환 API만 실제 공개되어 있다.
- Azure 발음 평가, 조음 이미지/영상 생성, S3 업로드 코드는 있지만 아직 제품 API로 묶이지 않았다.
- 다음 우선순위는 새 화면을 늘리는 것이 아니라 "앱의 Practice 흐름을 실제 백엔드 API와 연결할 수 있는 얇은 MVP 계약"을 만드는 것이다.

## 2. 현재 저장소 구조

```text
LingKo/
├── app/                 # Flutter iOS/Android 앱
├── backend/             # Spring Boot 백엔드
├── docs/                # 제품/기술 문서
├── plan.md              # 제품 계획
└── docs/research.md     # 현재 문서
```

현재 브랜치 구조:

- `chore-monorepo-structure`: 백엔드와 앱을 분리한 구조 변경
- `feature/flutter-mvp-prototype`: Flutter 앱 MVP 프로토타입 추가

현재 미추적 상태로 남아 있는 파일:

- `backend/src/test/java/com/lingko/lingko/Syllablemappingloadertest.java`
- `backend/src/test/java/com/lingko/lingko/Visemeextractiondemotest.java`

이 두 파일은 기존 구조와 맞지 않는 레거시 테스트로 보이며, 앱 작업 커밋에는 포함하지 않았다.

## 3. 제품 목표 재정의

LingKo의 핵심 경험은 아래 한 흐름으로 정리된다.

```text
문장 선택 또는 직접 입력
  -> 표준 발음 변환
  -> 문장 듣기
  -> 글자별 발음 가이드 확인
  -> 사용자 녹음
  -> 발음 점수 확인
  -> 취약 글자별 입/혀 가이드 확인
  -> 다시 연습
```

중요한 제품 원칙:

- 발음 교정이 중심이다.
- 문법/단어장/강의는 MVP 핵심이 아니다.
- 사용자는 음운 규칙 이름보다 "어떻게 입과 혀를 움직여야 하는지"를 원한다.
- 앱은 짧은 문장, 빠른 녹음, 즉각적인 피드백, 재도전 루프에 집중해야 한다.

## 4. Flutter 앱 분석

### 4.1 현재 구현된 앱 범위

현재 앱은 백엔드와 연결하지 않은 디자인/흐름 검증용 프로토타입이다.

구현된 흐름:

- 홈에서 추천 문장 선택
- 하단 `Practice` 탭에서 직접 문장 입력
- 추천 문장을 누르면 Practice 입력창에 문장이 자동 입력
- Practice 탭으로 직접 들어가면 빈 입력창 표시
- 문장이 없으면 연습 상세와 녹음 버튼 숨김
- 문장이 있으면 원문, 표준 발음, 번역, 듣기 버튼 자리, 글자별 가이드 표시
- `Record and score`를 누르면 더미 결과 화면 표시
- 결과 화면에서 취약 글자를 누르면 입/혀 가이드 바텀시트 표시
- 하단 탭은 `Home`, `Practice`, `Profile`만 유지
- `Result`와 `Course`는 하단 탭에서 제거

관련 파일:

- `app/lib/app/lingko_app.dart`
- `app/lib/screens/home_screen.dart`
- `app/lib/screens/practice_screen.dart`
- `app/lib/screens/result_screen.dart`
- `app/lib/models/practice_sentence.dart`
- `app/lib/data/mock_sentences.dart`

### 4.2 앱 구조 평가

현재 앱은 Flutter 초보자가 읽기 좋은 구조로 나뉘어 있다.

```text
lib/
├── main.dart             # 앱 시작점
├── app/                  # 앱 루트, 테마, 하단 탭
├── data/                 # 더미 데이터
├── models/               # 화면 데이터 모델
├── screens/              # 화면 단위 위젯
└── widgets/              # 재사용 UI 위젯
```

좋은 점:

- `main.dart`가 `runApp()`만 담당한다.
- 화면, 모델, 더미 데이터, 재사용 위젯이 분리되어 있다.
- `PracticeScreen`은 추천 문장과 직접 입력을 모두 처리한다.
- `LingKoShell`이 탭 상태와 현재 연습 문장을 중앙에서 관리한다.
- 위젯 테스트가 추천 문장 흐름과 직접 입력 흐름을 검증한다.

아쉬운 점:

- 아직 상태 관리 라이브러리나 API 계층이 없다.
- 결과 모델이 없고 `ResultScreen`이 더미 점수와 고정 피드백을 사용한다.
- `Normal`, `Slow`, `Play`, `Repeat`, 알림/더보기 버튼은 아직 실제 동작이 없다.
- `pubspec.yaml`은 Flutter 기본 주석이 많이 남아 있어 프로젝트 문서성과 정돈감이 약하다.
- 앱 패키지명이 아직 `com.example.lingko_app`이다.

### 4.3 앱에서 다음에 필요한 구조

백엔드 연동 전 최소한 아래 구조가 필요하다.

```text
lib/
├── api/
│   ├── api_client.dart
│   └── pronunciation_api.dart
├── models/
│   ├── practice_sentence.dart
│   └── practice_result.dart
└── services/
    └── practice_service.dart
```

초기에는 Riverpod/Bloc 없이 `FutureBuilder` 또는 단순 `StatefulWidget`으로도 충분하다. 다만 API가 3개 이상 붙고 로그인/토큰이 들어가면 상태 관리 도입을 검토해야 한다.

## 5. 백엔드 분석

### 5.1 기술 스택

`backend/build.gradle` 기준 주요 구성:

- Java 17
- Spring Boot 3.4.1
- Spring MVC
- Spring Validation
- Spring Data JPA
- MySQL Connector
- WebFlux `WebClient`
- Azure Cognitive Services Speech SDK
- AWS SDK v2 S3
- H2 test

HTTP 서버는 MVC 기반이고, 외부 API 호출은 `WebClient`를 사용한다. 하지만 실제 구현은 `.block()`과 폴링을 쓰는 동기식 흐름에 가깝다.

### 5.2 현재 공개 API

현재 실제로 노출된 API는 하나다.

```http
POST /api/pronunciation/convert
```

요청:

```json
{
  "text": "맛있겠다."
}
```

응답:

```json
{
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싯게따."
}
```

흐름:

```text
EvaluationController
  -> EvaluationService.convertToStandardPronunciation()
      -> KoreanPhonemeUtil.toPronunciation()
```

현재 제품 플로우 중 백엔드가 실제로 제공하는 것은 "문장 -> 표준 발음"뿐이다.

### 5.3 이미 있는 핵심 엔진

#### 표준 발음 변환

핵심 클래스:

- `KoreanPhonemeUtil`

역할:

- 한글 음절 분해
- 발음 규칙 적용
- 음절 재조합

반영된 규칙:

- 연음화
- 비음화
- 유음화
- 경음화
- 구개음화
- 격음화
- 종성 7음 규칙 일부

이 프로젝트에서 가장 바로 제품화할 수 있는 강한 축이다.

#### 자모/음절 가이드 매핑

핵심 클래스:

- `SyllableMappingUtil`

역할:

- `syllable_mapping.json` 로딩
- 자모별 입/혀 이미지 매핑
- S3 가이드 URL 생성
- 프레임 쌍 생성

제품적으로는 "사용자가 어떤 문장을 입력해도 자모 단위 자산을 조합해 가이드를 만들 수 있다"는 방향과 맞다.

#### 발음 평가

핵심 클래스:

- `SpeechEvaluator`
- `AzureSpeechEvaluator`
- `AssessmentResult`

역할:

- 녹음 파일과 기준 문장 입력
- Azure Pronunciation Assessment 호출
- 정확도, 유창성, 완성도, 종합 점수 반환

현재 문제:

- API에 연결되어 있지 않다.
- 앱에서 녹음 파일을 보내는 엔드포인트가 없다.
- 결과 저장 모델과 연결되어 있지 않다.

#### 조음 영상 생성

핵심 클래스:

- `VideoGenerator`
- `FrameInterpolationVideoGenerator`
- `ReplicateApiClient`
- `VideoMerger`
- `S3Uploader`

흐름:

```text
자모별 이미지 URL
  -> 프레임 쌍 생성
  -> Replicate frame interpolation
  -> FFmpeg 병합
  -> S3 업로드
```

이 기능은 흥미롭지만 MVP 1순위는 아니다. 이유는 비용, 응답 시간, 실패 가능성이 높기 때문이다. MVP에서는 먼저 정적 이미지 또는 사전 생성된 가이드 URL을 쓰는 것이 현실적이다.

## 6. 데이터 모델 분석

현재 백엔드에는 다음 엔티티가 있다.

- `User`
- `EvaluationLog`
- `EvaluationSyllable`
- `Syllable`

현재 엔티티의 의도:

- 사용자 계정
- 발음 평가 로그
- 음절별 평가 결과
- 음절 가이드 URL

하지만 제품 관점에서는 `evaluation`보다 `practice`가 더 적절하다. 사용자는 평가를 받는 것이 아니라 연습 세션을 수행한다.

권장 방향:

- `EvaluationLog` -> `PracticeSession`
- `EvaluationSyllable` -> `PracticeCharacterResult`
- `Syllable` -> `PronunciationGuideAsset`

이미 `docs/database.md`에 더 구체적인 설계가 정리되어 있다. 다음 구현 때는 해당 문서를 기준으로 엔티티를 정리하는 것이 좋다.

## 7. 주요 리스크

### 7.1 보안 정보 관리

가장 먼저 확인해야 할 리스크다.

기존 분석 기준으로 `backend/src/main/resources/application.yaml`에는 실제 키로 보이는 값이 포함되어 있었다.

위험한 값:

- OpenAI API key
- Azure key
- Google OAuth secret
- AWS access key
- DB password

해야 할 일:

- 실제 키가 커밋된 적이 있다면 폐기/재발급
- `application.yaml`은 추적 제외
- 저장소에는 `application.example.yaml`만 유지
- 로컬 실행은 `.env` 또는 개인 설정 파일 사용

### 7.2 설정 구조 불일치

`application.example.yaml`, 설정 클래스, 실제 런타임 설정의 키 구조가 완전히 통일되어 있지 않다.

대표 예:

- `DBSettings`는 `username`을 기대하지만 예제는 `user`를 쓴다.
- AWS 설정은 nested 구조를 기대한다.
- Replicate 설정은 `replicate.*` 구조가 필요하다.
- Compose는 `DB_*` 환경 변수를 주입하지만 Spring DataSource와 직접 연결되는지 명확하지 않다.

이 문제를 해결하지 않으면 배포나 로컬 실행 시 환경마다 깨질 가능성이 높다.

### 7.3 테스트 신뢰도

현재 백엔드 테스트에는 레거시 파일이 섞여 있다.

문제 파일:

- `Syllablemappingloadertest.java`
- `Visemeextractiondemotest.java`

이 파일들은 현재 존재하지 않는 클래스/패키지를 참조하는 것으로 보인다.

반면 Flutter 쪽은 다음 검증이 통과했다.

- `dart format lib test`
- `flutter analyze`
- `flutter test`

즉 현재 신뢰 가능한 자동 검증은 앱 쪽이 더 낫고, 백엔드는 테스트 정리가 선행되어야 한다.

### 7.4 앱과 백엔드 API 간극

앱은 이미 아래 화면 상태를 갖고 있다.

- 추천 문장
- 직접 입력 문장
- 표준 발음 표시
- 발음 가이드 칩
- 결과 화면
- 취약 글자 피드백

하지만 백엔드는 아직 다음 API를 제공하지 않는다.

- 추천 문장 조회
- 문장 상세/번역 조회
- 직접 입력 문장의 표준 발음 변환과 가이드 반환
- 녹음 파일 업로드
- 발음 평가
- 평가 결과 저장
- 결과 조회

따라서 다음 단계는 앱 화면을 더 꾸미는 것이 아니라, 앱이 사용할 최소 API 계약을 먼저 확정하는 것이다.

### 7.5 장시간 작업 처리

비디오 생성은 HTTP 요청 안에서 동기 처리하기에 부적합하다.

문제 요소:

- Replicate prediction 생성
- 폴링
- 비디오 다운로드
- FFmpeg 병합
- S3 업로드

MVP에서는 이 파이프라인을 실시간 요청에 넣지 않는 것이 좋다.

권장:

- MVP: 사전 생성된 정적 이미지/URL 사용
- 이후: 비동기 job으로 생성
- 결과 조회: job status API 또는 캐시된 guide asset 조회

## 8. 현재 가장 강한 자산

프로젝트에서 버리지 말고 살려야 할 부분:

- `KoreanPhonemeUtil`: 표준 발음 변환 엔진
- `SyllableMappingUtil`: 자모 가이드 자산 매핑
- `AzureSpeechEvaluator`: 발음 평가 연동 가능성
- `FrameInterpolationVideoGenerator`: 장기적으로 고급 조음 영상 생성에 활용 가능
- Flutter 앱의 `Practice` 중심 흐름: MVP 핵심 UX와 잘 맞음

## 9. MVP 재정의

현재 상태를 고려하면 MVP는 너무 많은 것을 포함하면 안 된다.

### MVP 1차 목표

앱에서 한 문장을 선택하거나 직접 입력하고, 백엔드로부터 표준 발음을 받아 표시한 뒤, 더미가 아닌 실제 녹음 평가 결과를 받는 것.

### MVP 1차에 포함

- 추천 문장 목록
- 직접 문장 입력
- 표준 발음 변환 API 연결
- 문장 번역은 초기에는 추천 문장에 한정
- 녹음 파일 업로드
- Azure 발음 평가
- 결과 화면에 실제 점수 표시
- 취약 글자/음절은 단순 규칙 또는 Azure 결과 기반으로 제한적 표시
- 학습 결과는 로그인 없이도 로컬 또는 임시 세션 기준으로 먼저 검증 가능

### MVP 1차에서 뒤로 미룰 것

- Google/Apple OAuth
- 광고 기반 연습권
- 코스 기능
- 개인화 추천
- 장시간 동영상 자동 생성
- 다국어 전체 확장
- 결제

이전 계획에는 OAuth, 광고, 학습 기록까지 MVP 필수로 들어가 있었지만, 현재 개발 단계에서는 범위가 너무 넓다. 먼저 "발음 연습 1회가 실제로 끝까지 되는가"를 증명하는 것이 우선이다.

## 10. 내가 해야 할 우선순위

### P0. 보안/설정 정리

목표: 프로젝트를 더 진행해도 위험하지 않은 상태로 만든다.

해야 할 일:

- 실제 비밀값이 커밋되어 있는지 확인
- 노출된 키 폐기/재발급
- `backend/src/main/resources/application.yaml` 추적 여부 확인
- `.gitignore`에 로컬 설정 파일 제외 규칙 점검
- `application.example.yaml`을 기준 설정으로 삼기
- `@ConfigurationProperties`와 YAML 키 이름 통일

완료 기준:

- 저장소에 실제 키가 없다.
- 새 개발자가 `application.example.yaml`을 보고 로컬 설정을 만들 수 있다.
- 백엔드가 로컬에서 동일한 설정 방식으로 뜬다.

### P1. 백엔드 테스트 복구

목표: 백엔드 변경을 안전하게 할 수 있는 최소 회귀 방어선을 만든다.

해야 할 일:

- 레거시 테스트 2개 삭제 또는 현재 클래스 기준으로 재작성
- 외부 API 테스트와 단위 테스트 분리
- `KoreanPhonemeUtil`, `SyllableMappingUtil`, `EvaluationService` 테스트 통과 확인
- 네트워크/API 키가 필요한 테스트는 별도 profile/tag로 분리

완료 기준:

- `./gradlew test`가 로컬에서 통과한다.
- 외부 API 키 없이도 단위 테스트는 실행된다.

### P2. 앱-백엔드 최소 API 계약 정의

목표: Flutter 앱이 더미 데이터를 버리고 실제 백엔드와 통신할 수 있게 한다.

먼저 필요한 API:

```http
GET /api/sentences/recommended
POST /api/pronunciation/prepare
POST /api/pronunciation/evaluate
```

상세 계약 초안은 `docs/api.md`를 기준으로 한다.

권장 응답 모델:

```json
{
  "sentenceId": 1,
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싯게따.",
  "translation": "It looks delicious.",
  "learningPoint": "Final consonant linking and tense sound",
  "characters": [
    {
      "text": "마",
      "guideType": "MOUTH",
      "mouthGuideUrl": "...",
      "tongueGuideUrl": "..."
    }
  ]
}
```

완료 기준:

- 앱의 `PracticeSentence` 모델과 백엔드 응답 DTO가 거의 1:1로 대응된다.
- 직접 입력 문장도 `prepare` API로 표준 발음과 기본 가이드를 받을 수 있다.

### P3. 표준 발음 API를 앱에 연결

목표: 직접 입력 문장이 더미 `Custom sentence`가 아니라 실제 표준 발음으로 바뀌게 한다.

해야 할 일:

- Flutter에 HTTP client 추가
- Android emulator에서 백엔드 접근 주소 정리
- `PracticeScreen`에서 직접 입력 제출 시 API 호출
- 로딩/에러 상태 추가
- 추천 문장은 더미에서 시작하더라도 표준 발음은 서버 응답으로 교체

완료 기준:

- 앱에서 `맛있겠다.` 입력
- 서버가 `마싯게따.` 반환
- 앱 Practice 화면에 실제 변환 결과 표시

### P4. 발음 평가 API 1차 구현

목표: 녹음 후 실제 점수를 받는다.

해야 할 일:

- 앱 녹음 패키지 검토 및 추가
- 마이크 권한 설정
- 백엔드 multipart 업로드 API 추가
- `AzureSpeechEvaluator`를 API에서 호출
- `AssessmentResult`를 앱 결과 모델로 변환

완료 기준:

- 앱에서 녹음
- 백엔드로 파일 업로드
- Azure 평가 점수 반환
- Result 화면에 실제 점수 표시

### P5. 추천 문장 콘텐츠 API

목표: 홈 추천 문장을 서버 콘텐츠로 바꾼다.

해야 할 일:

- 추천 문장 seed 20개 작성
- `sentence`/`sentence_translation` 최소 테이블 또는 인메모리 seed 선택
- `GET /api/sentences/recommended` 구현
- Flutter `mockSentences` 제거 또는 fallback으로만 유지

완료 기준:

- 홈 추천 문장이 서버에서 내려온다.
- 앱의 추천 카드 클릭 흐름은 현재 UX 그대로 유지된다.

### P6. 결과 모델과 저장

목표: 더미 결과 화면을 실제 결과 기반으로 바꾼다.

해야 할 일:

- Flutter `PracticeResult` 모델 추가
- `ResultScreen`이 `PracticeSentence`가 아니라 `PracticeResult`를 받도록 변경
- 백엔드 `PracticeSession` 저장 API 설계
- 점수 breakdown을 실제 값으로 표시
- 취약 글자 기준을 명확히 정의

완료 기준:

- Result 화면의 총점, accuracy, fluency, completeness가 실제 API 값이다.
- `Good` 같은 고정 문구가 사라지고 점수 기반 메시지가 나온다.

### P7. 조음 가이드 현실화

목표: 바텀시트의 임시 그림을 실제 가이드 자산으로 교체한다.

해야 할 일:

- MVP에서는 정적 이미지 URL 우선
- S3 guide asset 경로 정책 확정
- `SyllableMappingUtil` 반환 계약 정리
- 앱에서 이미지 로딩/에러 상태 처리

완료 기준:

- 취약 글자를 누르면 실제 입/혀 가이드 이미지가 뜬다.
- 영상 생성은 아직 하지 않아도 된다.

### P8. 로그인/기록/광고

목표: 제품 운영 기능을 붙인다.

순서:

1. Google/Apple OAuth
2. JWT refresh token
3. PracticeSession 저장
4. 프로필/학습 기록
5. 일일 무료 횟수
6. 광고 보상

이 단계는 발음 연습 1회가 실제로 동작한 뒤에 진행하는 것이 맞다.

## 11. 권장 작업 순서 요약

가장 현실적인 순서는 다음이다. 우선순위는 [priorities.md](priorities.md)에, 실제 커밋 단위의 더 작은 작업 목록은 [task-breakdown.md](task-breakdown.md)에 따로 정리한다.

```text
1. 보안/설정 정리
2. 백엔드 테스트 복구
3. 앱-백엔드 DTO/API 계약 정의
4. 표준 발음 API 앱 연결
5. 녹음/발음 평가 API 연결
6. 추천 문장 서버화
7. Result 모델 정리와 결과 저장
8. 조음 가이드 자산 연결
9. 로그인/기록/광고
10. 코스 기능 재도입
```

코스 기능은 제품적으로 필요하지만 지금 당장은 후순위다. 현재 앱에서 코스를 제거한 판단은 맞다. 먼저 자유 입력과 추천 문장 기반의 짧은 발음 연습이 실제로 끝까지 동작해야 한다.

## 12. 이번 주에 할 만한 구체 작업

가장 추천하는 단기 스프린트는 다음 5개다.

1. 백엔드 비밀값과 설정 파일 정리
2. 백엔드 테스트가 통과하도록 레거시 테스트 정리
3. `POST /api/pronunciation/prepare` 설계 및 구현
4. Flutter에서 직접 입력 문장을 `prepare` API에 연결
5. 앱 Result 화면용 `PracticeResult` 모델 설계

이 5개가 끝나면 LingKo는 "예쁜 목업"에서 "실제 발음 엔진과 연결된 MVP"로 넘어간다.

## 13. 최종 판단

현재 프로젝트는 방향이 좋다. 특히 "직접 입력"과 "추천 문장"을 모두 지원하는 Practice 중심 UX는 LingKo의 제품 핵심과 맞다.

다만 지금부터는 기능을 넓히기보다 깊이를 만들어야 한다.

지금 필요한 것은 코스, 광고, 로그인, 개인화가 아니라 아래 한 줄이다.

```text
사용자가 문장을 입력한다
  -> 서버가 표준 발음을 만든다
      -> 사용자가 녹음한다
          -> 서버가 실제 점수를 준다
              -> 앱이 그 결과로 다시 연습하게 만든다
```

이 흐름이 실제로 동작하면 나머지 기능은 그 위에 순서대로 얹을 수 있다.
