# LingKo API Contract

작성 기준: 2026-06-01

이 문서는 Flutter 앱의 `PracticeSentence` 모델과 백엔드 DTO가 같은 필드 계약을 쓰도록 맞추기 위한 초안이다. 현재 구현된 API는 `POST /api/pronunciation/convert`뿐이며, 아래 `sentences`와 `prepare` API는 다음 구현 작업의 목표 계약이다.

## 공통 규칙

- JSON 필드는 `camelCase`를 사용한다.
- 시간 값은 ISO-8601 문자열을 사용한다.
- 점수는 평가 완료 전에는 `null`을 사용한다.
- 앱 표시용 문장 준비 응답과 실제 평가 결과 응답은 분리한다.
- `PracticeSentence` 화면 모델은 `PracticeSentenceResponse` 또는 `PronunciationPrepareResponse`에서 만든다.
- 성공 응답은 별도 envelope 없이 endpoint별 리소스 본문을 직접 반환한다.
- 실패 응답은 모든 API에서 `ErrorResponse` 형식을 사용한다.
- MVP 내부 프로토타입에서는 `prepare`, `recommended sentences`, `evaluate`를 익명 호출 가능하게 둘 수 있지만, 공개 MVP에서는 `evaluate`, `evaluations/me`, `quota/today`에 인증을 요구한다.

## 인증 정책

| API | MVP 내부 프로토타입 | 공개 MVP | 비고 |
| --- | --- | --- | --- |
| `POST /api/pronunciation/convert` | 불필요 | 불필요 | 개발/호환용 표준 발음 변환 API |
| `POST /api/pronunciation/prepare` | 불필요 | 선택 | 직접 입력 또는 추천 문장 학습 준비 |
| `GET /api/sentences/recommended` | 불필요 | 불필요 | 공개 콘텐츠 조회 |
| `GET /api/sentences/{sentenceId}` | 불필요 | 불필요 | 공개 콘텐츠 조회 |
| `POST /api/evaluations` | 불필요 | 필요 | 비용 발생 API. 내부 프로토타입에서는 임시 허용 |
| `GET /api/evaluations/me` | 필요 | 필요 | 사용자별 기록 조회 |
| `GET /api/quota/today` | 필요 | 필요 | 사용자별 사용량 조회 |
| `POST /api/auth/oauth/login` | 불필요 | 불필요 | OAuth token으로 앱 자체 JWT 발급 |

인증이 필요한 API는 `Authorization: Bearer <accessToken>` 헤더를 사용한다.

## 기준 문장 정책

Azure Speech 평가의 `referenceText`는 서버가 계산한 `standardPronunciation`을 사용한다. 사용자가 입력하거나 추천 문장에 저장된 `originalText`는 화면 표시, 학습 맥락, 저장 스냅샷 용도로 유지한다.

이 정책을 선택하는 이유:

- LingKo의 핵심 학습 대상은 철자 그대로의 낭독이 아니라 실제 표준 발음이다.
- `맛있겠다.`처럼 원문과 실제 발음이 다른 문장에서 평가 기준이 원문이면 학습 목표와 점수가 어긋날 수 있다.
- 앱은 원문, 표준 발음, 인식 결과를 함께 보여줘 사용자가 "무엇을 봤고, 무엇을 말해야 했고, 실제로 어떻게 인식됐는지"를 비교할 수 있어야 한다.

예시:

| originalText | standardPronunciation | evaluation referenceText | 앱 표시 |
| --- | --- | --- | --- |
| `맛있겠다.` | `마싣게따.` | `마싣게따.` | 원문 `맛있겠다.`, 발음 `마싣게따.`, 인식 결과 |
| `밥을 먹었어요.` | `바블 머거써요.` | `바블 머거써요.` | 원문 `밥을 먹었어요.`, 발음 `바블 머거써요.`, 인식 결과 |
| `같이 가요.` | `가치 가요.` | `가치 가요.` | 원문 `같이 가요.`, 발음 `가치 가요.`, 인식 결과 |

