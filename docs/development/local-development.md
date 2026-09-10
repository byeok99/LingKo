# 로컬 개발

Java 21, Flutter SDK, MySQL과 필요한 외부 서비스 개발용 설정을 준비합니다. 환경변수 이름과 기본값은 [백엔드 설정 예시](../../backend/.env.example)를 기준으로 합니다. 실제 비밀값을 커밋하지 않습니다.

## 백엔드

`backend/`에서 실행합니다.

```bash
./gradlew bootRun
```

DB와 외부 서비스 설정을 실행 환경에 전달해야 합니다. `bootRun`이 `.env` 파일을 자동으로 읽는다고 가정하지 않습니다. Compose 구성은 [백엔드 디렉터리](../../backend/)에서 확인할 수 있으며 API와 독립 Worker의 역할을 구분합니다.

## 앱

`app/`에서 실행합니다.

```bash
flutter pub get
flutter run --dart-define=LINGKO_API_BASE_URL=http://localhost:8080
```

API 주소는 실행 기기에서 접근 가능해야 합니다. Android emulator의 호스트 주소는 `10.0.2.2`이며 실제 기기는 개발 PC의 접근 가능한 주소를 사용합니다. 녹음 권한과 소셜 로그인은 플랫폼별 설정이 필요합니다. 개발 요청을 운영 서버로 보내지 않도록 API 주소를 명시합니다.

[테스트 구성](testing-and-troubleshooting.md) · [API 계약](../api/api-reference.md)
