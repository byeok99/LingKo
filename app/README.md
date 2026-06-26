# LingKo Flutter App

LingKo의 iOS/Android 앱 프로토타입입니다. Phase 5 기준으로 Home 추천 문장은 백엔드 `GET /api/sentences/recommended`에서 로딩하고, Practice 탭의 직접 입력 문장은 `POST /api/pronunciation/prepare`와 연결되어 표준 발음과 글자별 가이드를 받습니다. Practice 녹음은 `POST /api/evaluations`로 업로드해 평가 결과를 표시합니다. 결과 저장은 아직 더미 흐름입니다.

코드가 실제로 어떤 순서로 실행되는지는 [docs/code-flow.md](docs/code-flow.md)에 별도로 정리했습니다.

## 현재 범위

현재 앱은 발음 학습 MVP만 다룹니다.

- 홈에서 추천 문장 선택
- 연습 탭에서 사용자가 직접 문장 입력 후 서버 prepare API 호출
- 문장 원문, 서버 표준 발음, 번역 확인
- 일반/느린 듣기 버튼 자리 확인
- 글자별 발음 가이드 진입점 확인
- 녹음 후 서버 평가 결과 표시
- 결과에서 취약 글자 선택
- 입/혀 가이드 바텀시트 확인

아직 구현하지 않은 것:

- 실제 로그인
- 결과 저장
- 실제 가이드 이미지/영상 재생
- 코스 기능

## 디렉터리 구조

```text
app/
├── android/              # Android 네이티브 프로젝트. 보통 직접 수정하지 않음
├── ios/                  # iOS 네이티브 프로젝트. 보통 직접 수정하지 않음
├── docs/
│   └── code-flow.md      # 코드 실행 흐름 설명
├── lib/
│   ├── main.dart         # 앱 시작점. runApp만 담당
│   ├── app/              # 앱 루트, 하단 탭 Shell, 테마
│   ├── api/              # 백엔드 API client
│   ├── data/             # 오래된 mock 데이터. Home 추천 목록은 API 응답을 사용
│   ├── models/           # 화면/데이터 모델
│   ├── screens/          # 화면 단위 위젯
│   └── widgets/          # 재사용 가능한 작은 UI 위젯
├── test/
│   └── widget_test.dart  # 화면 흐름 테스트
├── pubspec.yaml          # 앱 이름, 의존성, asset 설정
└── analysis_options.yaml # Dart/Flutter lint 설정
```

Flutter에서는 대부분의 화면 작업을 `lib/` 아래 Dart 파일에서 합니다. 현재는 클린 코드 기준으로 역할별 파일을 나눴습니다.

Android/iOS 폴더는 Flutter가 앱을 각 플랫폼에 올리기 위해 만든 네이티브 프로젝트입니다. 화면을 고치거나 기능 흐름을 바꿀 때는 대부분 `lib/`만 수정합니다.

## 구조 원칙

현재 앱 구조는 다음 기준으로 나눕니다.

- `main.dart`: 앱 실행만 담당
- `app/`: 앱 전체 조립, 전역 테마, 하단 탭 상태 관리
- `models/`: 화면에서 쓰는 데이터 형태 정의
- `data/`: 오래된 mock 데이터. 현재 Home 추천 목록은 백엔드 API 응답 사용
- `screens/`: 한 화면 전체를 구성하는 위젯
- `widgets/`: 여러 화면에서 재사용하는 작은 UI 부품

파일을 나누는 기준은 "크기"가 아니라 "변경 이유"입니다. 예를 들어 점수 표시 UI가 바뀌면 `score_breakdown.dart`만 바뀌고, 추천 문장 API 계약이 바뀌면 `api/sentence_api.dart`와 모델 매핑만 바뀌는 구조를 목표로 합니다.

이렇게 나눈 이유는 SOLID 중 특히 아래 원칙을 지키기 위해서입니다.

- Single Responsibility: 파일과 클래스가 한 가지 이유로만 바뀌도록 분리
- Open/Closed: 화면은 그대로 두고 데이터/API 구현만 교체하기 쉽게 구성
- Dependency Inversion: 화면이 백엔드 구현이 아니라 모델과 콜백에 의존하도록 구성

현재는 상태 관리 라이브러리를 쓰지 않습니다. MVP 디자인 검증 단계에서는 `StatefulWidget`과 콜백만으로 충분합니다. API 연동, 로그인, 기록 저장이 들어오면 Riverpod, Bloc 같은 상태 관리 도입을 검토할 수 있습니다.

## Flutter 기본 개념

### Widget

Flutter 화면의 모든 것은 Widget입니다. 텍스트, 버튼, 여백, 리스트, 화면 전체까지 모두 Widget입니다.

예:

- `Text`: 글자
- `IconButton`: 아이콘 버튼
- `ListView`: 스크롤 목록
- `Scaffold`: 앱 화면의 기본 뼈대
- `NavigationBar`: 하단 탭

### StatelessWidget

상태가 없는 화면 조각입니다. 입력값을 받아 그대로 그리기만 합니다.

현재 예:

- `HomeScreen`
- `PracticeScreen`
- `ResultScreen`
- `SentenceCard`

### StatefulWidget

화면 안에서 바뀌는 값이 필요할 때 씁니다. `LingKoShell`이 여기에 해당합니다.

현재 `LingKoShell`이 관리하는 상태:

- `selectedTab`: 현재 하단 탭
- `selectedSentence`: 현재 선택된 문장
- `hasResult`: Practice 탭에서 결과 화면을 보여줄지 여부

### setState