## 상태 코드

| Status | 사용 상황 |
| --- | --- |
| `200 OK` | 조회, 변환, 준비, 평가 성공 |
| `201 Created` | 후속 구현에서 리소스 생성만 수행하고 상세 응답을 별도 조회하게 만들 때 사용. 현재 MVP 평가 API는 결과 본문을 바로 반환하므로 `200 OK`를 사용한다. |
| `400 Bad Request` | 요청 JSON, multipart, query parameter 형식 오류 |
| `401 Unauthorized` | 인증 필요 API에서 access token 누락, 만료, 위조 |
| `403 Forbidden` | 인증은 됐지만 본인 리소스가 아니거나 권한이 부족함 |
| `404 Not Found` | 추천 문장, 평가 기록 등 대상 리소스 없음 |
| `409 Conflict` | 이미 처리된 `practiceToken` 재사용 등 상태 충돌 |
| `413 Payload Too Large` | 음성 파일 또는 입력 문장이 제한 초과 |
| `415 Unsupported Media Type` | 지원하지 않는 음성 파일 형식 |
| `422 Unprocessable Entity` | 한국어 문장으로 처리할 수 없는 입력, guide mapping 실패 등 의미 검증 실패 |
| `429 Too Many Requests` | 일일 무료 평가 횟수 초과 |
| `502 Bad Gateway` | Azure, S3, OAuth provider 등 외부 연동 실패 |
| `503 Service Unavailable` | 외부 서비스 일시 중단 또는 평가 기능 일시 비활성화 |

## Enum

### SentenceSource

```text
RECOMMENDED
CUSTOM
```

### GuideType

```text
MOUTH
TONGUE
BOTH
NONE
```

### GuideStatus

```text
AVAILABLE
MISSING
PENDING
```

3.1 범위에서는 정적 가이드 조회 기준으로 `AVAILABLE` 또는 `MISSING`을 주로 사용한다. 비동기 생성 상태인 `PROCESSING`, `COMPLETED`, `FAILED`는 3.3에서 별도 확정한다.

## PracticeSentenceResponse

추천 문장 카드와 Practice 화면의 기본 데이터다. Flutter `PracticeSentence`와의 매핑은 다음과 같다.

| API field | Flutter field | 설명 |
| --- | --- | --- |
| `originalText` | `text` | 화면에 표시할 원문 |
| `standardPronunciation` | `pronunciation` | 표준 발음 |
| `translation` | `translation` | 영어 번역. 직접 입력이면 fallback 문구 가능 |
| `categoryLabel` | `category` | 예: `Food`, `Free practice` |
| `learningPoint` | `point` | 학습 포인트 |
| `initialScore` | `score` | 평가 전에는 `0` 또는 앱에서 `null` 처리 전까지 `0` |
| `characters` | `characters` | 글자별 가이드 아이템 |

```json
{
  "sentenceId": 1,
  "source": "RECOMMENDED",
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싣게따.",
  "translation": "It looks delicious.",
  "categoryLabel": "Food",
  "learningPoint": "Final consonant linking and tense sound",
  "initialScore": 0,
  "characters": [
    {
      "position": 0,
      "text": "마",
      "pronunciationText": "마",
      "phonemes": ["ㅁ", "ㅏ"],
      "guideType": "MOUTH",
      "guideStatus": "AVAILABLE",
      "mouthGuideUrl": "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/vowel-a.png",
      "tongueGuideUrl": null,
      "note": "Stable vowel shape"
    }
  ]
}
```

## CharacterGuideItem

