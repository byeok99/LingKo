# API 레퍼런스

기본 주소는 로컬 기준 `http://localhost:8080`입니다. JSON 응답은 UTF-8을 사용하며 인증 API는 `Authorization: Bearer <access-token>` 헤더를 사용합니다.

## 인증

### `POST /api/auth/oauth/login`

Google ID Token을 검증하고 LingKo JWT를 발급합니다.

```json
{
  "provider": "GOOGLE",
  "idToken": "..."
}
```

응답 필드:

```text
tokenType, accessToken, refreshToken, expiresInSeconds, user
```

### `POST /api/auth/token/refresh`

현재 Refresh Token을 검증하고 새로운 Access/Refresh Token 쌍으로 회전합니다.

```json
{
  "refreshToken": "..."
}
```

응답 필드는 OAuth 로그인 응답과 같습니다. 만료·폐기·재사용 토큰은 `401 AUTHENTICATION_FAILED`를 반환합니다.

### `POST /api/auth/logout`

현재 기기의 Refresh Token 세션을 폐기합니다.

```json
{
  "refreshToken": "..."
}
```

성공하면 본문 없이 `204 No Content`를 반환합니다.

### `DELETE /api/auth/account`

현재 Access Token의 사용자와 현재 Refresh Token의 세션 소유자가 같은지 재확인한 뒤 계정과 사용자 소유 데이터를 삭제합니다.

```http
Authorization: Bearer <access-token>
```

```json
{
  "refreshToken": "..."
}
```

성공하면 본문 없이 `204 No Content`를 반환하고 앱은 로컬 토큰을 제거합니다. 서버는 `evaluation-audio/{userId}/`의 현재 object, 과거 version과 delete marker를 먼저 삭제한 뒤 Refresh 세션, 평가 작업·기록, 쿼터와 사용자 프로필을 삭제합니다. S3 정리가 실패하면 DB 계정과 로컬 세션을 보존하고 `503 ACCOUNT_DELETION_UNAVAILABLE`을 반환하므로 사용자는 동일 세션으로 재시도할 수 있습니다.

## 문장

### `GET /api/sentences/recommended`

쿼리:

- `limit`: 기본 20, 범위 1~50
- `category`: 선택

### `GET /api/sentences/{sentenceId}`

추천 문장 단건을 조회합니다.

추천 문장 응답의 `standardPronunciation`은 DB에 저장한 정답을 반환하지 않습니다. 서버가 `originalText`를 Unicode 문장부호·기호 제거와 공백 정규화한 뒤 현재 한국어 음운 규칙으로 매 요청 계산합니다.

## 발음 준비

### `POST /api/pronunciation/convert`

```json
{
  "text": "같이 먹어요"
}
```

원문과 표준 발음을 반환합니다.

`text`는 Unicode 문장부호·기호를 제거하고 연속 공백을 하나로 합친 뒤 변환합니다. 정규화 결과가 비어 있으면 `400 INVALID_REQUEST`를 반환합니다.

### `POST /api/pronunciation/prepare`

현재 자유 문장 요청만 지원합니다.

```json
{
  "source": "CUSTOM",
  "text": "같이 먹어요"
}
```

응답에는 문장 원문, 표준 발음, 번역·학습 정보, 글자별 가이드가 포함됩니다.

`mouthGuideUrl`과 `tongueGuideUrl`은 제공된 매체 URL입니다. 문장 준비 응답은 빠른 입력 피드백을 위해 정적 PNG mapping을 반환합니다. 평가가 완료되면 글자 점수 제공 여부와 관계없이 프레임 전환이 필요한 모든 음절의 입·혀 가이드를 Worker가 MP4로 생성하고, 앱은 영상 확장자를 영상으로 재생합니다. 단일 프레임이거나 영상 생성에 실패하면 첫 PNG를 정적 fallback으로 표시합니다. 동일 음절·가이드 종류·프레임 조합의 MP4는 결정적 S3 key로 재사용합니다.

## 발음 평가

### `POST /api/evaluations/uploads`

인증 필요. 앱이 API 서버를 거치지 않고 비공개 S3에 직접 PUT할 URL을 발급합니다.

```json
{
  "fileName": "recording.wav",
  "contentType": "audio/wav",
  "contentLength": 32044
}
```

응답 `201 Created`:

```json
{
  "objectKey": "evaluation-audio/7/uuid.wav",
  "uploadUrl": "https://signed-s3-url",
  "expiresAt": "2026-07-27T01:10:00Z"
}
```

앱은 `uploadUrl`에 `Content-Type: audio/wav`와 발급 요청과 같은 길이로 WAV를 PUT합니다. URL 전체는 로그에 기록하지 않습니다.

### `POST /api/evaluations/jobs`

인증과 `Idempotency-Key` header가 필요합니다.

```json
{
  "objectKey": "evaluation-audio/7/uuid.wav",
  "sentenceId": 12
}
```

자유 문장은 `sentenceId` 대신 `text`를 사용합니다. 서버는 object 소유권과 metadata를 확인한 뒤 쿼터 예약과 `PENDING` 작업 생성을 하나의 transaction으로 처리합니다.

응답 `202 Accepted`:

```json
{
  "jobId": "uuid",
  "status": "PENDING",
  "result": null,
  "errorCode": null,
  "createdAt": "2026-07-27T01:00:00Z",
  "updatedAt": "2026-07-27T01:00:00Z"
}
```

