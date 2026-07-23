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

## 문장

### `GET /api/sentences/recommended`

쿼리:

- `limit`: 기본 20, 범위 1~50
- `category`: 선택

### `GET /api/sentences/{sentenceId}`

추천 문장 단건을 조회합니다.

## 발음 준비

### `POST /api/pronunciation/convert`

```json
{
  "text": "같이 먹어요"
}
```

원문과 표준 발음을 반환합니다.

### `POST /api/pronunciation/prepare`

현재 자유 문장 요청만 지원합니다.

```json
{
  "source": "CUSTOM",
  "text": "같이 먹어요"
}
```

응답에는 문장 원문, 표준 발음, 번역·학습 정보, 글자별 가이드가 포함됩니다.

## 발음 평가

### `POST /api/evaluations`

`multipart/form-data`

| 필드 | 필수 | 설명 |
|---|---:|---|
| `audio` | 예 | 10MiB 이하 16-bit mono PCM WAV |
| `sentenceId` | 조건부 | 추천 문장 ID |
| `text` | 조건부 | 자유 문장 원문 |

`sentenceId`와 `text` 중 하나는 필요합니다.

대표 응답:

```json
{
  "overallScore": 82,
  "gradeLabel": "Good",
  "summary": "...",
  "recognizedText": "...",
  "characterScoreStatus": "AVAILABLE",
  "scoreBreakdown": {
    "accuracy": 84,
    "fluency": 80,
    "completeness": 83
  },
  "weakCharacters": [],
  "characters": []
}
```

## 사용자 연습 기록

### `GET /api/evaluations/me`

인증 필요.

쿼리:

- `page`: 기본 0, 0 이상
- `size`: 기본 10, 범위 1~50

## 일일 쿼터

### `GET /api/quota/today`

인증 필요. Asia/Seoul 기준 오늘의 무료·보상·잔여 횟수와 다음 갱신 시각을 반환합니다.

현재 평가 업로드와 쿼터 차감은 연결되지 않았습니다.

## 사용자 설정

### `GET /api/users/me/preferences`

인증 필요. 표시 언어, 모국어, 목표 레벨을 반환합니다.

### `PATCH /api/users/me/preferences`

인증 필요.

```json
{
  "displayLanguage": "en",
  "nativeLanguage": "ko",
  "targetLevel": "BEGINNER_2"
}
```

지원 레벨:

- `BEGINNER_1`
- `BEGINNER_2`
- `INTERMEDIATE_1`
- `INTERMEDIATE_2`
- `ADVANCED`

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
