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

[로컬 개발](../docs/development/local-development.md) · [인증 흐름](../docs/architecture/authentication-flow.md) · [API 계약](../docs/api/api-reference.md)