문장 준비 단계의 글자별 guide item이다. 평가 점수는 포함하지 않는다. 점수와 취약 글자 판단은 3.2의 `PracticeResult` 계약에서 다룬다.

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `position` | number | Y | 공백을 제외한 표시 글자 순서. 0부터 시작 |
| `text` | string | Y | 원문 기준 표시 글자 |
| `pronunciationText` | string | Y | 표준 발음 기준 표시 글자 |
| `phonemes` | string[] | Y | 가이드 생성을 위한 자모 배열 |
| `guideType` | GuideType | Y | 우선 표시할 가이드 종류 |
| `guideStatus` | GuideStatus | Y | 정적 가이드 사용 가능 여부 |
| `mouthGuideUrl` | string/null | Y | 입 모양 정적 가이드 URL |
| `tongueGuideUrl` | string/null | Y | 혀 위치 정적 가이드 URL |
| `note` | string | Y | 앱 카드에 바로 표시할 짧은 안내 문구 |

가이드 타입 결정 규칙 초안:

- 입 모양 URL만 있으면 `MOUTH`
- 혀 위치 URL만 있으면 `TONGUE`
- 둘 다 있으면 `BOTH`
- 둘 다 없으면 `NONE`, `guideStatus`는 `MISSING`

## GET /api/sentences/recommended

추천 문장 목록을 반환한다. 앱 Home의 `mockSentences`를 대체한다.

### Auth

- 내부 프로토타입: 불필요
- 공개 MVP: 불필요

### Query

| Name | Type | Required | Default | 설명 |
| --- | --- | --- | --- | --- |
| `limit` | number | N | `20` | 반환 개수 |
| `category` | string | N | 없음 | 예: `FOOD` |

### Status

- `200 OK`
- `400 Bad Request`: query parameter 형식 오류

### Response 200

```json
{
  "items": [
    {
      "sentenceId": 1,
      "source": "RECOMMENDED",
      "originalText": "맛있겠다.",
      "standardPronunciation": "마싣게따.",
      "translation": "It looks delicious.",
      "categoryLabel": "Food",
      "learningPoint": "Final consonant linking and tense sound",
      "initialScore": 0,
      "characters": [
        {
          "position": 0,
          "text": "마",
          "pronunciationText": "마",
          "phonemes": ["ㅁ", "ㅏ"],
          "guideType": "MOUTH",
          "guideStatus": "AVAILABLE",
          "mouthGuideUrl": "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/vowel-a.png",
          "tongueGuideUrl": null,
          "note": "Stable vowel shape"
        }
      ]
    }
  ]
}
```

## GET /api/sentences/{sentenceId}

추천 문장 하나를 조회한다. 추천 카드 선택 후 최신 prepare 상태를 다시 받을 때 사용한다.

### Auth

- 내부 프로토타입: 불필요
- 공개 MVP: 불필요

### Status

- `200 OK`
- `404 Not Found`: 문장이 존재하지 않음

### Response 200

`PracticeSentenceResponse` 단일 객체를 반환한다.

## POST /api/pronunciation/prepare

직접 입력 또는 추천 문장을 Practice 화면용 데이터로 준비한다. 표준 발음 변환, 표시 글자 분해, 정적 guide URL 조회를 수행한다.

### Auth

- 내부 프로토타입: 불필요
- 공개 MVP: 선택

인증 사용자가 호출하면 이후 평가/저장을 위해 `practiceToken`을 사용자와 연결할 수 있다. 익명 호출이면 `practiceToken`은 짧은 TTL을 가진 임시 토큰으로 취급한다.

### Request

직접 입력:

```json
{
  "source": "CUSTOM",
  "text": "한국어를 배우고 있어요."
}
```

추천 문장:

```json
{
  "source": "RECOMMENDED",
  "sentenceId": 1
}
```

### Request Validation

- `source=CUSTOM`이면 `text`는 필수이며 trim 후 1자 이상이어야 한다.
- `source=RECOMMENDED`이면 `sentenceId`는 필수다.
- MVP에서는 한 요청의 `text` 최대 길이를 100자로 제한한다.

### Status

