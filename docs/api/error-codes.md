# API 오류 코드

오류 응답의 기본 구조는 다음과 같습니다.

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "fieldErrors": [
    {"field": "text", "message": "must not be blank"}
  ]
}
```

| HTTP | 코드 | 발생 조건 | 앱 처리 권장 |
|---:|---|---|---|
| 400 | `VALIDATION_FAILED` | DTO 필수값·형식 누락 | 해당 입력 또는 녹음 상태 안내 |
| 400 | `INVALID_REQUEST` | JSON 파싱, 쿼리 범위, S3 객체 소유권·메타데이터 불일치, 동의 문서 버전이 서버 현재 버전과 다르거나 필수 동의가 누락된 경우 | 사용자 입력 또는 업로드 상태 확인. 동의 관련이면 최신 문서 버전을 다시 받아 동의 화면을 표시 |
| 400 | `INVALID_AD_REWARD_CALLBACK` | AdMob SSV 서명 또는 지급 정책 불일치 | 앱에서 호출하지 않음. 서버 보안 로그와 AdMob 설정 확인 |
| 401 | `AUTHENTICATION_FAILED` | Bearer 토큰 누락·만료·검증 실패 | 세션 갱신 또는 재로그인 |
| 403 | `GUIDE_JOB_FORBIDDEN` | 일반 사용자 토큰으로 내부 가이드 작업 요청 | 앱에서는 요청하지 않고 내부 도구 권한 확인 |
| 404 | `SENTENCE_NOT_FOUND` | 추천 문장 ID가 없거나 비활성 | 목록 재조회 |
| 404 | `GUIDE_JOB_NOT_FOUND` | 작업 ID 없음 또는 서버 재시작 후 상태 소실 | 작업 재생성 안내 |
| 404 | `EVALUATION_JOB_NOT_FOUND` | 작업 ID가 없거나 다른 사용자 소유 | 작업 생성 상태 확인 |
| 404 | `AD_REWARD_SESSION_NOT_FOUND` | 보상 token이 없거나 다른 사용자 소유 | 새 광고 보상 session 생성 |
| 410 | 응답 본문 없음 | 폐기된 client 직접 광고 지급 endpoint 호출 | 앱을 SSV session 계약으로 업데이트 |
| 409 | `IDEMPOTENCY_CONFLICT` | 같은 Idempotency Key를 다른 평가 요청에 재사용 | 새 키로 새 작업 생성 |
| 413 | `AUDIO_TOO_LARGE` | 업로드 크기 10MiB 초과 | 더 짧게 재녹음 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | WAV가 아닌 파일 | WAV 재녹음 |
| 415 | `INVALID_WAV` | PCM·채널·비트·헤더 불일치 | 녹음 서비스 설정 확인 |
| 429 | `QUOTA_EXCEEDED` | 무료·보상 횟수 모두 소진 | 리셋 시각 또는 보상 흐름 표시 |
| 429 | `GUIDE_JOB_RATE_LIMITED` | 내부 호출자의 분당 생성 요청 한도 초과 | `Retry-After` 이후 재시도 |
| 429 | `GUIDE_JOB_CAPACITY_EXCEEDED` | 허용된 동시 가이드 생성 슬롯 사용 중 | `Retry-After` 이후 상태 확인·재시도 |
| 502 | `EVALUATION_FAILED` | 음성 평가 또는 영상 처리 외부 연동 실패 | 잠시 후 재시도 |
| 503 | `ACCOUNT_DELETION_UNAVAILABLE` | 회원 탈퇴 중 S3 음성 삭제 실패 | 로그인 상태를 유지하고 잠시 후 재시도 |
| 503 | `AD_REWARD_UNAVAILABLE` | SSV allowlist 미설정, 최대 quota 또는 Google 공개키 일시 실패 | 설정 확인 또는 잠시 후 재시도 |
| 500 | `INTERNAL_SERVER_ERROR` | 처리되지 않은 서버 오류 | 일반 오류와 문의 경로 표시 |

## 앱 처리 원칙

- HTTP 상태만 보지 말고 `code`를 우선 분기합니다.
- 서버의 내부 예외 메시지를 사용자에게 그대로 노출하지 않습니다.
- 재시도 가능한 오류와 입력 수정이 필요한 오류를 구분합니다.
- 401 발생 시 무한 재로그인·재요청 루프를 만들지 않습니다.
- 429의 경우 서버가 제공하는 `nextRefillAt`과 `serverTime`의 차이로 다음 충전 countdown을 표시합니다.
- 평가 작업 API가 202를 반환한 뒤에는 `PENDING`·`PROCESSING`을 폴링하고, `FAILED.errorCode`를 사용자용 메시지로 매핑합니다.
- 동의 상태 조회·기록이 실패하면 Home으로 진입시키지 않고 동의 화면을 유지합니다. 실패를 통과로 해석하면 동의 없이 서비스를 쓰게 됩니다.
- `GET /legal/{document}`는 오류 응답 구조를 쓰지 않습니다. 브라우저가 여는 HTML 페이지이며 알 수 없는 문서는 본문 없는 `404`입니다.

## 운영 원칙

- 새로운 오류 코드는 기존 코드와 의미가 겹치지 않게 추가합니다.
- 같은 실패 조건에서 API마다 다른 코드를 사용하지 않습니다.
- 로그에는 요청 ID와 오류 코드만 남기고 토큰·음성 데이터·비밀값은 남기지 않습니다.
