# 로컬 개발

Java 21, Flutter SDK, MySQL과 필요한 외부 서비스 개발용 설정을 준비합니다. 환경변수 이름과 기본값은 [백엔드 설정 예시](../../backend/.env.example)를 기준으로 합니다. 실제 비밀값을 커밋하지 않습니다.

## 백엔드

`backend/`에서 실행합니다.

```bash
./gradlew bootRun
```

DB와 외부 서비스 설정을 실행 환경에 전달해야 합니다. `bootRun`이 `.env` 파일을 자동으로 읽는다고 가정하지 않습니다. Compose 구성은 [백엔드 디렉터리](../../backend/)에서 확인할 수 있으며 API와 독립 Worker의 역할을 구분합니다.

가이드 영상 cache 범위는 외부 호출 없이 확인할 수 있습니다.

```bash
./gradlew guidePrewarm
```

실제 사전 생성은 Gradle이 `.env`를 자동으로 읽지 않으므로 현재 shell에 필요한 AWS·Replicate 설정을 export하거나 `guide-prewarm` Compose profile을 사용하고 `--prewarm-dry-run=false`를 명시합니다. 먼저 작은 `--prewarm-limit`으로 S3 쓰기·FFmpeg·Replicate 연결을 확인하고, 성공한 범위는 `--prewarm-offset`으로 건너뛰어 재개합니다. 명령과 옵션은 [백엔드 README](../../backend/README.md#가이드-미디어-사전-생성)를 참고합니다.

## 앱

iOS Apple 로그인은 Runner의 Sign in with Apple entitlement와 서명 profile/App ID의
동일 capability가 필요합니다. Backend `APPLE_CLIENT_ID`는 앱 Bundle ID와 일치해야 하며
기본값은 `com.byeok.lingko`입니다. 빈 환경변수는 startup validation에 실패합니다.

AI 평가에는 `/api/legal/ai-consent`의 현행 고지 허용이 필요합니다. 신규 V23 migration을
API와 Worker보다 먼저 적용하고 두 프로세스 모두 같은 버전으로 실행합니다. 일반 약관과
AI 고지 버전은 `2026-09-12`이며 기존 계정의 일반 동의를 AI 허용으로 복사하지 않습니다.

`app/`에서 실행합니다.

```bash
flutter pub get
flutter run --dart-define=LINGKO_API_BASE_URL=http://localhost:8080
```

API 주소는 실행 기기에서 접근 가능해야 합니다. Android emulator의 호스트 주소는 `10.0.2.2`이며 실제 기기는 개발 PC의 접근 가능한 주소를 사용합니다. 녹음 권한과 소셜 로그인은 플랫폼별 설정이 필요합니다. 개발 요청을 운영 서버로 보내지 않도록 API 주소를 명시합니다.

광고가 포함된 로컬 실행은 `app/.env.example`을 `app/.env.local`로 복사한 뒤 플랫폼별
Rewarded·Review banner·Profile banner Ad Unit ID를 설정하고 아래 script를 사용합니다.

```bash
./scripts/run-local.sh ios
./scripts/run-local.sh android
```

debug build는 Google 공식 test Ad Unit을 사용합니다. iOS release는 AdMob의 공용
`IOS_BANNER`를 Review·Profile 기본값으로 사용합니다. 다음 `--dart-define`은 Android 운영
ID를 설정하거나 iOS 화면별 성과를 분리할 때 기본값을 덮어씁니다. 운영 ID가 없는
플랫폼·화면은 배너 요청이 비활성화됩니다.

- `ADMOB_ANDROID_REVIEW_BANNER_AD_UNIT_ID`
- `ADMOB_ANDROID_PROFILE_BANNER_AD_UNIT_ID`
- `ADMOB_IOS_REVIEW_BANNER_AD_UNIT_ID`
- `ADMOB_IOS_PROFILE_BANNER_AD_UNIT_ID`

`ADMOB_TEST_DEVICE_ID`는 로컬 실기기 test mode에만 사용합니다. 광고 요청 전 UMP 상태를
갱신하고 `canRequestAds`가 허용한 경우에만 SDK를 초기화합니다. AdMob Privacy & messaging에서
필요한 메시지와 iOS IDFA message를 구성하고, App Store Connect의 개인정보 공개 내용도 실제
광고 데이터 처리와 일치하는지 release 제출 전에 확인합니다.

[테스트 구성](testing-and-troubleshooting.md) · [API 계약](../api/api-reference.md)