동일 사용자·동일 Key·동일 payload 재호출은 기존 작업을 반환하며 다른 payload는 `409 IDEMPOTENCY_CONFLICT`입니다. 성공·최종 실패 작업은 완료 시점부터 기본 7일 동안 이 응답 재사용을 위해 보존합니다. 보존 기간이 지난 작업은 설정된 batch 단위로 삭제되며, `PENDING`·`PROCESSING` 작업은 자동 정리 대상이 아닙니다.

### `GET /api/evaluations/jobs/{jobId}`

인증 필요. 상태는 `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`입니다. 성공 시 `result`에 기존 평가 결과 계약을 반환합니다.

대표 성공 결과:

```json
{
  "jobId": "uuid",
  "status": "SUCCEEDED",
  "result": {
    "overallScore": 82,
    "gradeLabel": "Good",
    "summary": "...",
    "recognizedText": "...",
    "characterScoreStatus": "UNAVAILABLE",
    "wordScoreStatus": "AVAILABLE",
    "scoreBreakdown": {
      "accuracy": 84,
      "fluency": 80,
      "completeness": 83
    },
    "weakCharacters": [],
    "characters": [],
    "words": [
      {
        "position": 0,
        "text": "김치찌개",
        "score": 82,
        "scoreStatus": "AVAILABLE",
        "syllables": [
          {
            "position": 0,
            "text": "김",
            "score": null,
            "scoreStatus": "UNAVAILABLE",
            "mouthGuideUrl": "...",
            "tongueGuideUrl": "..."
          }
        ]
      }
    ]
  },
  "errorCode": null
}
```

`words[].score`는 Azure detailed 결과의 단어 수·정규화 텍스트·위치가 기준 문장과 모두 일치할 때만 제공됩니다. `words[].syllables`는 입·혀 가이드 탐색 단위이며 단어 점수를 복제하지 않으므로 `score`가 `null`입니다. 단어 정렬을 신뢰할 수 없으면 `wordScoreStatus`와 각 단어의 `scoreStatus`는 `UNAVAILABLE`입니다.

기존 `POST /api/evaluations` multipart endpoint는 기본 비활성화되며 임시 호환이 필요할 때만 `EVALUATION_LEGACY_MULTIPART_ENABLED=true`로 활성화합니다.

## 사용자 연습 기록

### `GET /api/evaluations/me`

인증 필요.

쿼리:

- `page`: 기본 0, 0 이상
- `size`: 기본 10, 범위 1~50

각 `items[]`는 평가 당시 저장한 `words[]`와 그 하위 `syllables[]`를 반환합니다. 신규 기록은 단어 점수를 snapshot으로 보존하며, 단어 snapshot이 없는 과거 기록은 표준 발음의 공백과 저장된 음절 순서로 점수 없는 그룹을 복원합니다.

## 발음 평가 기회

### `GET /api/quota/today`

인증 필요. endpoint 경로는 호환성을 위해 유지하며 현재·최대 평가 기회와 서버 기준 다음 자연 충전 시각을 반환합니다. 과거 자정 초기화용 `resetAt`은 응답 계약에서 제거했습니다.

```json
{
  "date": "2026-08-03",
  "freeLimit": 5,
  "freeUsed": 2,
  "rewardedAvailable": 0,
  "remainingPractices": 3,
  "nextRefillAt": "2026-08-03T17:30:00+09:00",
  "serverTime": "2026-08-03T16:47:42+09:00"
}
```

평가 진행 중 예약된 횟수는 `remainingPractices`에서 즉시 제외되고 최초 예약 시 1시간 timer가 시작됩니다. 평가 성공 시 사용량으로 확정되고 시스템 오류 시 복구되며, 다시 최대치가 되면 timer도 제거됩니다. 자연 충전은 API 접근 시 서버가 경과 구간을 계산하는 lazy refill 방식입니다.

광고 SDK와 서버 보상 검증 endpoint는 아직 연결되지 않았습니다. 앱은 5개 미만에서 `+` 버튼과 외부 callback 경계만 제공하며 자체적으로 성공이나 횟수 증가를 만들지 않습니다.

## 사용자 설정

### `GET /api/users/me/preferences`

인증 필요. 표시 언어와 모국어를 반환합니다.

### `PATCH /api/users/me/preferences`

인증 필요.

```json
{
  "displayLanguage": "en",
  "nativeLanguage": "ko"
}
```

## 가이드 생성 작업

### `POST /api/pronunciation/guide-jobs`

현재 인증되지 않은 비동기 작업 생성 API입니다.

```json
{
  "syllable": "가",
  "type": "MOUTH",
  "urlPairs": [
    ["https://.../start.png", "https://.../end.png"]
  ]
}
```

성공 시 `202 Accepted`와 작업 ID, 상태, 캐시 키를 반환합니다.

### `GET /api/pronunciation/guide-jobs/{jobId}`

상태 후보:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

작업 상태는 현재 서버 프로세스 메모리에만 존재합니다.

## 공통 오류 형식

```json
{
  "code": "INVALID_REQUEST",
  "message": "...",
  "fieldErrors": []
}
```

전체 코드는 [오류 코드](error-codes.md)를 참고합니다.

## 계약 변경 규칙

- 필드 제거·이름 변경은 호환성 깨짐으로 취급합니다.
- 앱과 백엔드 변경을 같은 PR 또는 순서가 보장된 PR로 배포합니다.
- 인증 여부, 최대 크기, 페이지 범위, 오류 코드를 변경하면 이 문서를 함께 갱신합니다.
- 운영 전에는 OpenAPI 자동 생성 도입을 권장합니다.
