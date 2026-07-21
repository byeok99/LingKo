# 로컬 개발 가이드

## 사전 요구사항

- Java 17
- Flutter SDK와 Dart 3.7 이상
- Android Studio 또는 Xcode
- Docker Desktop 또는 Docker Engine + Compose
- Git
- 외부 영상 생성 테스트 시 FFmpeg

## 백엔드 실행

```bash
cd backend
cp .env.example .env
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

Google 로그인까지 사용할 때:

```bash
flutter run \
  --dart-define=LINGKO_API_BASE_URL=http://192.168.0.10:8080 \
  --dart-define=GOOGLE_SERVER_CLIENT_ID=Google-Web-Client-ID
```

## Google OAuth 구성

- 서버 검증용으로 Web application OAuth Client ID를 만듭니다.
- Android 앱은 Android OAuth Client를 별도로 만들고 패키지명·SHA 인증서를 등록합니다.
- iOS 앱은 iOS OAuth Client를 별도로 만들고 Bundle ID를 등록합니다.
- Flutter가 얻은 Google ID Token의 audience는 서버의 `GOOGLE_CLIENT_ID`와 일치해야 합니다.
- Client Secret은 모바일 앱에 포함하지 않습니다.

## 권한

### Android

- `RECORD_AUDIO`
- 현재 최소 SDK 23

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
