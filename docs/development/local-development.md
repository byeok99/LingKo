# 로컬 개발 가이드

## 사전 요구사항

- Java 21
- Flutter SDK와 Dart 3.7 이상
- Android Studio 또는 Xcode
- Docker Desktop 또는 Docker Engine + Compose
- Git
- 외부 영상 생성 테스트 시 FFmpeg

## 백엔드 실행

```bash
cd backend
cp .env.example .env
cp application.example.yaml src/main/resources/application.yaml
```

### 주요 환경변수

| 변수 | 설명 | 기본/비고 |
|---|---|---|
| `APP_PORT` | 백엔드 포트 | 8080 |
| `DB_HOST` | MySQL 호스트 | Docker에서는 `mysql` |
| `DB_PORT` | MySQL 포트 | 3306 |
| `DB_NAME` | DB 이름 | `lingko` |
| `DB_USER` | DB 사용자 | `lingko` |
| `DB_PASSWORD` | DB 비밀번호 | 필수 |
| `JPA_DDL_AUTO` | Hibernate DDL | `none` 권장 |
| `JWT_SECRET_KEY` | JWT 서명 키 | 필수, 충분히 긴 랜덤값 |
| `JWT_ACCESS_TOKEN_EXPIRE_MINUTES` | Access Token 만료 | 30 |
| `JWT_REFRESH_TOKEN_EXPIRE_DAYS` | Refresh Token 만료 | 14 |
| `GOOGLE_CLIENT_ID` | 서버에서 검증할 Google Web Client ID | 로그인 시 필수 |
| `AZURE_SECRET_KEY` | Azure Speech 키 | 실제 평가 시 필수 |
| `AZURE_REGION` | Azure Speech 리전 | 실제 평가 시 필수 |
| `REPLICATE_API_KEY` | Replicate 키 | 영상 생성 시 필수 |
| `AWS_S3_BUCKET` | S3 버킷 | 미디어 저장 시 필수 |
| `AWS_S3_REGION` | S3 리전 | 미디어 저장 시 필수 |
| `AWS_ACCESS_KEY` | AWS 접근 키 | 비밀 |
| `AWS_SECRET_KEY` | AWS 비밀 키 | 비밀 |
| `FFMPEG_PATH` | FFmpeg 실행 경로 | `ffmpeg` |
| `GUIDE_JOBS_API_ENABLED` | 내부 guide-jobs HTTP API 등록 여부 | `false` |
| `GUIDE_JOBS_INTERNAL_TOKEN` | 활성화 시 필요한 내부 service Secret | 32자 이상, 비밀 |
| `GUIDE_JOBS_REQUESTS_PER_MINUTE` | 내부 호출자 분당 생성 한도 | 2 |
| `GUIDE_JOBS_MAX_CONCURRENT` | process당 동시 생성 한도 | 1 |
| `LOG_LEVEL` | 로그 레벨 | 로컬은 DEBUG 가능 |

### Docker Compose

```bash
cd backend
docker compose up --build
```

중지:

```bash
docker compose down
```

DB 데이터까지 삭제:

```bash
docker compose down -v
```

### Gradle 직접 실행

MySQL을 별도로 실행하고 환경변수를 주입한 뒤:

```bash
cd backend
./gradlew bootRun
```

## Flutter 실행

```bash
cd app
flutter doctor
flutter pub get
flutter devices
flutter run
```

### API 주소

- Android emulator: `http://10.0.2.2:8080`
- iOS simulator·desktop·test: `http://localhost:8080`
- 실기기: 개발 PC의 내부망 IP 사용

```bash
flutter run --dart-define=LINGKO_API_BASE_URL=http://192.168.0.10:8080
```

Google 로그인과 광고 테스트용 로컬 값은 Git에서 제외되는 `app/.env.local`에 한 번만 저장합니다. `run-local.sh`가 자동으로 읽으므로 별도 `source`는 필요하지 않습니다.

```bash
# app/.env.local
GOOGLE_SERVER_CLIENT_ID=Google-Web-Client-ID
IOS_DEVICE_ID=Flutter-iOS-Device-ID
ANDROID_DEVICE_ID=emulator-5554
ANDROID_EMULATOR_ID=Flutter-Android-AVD-ID
ADMOB_ANDROID_REWARDED_AD_UNIT_ID=Android-Rewarded-Ad-Unit-ID
ADMOB_IOS_REWARDED_AD_UNIT_ID=iOS-Rewarded-Ad-Unit-ID
```

```bash
cd app
./scripts/run-local.sh ios
./scripts/run-local.sh android
```

개발 중 보상형 광고를 확인할 때는 `.env.local`의 플랫폼별 값을 Google 공식 test Rewarded Ad Unit ID로 설정합니다.

```bash
# app/.env.local
ADMOB_ANDROID_REWARDED_AD_UNIT_ID=ca-app-pub-3940256099942544/5224354917
ADMOB_IOS_REWARDED_AD_UNIT_ID=ca-app-pub-3940256099942544/1712485313
```

광고 ID가 없으면 Home의 `+`와 Profile의 광고 개인정보 설정은 비활성입니다. 광고 SDK 변경은 hot reload가 아닌 앱 완전 재빌드가 필요합니다.

`run-local.sh`는 iOS에 `http://localhost:8080`, Android emulator에 `http://10.0.2.2:8080`을 자동 적용합니다. 지정한 iOS Simulator가 꺼져 있으면 부팅하고, Android Device ID가 연결되지 않았으면 `ANDROID_EMULATOR_ID`의 AVD를 실행한 뒤 준비될 때까지 기다립니다. Android 실기기는 `API_URL=http://개발-PC-IP:8080`을 추가합니다. 명령 앞에 직접 지정한 환경변수는 `.env.local`보다 우선합니다.

## Google OAuth 구성

- 서버 검증용으로 Web application OAuth Client ID를 만듭니다.
- Android 앱은 Android OAuth Client를 별도로 만들고 패키지명·SHA 인증서를 등록합니다.
- iOS 앱은 iOS OAuth Client를 별도로 만들고 Bundle ID를 등록합니다.
- Flutter가 얻은 Google ID Token의 audience는 서버의 `GOOGLE_CLIENT_ID`와 일치해야 합니다.
- Client Secret은 모바일 앱에 포함하지 않습니다.

현재 Android Debug 등록값은 다음 명령으로 확인합니다.

```bash
cd app/android
./gradlew signingReport
```

Google Cloud의 Android OAuth Client에는 package name `com.byeok.lingko`와 `Variant: debug`의 SHA-1을 등록해야 합니다. 계정 선택 후 로그인 창이 취소되는 것처럼 보이면 package name, SHA-1, Web Client ID 순서로 확인합니다.

## 권한

### Android

- `RECORD_AUDIO`
- 현재 최소 SDK 24

### iOS

- `NSMicrophoneUsageDescription`
- CocoaPods 설치 필요

```bash
cd app/ios
pod install
```

## 로컬 개발 체크

```bash
cd backend && ./gradlew test integrationTest
cd app && flutter analyze && flutter test
```

## 비밀정보 관리

- `.env`는 커밋하지 않습니다.
- `.env.example`에는 값이 아닌 키와 안전한 기본값만 둡니다.
- PR, 이슈, 터미널 캡처에 토큰과 API 키를 노출하지 않습니다.
- 노출 가능성이 있으면 즉시 키를 폐기하고 재발급합니다.
