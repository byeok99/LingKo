# 테스트 전략과 트러블슈팅

## 테스트 계층

| 계층 | 명령 | 외부 서비스 | 실행 시점 |
|---|---|---:|---|
| Backend 단위·Controller | `./gradlew test` | 없음 | 모든 PR |
| Backend 내부 통합 | `./gradlew integrationTest` | 없음 | 모든 PR |
| Backend 외부 통합 | `./gradlew externalIntegrationTest` | Azure·Replicate·S3·FFmpeg | 수동·야간·릴리스 전 |
| Flutter 정적 분석 | `flutter analyze` | 없음 | 모든 PR |
| Flutter 테스트 | `flutter test` | 없음 | 모든 PR |
| Android Debug 빌드 | `flutter build apk --debug` | 없음 | 네이티브 설정 변경 시 |
| 실기기 점검 | 수동 | OAuth·마이크·백엔드 | 릴리스 전 |

## Backend

```bash
cd backend
./gradlew test
./gradlew integrationTest
```

외부 통합 테스트는 다음 환경변수가 필요합니다.

```text
AZURE_SECRET_KEY
AZURE_REGION
REPLICATE_API_KEY
REPLICATE_VERSION
AWS_S3_BUCKET
AWS_S3_REGION
AWS_ACCESS_KEY
AWS_SECRET_KEY
```

```bash
./gradlew externalIntegrationTest
```

JaCoCo HTML 결과는 일반적으로 `backend/build/reports/jacoco/test/html/index.html`에서 확인합니다.

## Flutter

```bash
cd app
flutter analyze
flutter test
```

네이티브 녹음 설정 변경 시:

```bash
flutter build apk --debug
```

## 릴리스 전 수동 체크리스트

- Android와 iOS에서 첫 마이크 권한 팝업
- 권한 거부 후 재시도 UX
- 실제 WAV 업로드와 평가 성공
- 녹음 중 화면 이동·앱 백그라운드 전환
- Google 로그인·로그아웃·앱 재실행 후 세션 복원
- 만료되거나 잘못된 JWT 처리
- 느린 네트워크와 서버 오류 표시
- 추천 문장·자유 문장 평가
- 연습 기록·사용자 설정 조회와 변경
- S3 가이드 이미지 로딩 실패 fallback
- 외부 가이드 작업 실패와 polling 종료

## 자주 발생하는 문제

### Flutter에서 백엔드 연결 실패

- Android 에뮬레이터는 `localhost` 대신 `10.0.2.2` 사용
- 실기기는 PC와 같은 네트워크인지 확인
- 백엔드가 `0.0.0.0:8080` 또는 접근 가능한 인터페이스에 열렸는지 확인
- OS 방화벽에서 8080 포트 허용 여부 확인

### `No pubspec.yaml file found`

`app/` 디렉터리에서 실행합니다.

```bash
cd app
flutter pub get
flutter run
```

### Google 로그인 audience 오류

- Flutter의 `GOOGLE_SERVER_CLIENT_ID`
- 백엔드의 `GOOGLE_CLIENT_ID`
- Google Cloud의 Web application Client ID

세 값이 같은 서버 Client ID를 가리키는지 확인합니다.

### WAV 415 오류

- 확장자가 `.wav`인지 확인
- 16-bit mono PCM인지 확인
- 실제 WAV 헤더의 byte rate와 block align이 일치하는지 확인
- 파일이 44바이트보다 크고 data chunk가 존재하는지 확인

### Docker MySQL이 시작되지 않음

- `.env`의 `DB_PASSWORD`가 비어 있지 않은지 확인
- 기존 3306 포트 사용 프로세스 확인
- `docker compose logs mysql` 확인
- 테스트 데이터가 필요 없으면 `docker compose down -v` 후 재시작

### 외부 통합 테스트가 즉시 실패

필수 환경변수 누락 시 의도적으로 테스트 시작 전에 실패합니다. `.env`를 shell 환경으로 export한 뒤 실행합니다.

## 테스트 작성 원칙

- 기능 성공 경로와 실패 경로를 함께 작성합니다.
- 외부 네트워크를 호출하는 테스트에는 `external` 태그를 사용합니다.
- 테스트가 실제 비밀값과 운영 리소스를 기본적으로 요구하지 않게 합니다.
- API 계약 변경 시 Flutter API 테스트와 Backend Controller 테스트를 함께 수정합니다.
- 시간·날짜 로직은 주입 가능한 `Clock`을 사용합니다.