- `200 OK`
- `400 Bad Request`: source별 필수값 누락 또는 형식 오류
- `404 Not Found`: 추천 문장이 존재하지 않음
- `413 Payload Too Large`: 입력 문장 길이 초과
- `422 Unprocessable Entity`: 표준 발음 변환 또는 guide mapping 실패

### Response 200

```json
{
  "practiceToken": "prep_20260601_000001",
  "sentence": {
    "sentenceId": null,
    "source": "CUSTOM",
    "originalText": "한국어를 배우고 있어요.",
    "standardPronunciation": "한구거를 배우고 이써요.",
    "translation": "Practice with your own sentence.",
    "categoryLabel": "Free practice",
    "learningPoint": "Linking across syllables",
    "initialScore": 0,
    "characters": [
      {
        "position": 0,
        "text": "한",
        "pronunciationText": "한",
        "phonemes": ["ㅎ", "ㅏ", "ㄴ"],
        "guideType": "TONGUE",
        "guideStatus": "AVAILABLE",
        "mouthGuideUrl": null,
        "tongueGuideUrl": "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/tongue/h.png",
        "note": "Focus on final consonant placement"
      }
    ]
  }
}
```

`practiceToken`은 이후 녹음 평가 요청에서 같은 prepare 결과를 참조하기 위한 임시 식별자다. DB 저장 모델이 확정되면 `practiceSessionId`로 대체하거나 병행한다.

## PracticeResultResponse

녹음 평가 완료 후 Result 화면에 표시할 데이터다. Flutter `ResultScreen`의 더미 총점, 점수 breakdown, 약한 글자 목록을 대체한다.

| API field | Flutter usage | 설명 |
| --- | --- | --- |
| `overallScore` | 상단 큰 점수 | Azure `pronunciationScore` 기준 총점 |
| `gradeLabel` | 점수 옆 라벨 | 예: `Excellent`, `Good`, `Needs work` |
| `summary` | 상단 설명 문구 | 가장 중요한 피드백 한 문장 |
| `scoreBreakdown.accuracyScore` | `ScoreBreakdown` | 음소/단어 정확도 |
| `scoreBreakdown.fluencyScore` | `ScoreBreakdown` | 유창성 |
| `scoreBreakdown.completenessScore` | `ScoreBreakdown` | 완성도 |
| `weakCharacters` | `Weak sounds` 리스트 | 기준 점수 미만인 글자별 결과 |
| `characters` | 전체 글자 결과 | 상세 화면이나 저장용 전체 결과 |

```json
{
  "practiceSessionId": 101,
  "practiceToken": "prep_20260601_000001",
  "sentenceId": 1,
  "originalText": "맛있겠다.",
  "standardPronunciation": "마싣게따.",
  "recognizedText": "마싣게따",
  "overallScore": 82,
  "gradeLabel": "Good",
  "summary": "Tense consonants and final consonant linking need attention.",
  "scoreBreakdown": {
    "accuracyScore": 84,
    "fluencyScore": 80,
    "completenessScore": 91
  },
  "weakCharacters": [
    {
      "position": 1,
      "text": "싣",
      "pronunciationText": "싣",
      "score": 68,
      "guideType": "TONGUE",
      "guideStatus": "AVAILABLE",
      "mouthGuideUrl": null,
      "tongueGuideUrl": "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/tongue/s.png",
      "note": "Keep the tongue closer for the sibilant sound"
    }
  ],
  "characters": [
    {
      "position": 0,
      "text": "마",
      "pronunciationText": "마",
      "score": 94,
      "guideType": "MOUTH",
      "guideStatus": "AVAILABLE",
      "mouthGuideUrl": "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/vowel-a.png",
      "tongueGuideUrl": null,
      "note": "Stable vowel shape"
    },
    {
      "position": 1,
      "text": "싣",
      "pronunciationText": "싣",
      "score": 68,
      "guideType": "TONGUE",
      "guideStatus": "AVAILABLE",
      "mouthGuideUrl": null,
      "tongueGuideUrl": "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/tongue/s.png",
      "note": "Keep the tongue closer for the sibilant sound"
    }
  ]
}
```