상태를 바꾸고 화면을 다시 그리라고 Flutter에 알려주는 함수입니다.

현재 예:

```dart
setState(() {
  selectedSentence = sentence;
  selectedTab = 1;
  hasResult = false;
});
```

이 코드는 문장을 선택하면 Practice 탭으로 이동하고 결과 화면 상태를 초기화합니다.

## 화면 흐름

```text
Home
  └─ 추천 문장 선택
      └─ Practice
          └─ Record and score
              └─ Result
                  └─ 취약 글자 선택
                      └─ Guide bottom sheet
```

또는 하단 `Practice` 탭에서 직접 문장을 입력해 바로 연습 대상으로 바꿀 수 있습니다.

하단 탭은 MVP 기준으로 3개만 둡니다.

- `Home`: 추천 문장 목록
- `Practice`: 현재 선택한 문장 연습 또는 결과
- `Profile`: 언어/레벨 설정 자리

`Result`는 별도 하단 탭이 아닙니다. 사용자가 녹음 후 자연스럽게 도달하는 Practice 내부 상태입니다.

더 자세한 코드 단위 흐름은 [docs/code-flow.md](docs/code-flow.md)를 참고합니다.

## 주요 코드 위치

### 앱 루트와 테마

관련 파일:

- `lib/main.dart`
- `lib/app/lingko_app.dart`
- `lib/app/app_theme.dart`

- 앱 이름 설정
- Material 3 사용
- 브랜드 컬러와 텍스트 스타일 설정
- 첫 화면으로 `LingKoShell` 지정

### 추천 문장 API

관련 파일:

- `lib/models/practice_sentence.dart`
- `lib/api/sentence_api.dart`

Home 추천 목록은 `GET /api/sentences/recommended` 응답을 `PracticeSentence`로 매핑해 표시합니다. `lib/data/mock_sentences.dart`는 이전 프로토타입 데이터이며 현재 Home fallback으로 사용하지 않습니다.

### 하단 탭과 상태

관련 파일:

- `lib/app/lingko_app.dart`

앱의 현재 상태를 관리합니다.

- 홈에서 문장을 누르면 `openPractice()` 실행
- Practice에서 녹음 후 `Upload and score`를 누르면 평가 API 호출 후 결과 화면으로 이동
- 하단 탭을 누르면 `selectedTab` 변경

### 홈

관련 파일:

- `lib/screens/home_screen.dart`
- `lib/widgets/progress_panel.dart`
- `lib/widgets/sentence_card.dart`

추천 문장 목록은 `GET /api/sentences/recommended`에서 로딩합니다. Home은 로딩, 실패, 빈 목록 상태를 표시하며 mock fallback은 사용하지 않습니다.

### 연습

관련 파일:

- `lib/screens/practice_screen.dart`
- `lib/widgets/shared_widgets.dart`

선택한 문장의 원문, 표준 발음, 번역, 듣기 버튼, 글자별 가이드 칩, 녹음 상태와 평가 업로드 버튼을 보여줍니다.

사용자가 직접 입력한 문장은 `POST /api/pronunciation/prepare`로 전송됩니다. 서버 응답의 `standardPronunciation`과 `characters`를 `PracticeSentence`로 매핑해 화면에 표시합니다.

녹음은 `record` 패키지로 WAV 파일을 만들고, `POST /api/evaluations` multipart 요청으로 업로드합니다. Android는 `RECORD_AUDIO`, iOS는 `NSMicrophoneUsageDescription` 권한 설정이 필요합니다.

#### API base URL

기본 API 주소는 실행 환경에 따라 다릅니다.

- Android emulator: `http://10.0.2.2:8080`
- iOS simulator, desktop, tests: `http://localhost:8080`

실기기 또는 다른 호스트의 백엔드를 사용할 때는 Flutter 실행 시 compile-time define으로 덮어씁니다.

```bash
flutter run --dart-define=LINGKO_API_BASE_URL=http://192.168.0.10:8080
```

### 결과

관련 파일:

- `lib/screens/result_screen.dart`
- `lib/widgets/score_breakdown.dart`
- `lib/widgets/result_tile.dart`

서버 평가 응답의 `overallScore`, `gradeLabel`, `summary`, `scoreBreakdown`, `weakCharacters`를 표시합니다. 저장된 기록 화면은 아직 구현하지 않았습니다.

### 글자별 가이드

관련 파일:

- `lib/widgets/guide_sheet.dart`
- `lib/widgets/guide_painter.dart`

취약 글자를 누르면 하단 패널이 열립니다. 지금은 `GuidePainter`로 임시 그림을 그립니다. 실제 구현에서는 S3 이미지/영상 URL을 받아 표시하도록 교체합니다.

## 실행

Android 에뮬레이터:

```bash
flutter run -d emulator-5554
```

연결된 장치 확인:

```bash
flutter devices
```

iOS 시뮬레이터가 켜져 있으면:

```bash
flutter run -d ios
```

## 검증

정적 분석:

```bash
flutter analyze
```

테스트:

```bash
flutter test
```

현재 테스트는 다음 흐름을 확인합니다.

- 앱 시작
- 홈 문장 노출
- 문장 선택
- Practice 화면 이동
- 녹음 버튼 노출
- 결과 화면 이동

## 다음 구현 방향

MVP 기준 다음 순서가 적절합니다.

1. 평가 결과 저장
2. 평가 기록 화면 연결
3. 발음 평가 API 연결
4. 결과 저장/조회 연결
5. 실제 입/혀 가이드 이미지 또는 영상 연결
