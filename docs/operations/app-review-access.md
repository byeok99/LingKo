# App Review 접근 Runbook

## 목적과 경계

소셜 로그인 계정과 2단계 인증을 심사자에게 공유하지 않고도 실제 Backend 기능을 확인할 수 있게
전용 review 계정 세션을 발급합니다. 앱에는 계정 비밀번호, 접근 코드, 고정 JWT가 들어가지 않습니다.
기능은 기본 비활성화이며 심사 기간에만 운영 Secret으로 엽니다.
운영 전송 구간은 반드시 HTTPS여야 하며 reverse proxy와 APM의 request body logging을 끕니다.

로그인 화면의 `LingKo` wordmark를 3초 안에 5회 탭하면 코드 입력창이 나타납니다. 입력한 원문은
`POST /api/auth/review/login` 요청에만 사용되고 로컬 설정에 저장되지 않습니다. Backend는 SHA-256
hash를 상수 시간 비교한 뒤 미리 저장된 review 사용자에게 일반 로그인과 동일한 폐기 가능 세션을
발급합니다.

## 제출 전 준비

1. 운영 환경에서 전용 Google 또는 Apple 계정으로 한 번 로그인해 `users.user_idx`를 확인합니다.
2. 개인 데이터가 아닌 심사용 sample 기록을 준비하고 현재 약관 버전 동의를 완료합니다.
3. 4~128자 접근 코드를 정하고 원문은 password manager와 App Store Connect Review Notes에만 보관합니다. 4자리 코드는 오직 짧은 심사 기간에만 활성화하고 심사 직후 폐기합니다.
4. 원문을 노출하지 않는 로컬 입력으로 SHA-256 hex를 계산합니다.

```bash
read -s LINGKO_REVIEW_CODE_INPUT
printf '%s' "$LINGKO_REVIEW_CODE_INPUT" | shasum -a 256
unset LINGKO_REVIEW_CODE_INPUT
```

5. 운영 Secret Manager에 다음 값을 설정하고 Backend를 재배포합니다.

```env
REVIEW_ACCESS_ENABLED=true
REVIEW_ACCESS_CODE_SHA256=<64자리 SHA-256 hex>
REVIEW_ACCESS_USER_ID=<기존 review 사용자 ID>
REVIEW_ACCESS_MAX_ATTEMPTS=5
REVIEW_ACCESS_WINDOW_SECONDS=300
```

6. 제출할 동일 build에서 4회 탭은 아무 동작이 없고, 5회 탭 후 올바른 코드만 로그인되는지 확인합니다.
7. 잘못된 코드가 `401`, 제한 초과가 `Retry-After`를 포함한 `429`인지 확인합니다.

애플리케이션 제한은 process별·서버가 관찰한 원격 주소별입니다. 여러 Backend instance를 운영하거나
reverse proxy가 모든 요청을 같은 주소로 전달한다면 gateway에서 신뢰할 client IP 기준의 공유 Rate
Limit을 추가하고 request body logging을 끕니다.

## App Store Connect Review Notes

아래 영문을 Review Notes에 붙여 넣고 대괄호 부분만 실제 심사 정보로 교체합니다. 원문 접근 코드는
GitHub Issue, PR, 문서 파일에 넣지 않습니다.

```text
App Review access

The app normally supports Google Sign-In and Sign in with Apple. A dedicated
review account is available without requiring the reviewer to use external
social credentials or two-factor authentication.

1. Launch the app and remain on the sign-in screen.
2. Tap the “LingKo” wordmark five times within three seconds.
3. Enter this review access code: [REVIEW ACCESS CODE]
4. Tap “Continue.” The app signs in to a preconfigured review account with
   sample data and access to the submitted app features.

The access code is valid until [EXPIRATION DATE AND TIME, INCLUDING TIME ZONE].
If access fails, contact [REVIEW CONTACT NAME / EMAIL / PHONE].
```

## 심사 완료 후

1. `REVIEW_ACCESS_ENABLED=false`로 재배포합니다.
2. review 사용자의 활성 세션을 운영 DB에서 폐기합니다.

```sql
UPDATE auth_refresh_sessions
SET revoked_at = CURRENT_TIMESTAMP(6)
WHERE user_idx = <REVIEW_ACCESS_USER_ID>
  AND revoked_at IS NULL;
```

3. 사용한 접근 코드를 password manager와 Review Notes에서 폐기하고 다음 제출 때 새 코드를 만듭니다.
4. review 계정의 sample 데이터와 정상 로그인 가능 여부를 다음 제출 전에 다시 확인합니다.

비활성화만 하면 새 로그인은 막히지만 이미 발급된 Access Token은 세션 폐기 전까지 유효할 수 있으므로
Backend 설정 변경과 세션 폐기를 함께 수행합니다.
