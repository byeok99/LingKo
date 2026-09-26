# LingKo App

Flutter 기반 한국어 발음 학습 모바일 앱입니다. 화면과 API client, 응답 모델, 녹음·TTS·인증 같은 기기 경계를 나누어 관리합니다.

## 사용자 흐름

가입 동의·로그인 → 서버 동의 기록 및 현재 버전 확인 → 문장 선택 또는 직접 입력 → 발음 가이드·TTS → 녹음 → 업로드와 평가 작업 생성 → 상태 조회 → 결과·재연습.

Home은 추천·저장 문장, Review는 학습 기록, Profile은 계정과 법무 문서 접근을 제공합니다. 단어 점수와 음절 가이드는 다른 정보이며 실제 음절 점수가 없는 경우 0점으로 대체하지 않습니다.

## 주요 디렉터리

- `lib/api`: 인증·문장·평가·쿼터 등 서버 계약
- `lib/app`: 앱 구성과 테마
- `lib/models`: 응답 모델
- `lib/screens`: 화면과 사용자 흐름
- `lib/services`: 녹음, TTS, 로그인, 보안 저장소
- `lib/widgets`: 재사용 UI
- `test`: widget·API·상태 관련 테스트

## 실행과 검증

```bash
flutter pub get
flutter run --dart-define=LINGKO_API_BASE_URL=http://localhost:8080
flutter analyze
flutter test
```

로컬 주소는 실행 기기에 맞게 설정해야 합니다. Android emulator는 호스트 접근 시 `10.0.2.2`를 사용하고 실제 기기는 접근 가능한 개발 서버 주소가 필요합니다. 소셜 로그인과 녹음은 플랫폼별 설정·권한도 필요합니다.

## 광고 설정

Review의 추세와 최근 기록 사이, Profile의 계정 정보와 `Your content` 사이에는
inline adaptive banner를 표시합니다. iOS release는 AdMob의 `IOS_BANNER`를 두 화면의
기본 Ad Unit으로 사용합니다. 화면별 성과를 분리하려면 아래 값을 각각 주입해 덮어쓸 수 있습니다.

```text
ADMOB_ANDROID_REVIEW_BANNER_AD_UNIT_ID
ADMOB_ANDROID_PROFILE_BANNER_AD_UNIT_ID
ADMOB_IOS_REVIEW_BANNER_AD_UNIT_ID
ADMOB_IOS_PROFILE_BANNER_AD_UNIT_ID
```

값은 `.env.example`을 복사한 `.env.local`에 설정하고 `./scripts/run-local.sh ios` 또는
`./scripts/run-local.sh android`로 실행합니다. debug build는 Google 공식 test Ad Unit을
사용합니다. iOS release는 저장소의 공개 `IOS_BANNER` ID를 기본으로 사용하고, Android 또는
분리된 iOS Ad Unit이 필요하면 배포용 `flutter build`에 같은 이름을 `--dart-define`으로
명시합니다. 운영 ID가 없는 플랫폼·화면은 배너를 요청하지 않습니다.

광고 요청 전 UMP 동의 상태를 매 launch 갱신하며, 필요한 지역에서는 Profile의
`Advertising privacy`에서 선택을 다시 열 수 있습니다. iOS ATT/IDFA message를 사용할 때는
AdMob의 Privacy & messaging 설정도 활성화되어 있어야 합니다.

[로컬 개발](../docs/development/local-development.md) · [인증 흐름](../docs/architecture/authentication-flow.md) · [API 계약](../docs/api/api-reference.md)