## ScoreBreakdown

Azure Speech 평가 결과를 앱 표시용 정수 점수로 반올림해서 내려준다.

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `accuracyScore` | number | Y | 발음 정확도. `AssessmentResult.accuracyScore` |
| `fluencyScore` | number | Y | 유창성. `AssessmentResult.fluencyScore` |
| `completenessScore` | number | Y | 완성도. `AssessmentResult.completenessScore` |

`overallScore`는 `AssessmentResult.pronunciationScore`를 사용한다. Azure 응답이 없거나 평가 실패 시 `0`을 임의로 내려주지 말고 에러 응답을 반환한다.

## CharacterEvaluationResult

평가 완료 후 글자별 점수와 가이드 연결 정보를 표현한다.

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `position` | number | Y | 공백을 제외한 표시 글자 순서 |
| `text` | string | Y | 원문 또는 표준 발음 기준 표시 글자 |
| `pronunciationText` | string | Y | 표준 발음 기준 표시 글자 |
| `score` | number | Y | 글자별 점수. MVP에서는 없으면 서버 규칙으로 추정하거나 약한 글자만 생성 |
| `guideType` | GuideType | Y | ResultTile과 GuideSheet에 표시할 가이드 종류 |
| `guideStatus` | GuideStatus | Y | 가이드 URL 사용 가능 여부 |
| `mouthGuideUrl` | string/null | Y | 입 모양 가이드 URL |
| `tongueGuideUrl` | string/null | Y | 혀 위치 가이드 URL |
| `note` | string | Y | ResultTile과 GuideSheet에 표시할 피드백 |

`weakCharacters`는 `characters` 중 `score < 80`인 항목을 기본으로 한다. 서버가 학습 정책상 다른 기준을 쓰는 경우에도 앱은 서버가 내려준 `weakCharacters`를 그대로 표시한다.

## POST /api/evaluations

녹음 파일을 평가하고 Result 화면용 응답을 반환한다.

### Auth

- 내부 프로토타입: 불필요
- 공개 MVP: 필요

공개 MVP에서는 평가 성공 시 quota를 차감한다. 내부 프로토타입에서 익명 호출을 허용하더라도 운영 환경에서는 비용 제어를 위해 인증을 요구한다.

### Request

`multipart/form-data`

| Part | Type | Required | 설명 |
| --- | --- | --- | --- |
| `audio` | file | Y | 사용자가 녹음한 음성 파일 |
| `practiceToken` | string | N | `prepare` 응답에서 받은 임시 식별자 |
| `sentenceId` | number | N | 추천 문장 기반 평가일 때 사용 |
| `text` | string | N | 직접 입력 기반 평가에서 `practiceToken`이 없을 때 사용 |

요청 기준:

- `practiceToken`, `sentenceId`, `text` 중 하나는 필요하다.
- `practiceToken`이 있으면 서버는 prepare 당시의 `originalText`와 `standardPronunciation`을 우선 사용한다.
- Azure Speech 평가의 기준 문장은 서버가 계산한 `standardPronunciation`이다.
- MVP 음성 포맷은 `wav`를 우선 지원한다.

### Status

- `200 OK`
- `400 Bad Request`: multipart part 누락 또는 기준 문장 식별자 누락
- `401 Unauthorized`: 공개 MVP에서 인증 누락 또는 access token 오류
- `404 Not Found`: `sentenceId` 또는 `practiceToken` 대상 없음
- `409 Conflict`: 이미 평가에 사용된 `practiceToken` 재사용
- `413 Payload Too Large`: 음성 파일 크기 제한 초과
- `415 Unsupported Media Type`: 지원하지 않는 음성 파일 형식
- `422 Unprocessable Entity`: 평가 가능한 한국어 기준 문장을 만들 수 없음
- `429 Too Many Requests`: 일일 평가 quota 초과
- `502 Bad Gateway`: Azure Speech 평가 실패
- `503 Service Unavailable`: 평가 기능 일시 비활성화

