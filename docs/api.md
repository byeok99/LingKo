# LingKo API Contract

작성 기준: 2026-06-01

이 문서는 Flutter 앱의 `PracticeSentence` 모델과 백엔드 DTO가 같은 필드 계약을 쓰도록 맞추기 위한 초안이다. 현재 구현된 API는 `POST /api/pronunciation/convert`뿐이며, 아래 `sentences`와 `prepare` API는 다음 구현 작업의 목표 계약이다.

## 공통 규칙

- JSON 필드는 `camelCase`를 사용한다.
- 시간 값은 ISO-8601 문자열을 사용한다.
- 점수는 평가 완료 전에는 `null`을 사용한다.
- 앱 표시용 문장 준비 응답과 실제 평가 결과 응답은 분리한다.
- `PracticeSentence` 화면 모델은 `PracticeSentenceResponse` 또는 `PronunciationPrepareResponse`에서 만든다.

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

### Query

| Name | Type | Required | Default | 설명 |
| --- | --- | --- | --- | --- |
| `limit` | number | N | `20` | 반환 개수 |
| `category` | string | N | 없음 | 예: `FOOD` |

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

### Response 200

`PracticeSentenceResponse` 단일 객체를 반환한다.

## POST /api/pronunciation/prepare

직접 입력 또는 추천 문장을 Practice 화면용 데이터로 준비한다. 표준 발음 변환, 표시 글자 분해, 정적 guide URL 조회를 수행한다.

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

## POST /api/pronunciation/evaluate

녹음 파일을 평가하고 Result 화면용 응답을 반환한다.

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
- MVP 음성 포맷은 `wav`를 우선 지원한다.

### Response 200

`PracticeResultResponse` 단일 객체를 반환한다.

## Error Response

```json
{
  "code": "INVALID_REQUEST",
  "message": "text is required for CUSTOM prepare request",
  "details": []
}
```

권장 코드:

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