### Response 200

`PracticeResultResponse` 단일 객체를 반환한다.

## GET /api/evaluations/me

로그인 사용자의 평가 기록을 최신순으로 조회한다.

### Auth

- 내부 프로토타입: 필요
- 공개 MVP: 필요

### Query

| Name | Type | Required | Default | 설명 |
| --- | --- | --- | --- | --- |
| `page` | number | N | `0` | 0부터 시작하는 페이지 번호 |
| `size` | number | N | `20` | 페이지 크기. MVP 최대 `50` |

### Status

- `200 OK`
- `400 Bad Request`: paging parameter 형식 오류
- `401 Unauthorized`: 인증 누락 또는 access token 오류

### Response 200

```json
{
  "items": [
    {
      "practiceSessionId": 101,
      "sentenceId": 1,
      "source": "RECOMMENDED",
      "originalText": "맛있겠다.",
      "standardPronunciation": "마싣게따.",
      "recognizedText": "마싣게따",
      "overallScore": 82,
      "createdAt": "2026-06-16T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

상세 기록 화면이 필요해지면 `GET /api/evaluations/{practiceSessionId}`를 후속 계약으로 추가한다.

## GET /api/quota/today

로그인 사용자의 오늘 평가 가능 횟수와 보상권 상태를 조회한다.

### Auth

- 내부 프로토타입: 필요
- 공개 MVP: 필요

### Status

- `200 OK`
- `401 Unauthorized`: 인증 누락 또는 access token 오류

### Response 200

```json
{
  "date": "2026-06-17",
  "freeLimit": 5,
  "freeUsed": 2,
  "rewardedAvailable": 1,
  "remainingPractices": 4,
  "resetAt": "2026-06-18T00:00:00+09:00"
}
```

quota 차감은 `POST /api/evaluations`가 성공해 평가 결과를 만들었을 때만 수행한다. Azure 평가 실패, validation 실패, 파일 형식 오류는 quota를 차감하지 않는다.

## POST /api/auth/oauth/login

Google OAuth 또는 Apple Sign in 결과로 받은 provider token을 서버에서 검증하고 LingKo JWT를 발급한다.

### Auth

- 불필요

### Request

```json
{
  "provider": "GOOGLE",
  "idToken": "provider-id-token",
  "displayLanguage": "en",
  "nativeLanguage": "en"
}
```

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `provider` | string | Y | `GOOGLE` 또는 `APPLE` |
| `idToken` | string | Y | 앱이 provider에서 받은 ID token |
| `displayLanguage` | string | N | 앱 표시 언어. MVP 기본값 `en` |
| `nativeLanguage` | string | N | 사용자 모국어. MVP 기본값 `en` |

### Status

- `200 OK`
- `400 Bad Request`: provider 또는 token 형식 오류
- `401 Unauthorized`: provider token 검증 실패
- `502 Bad Gateway`: OAuth provider 연동 실패

### Response 200

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "name": "User",
    "profileImageUrl": "https://example.com/image.png",
    "displayLanguage": "en",
    "nativeLanguage": "en",
    "targetLevel": null
  }
}
```

## Error Response

모든 실패 응답은 아래 구조를 사용한다. 성공 응답에는 공통 envelope을 쓰지 않는다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "text is required for CUSTOM prepare request",
  "details": [
    {
      "field": "text",
      "reason": "must not be blank"
    }
  ],
  "requestId": "req_20260617_000001"
}
```

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `code` | string | Y | 앱 분기용 안정적인 에러 코드 |
| `message` | string | Y | 사용자에게 보여줄 수 있는 기본 영어 메시지 |
| `details` | array | Y | validation 상세. 없으면 빈 배열 |
| `requestId` | string/null | Y | 서버 로그 추적용 ID. 구현 전에는 `null` 허용 |

권장 코드와 상태:

| Code | Status | Flutter 처리 |
| --- | --- | --- |
| `INVALID_REQUEST` | `400` | 입력값 수정 안내 |
| `VALIDATION_FAILED` | `400` | 필드별 오류 표시 |
| `UNAUTHORIZED` | `401` | 로그인 화면 또는 token refresh |
| `FORBIDDEN` | `403` | 접근 불가 안내 |
| `SENTENCE_NOT_FOUND` | `404` | 추천 문장 새로고침 안내 |
| `PRACTICE_TOKEN_NOT_FOUND` | `404` | prepare 재시도 |
| `PRACTICE_TOKEN_CONFLICT` | `409` | 새 연습 세션 시작 |
| `PAYLOAD_TOO_LARGE` | `413` | 짧은 문장 또는 작은 파일 요청 |
| `AUDIO_FORMAT_UNSUPPORTED` | `415` | 지원 포맷 안내 |
| `UNSUPPORTED_TEXT` | `422` | 한국어 문장 입력 요청 |
| `GUIDE_MAPPING_FAILED` | `422` | 가이드 없이 학습 계속 또는 fallback 표시 |
| `QUOTA_EXCEEDED` | `429` | 광고 보상 또는 다음 날 재시도 안내 |
| `OAUTH_PROVIDER_FAILED` | `502` | 로그인 재시도 안내 |
| `EVALUATION_FAILED` | `502` | 녹음 평가 재시도 안내 |
| `EXTERNAL_SERVICE_UNAVAILABLE` | `503` | 잠시 후 재시도 안내 |

기존 초안 코드 중 다음 값은 위 표에 포함된 코드로 유지한다.

```text
INVALID_REQUEST
SENTENCE_NOT_FOUND
UNSUPPORTED_TEXT
GUIDE_MAPPING_FAILED
EVALUATION_FAILED
AUDIO_FORMAT_UNSUPPORTED
```

## Flutter Mapping Notes

현재 앱 모델에 바로 매핑할 때:

- `sentence.originalText` -> `PracticeSentence.text`
- `sentence.standardPronunciation` -> `PracticeSentence.pronunciation`
- `sentence.translation` -> `PracticeSentence.translation`
- `sentence.categoryLabel` -> `PracticeSentence.category`
- `sentence.learningPoint` -> `PracticeSentence.point`
- `sentence.initialScore ?? 0` -> `PracticeSentence.score`
- `sentence.characters[].text` -> `CharacterResult.character`
- 평가 전 `CharacterResult.score`는 `0`
- `sentence.characters[].note` -> `CharacterResult.note`
- `sentence.characters[].guideType` -> `CharacterResult.kind`

추후 앱 모델을 정리할 때는 `score`를 prepare 모델에서 제거하고, `CharacterResult`를 `PreparedCharacterGuide`와 `EvaluatedCharacterResult`로 분리하는 편이 더 명확하다.

## PracticeResult Mapping Notes

Result 화면 모델을 추가할 때:

- `overallScore` -> Result 상단 큰 점수
- `gradeLabel` -> 점수 옆 상태 라벨
- `summary` -> 점수 아래 설명 문구
- `scoreBreakdown.accuracyScore` -> `ScoreBreakdown`의 Accuracy
- `scoreBreakdown.fluencyScore` -> `ScoreBreakdown`의 Fluency
- `scoreBreakdown.completenessScore` -> `ScoreBreakdown`의 Completeness
- `weakCharacters[].text` -> `CharacterResult.character`
- `weakCharacters[].score` -> `CharacterResult.score`
- `weakCharacters[].note` -> `CharacterResult.note`
- `weakCharacters[].guideType` -> `CharacterResult.kind`
- `weakCharacters[].mouthGuideUrl`, `tongueGuideUrl`, `guideStatus` -> 추후 `GuideSheet` 이미지/영상 표시용 필드
