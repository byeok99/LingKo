# LingKo App Code Flow

이 문서는 Flutter 앱 코드가 어떤 순서로 실행되고, 사용자의 행동이 어떤 파일과 함수로 이어지는지 설명한다.

## 1. 앱 시작

시작 파일:

- `lib/main.dart`

흐름:

```text
main()
  -> runApp(const LingKoApp())
```

`main()`은 Flutter 앱의 진입점이다. 여기서는 아무 화면 로직을 두지 않고 `LingKoApp`만 실행한다.

관련 코드:

```dart
void main() {
  runApp(const LingKoApp());
}
```

이렇게 둔 이유는 `main.dart`가 앱 실행만 책임지게 하기 위해서다.

## 2. 앱 루트 생성

관련 파일:

- `lib/app/lingko_app.dart`
- `lib/app/app_theme.dart`

흐름:

```text
LingKoApp.build()
  -> MaterialApp
      -> theme: AppTheme.light()
      -> home: LingKoShell
```

`LingKoApp`은 앱 전체 설정을 담당한다.

- 앱 이름
- debug banner 표시 여부
- 전체 테마
- 첫 화면

`AppTheme.light()`는 색상과 텍스트 스타일을 만든다. 색상 값은 `AppColors`에 모아 둔다.

테마를 분리한 이유:

- 화면 파일마다 색상 값을 직접 쓰는 일을 줄인다.
- 브랜드 컬러가 바뀌면 `app_theme.dart`만 고치면 된다.
- 화면 코드는 레이아웃과 사용자 흐름에 집중한다.

## 3. 하단 탭과 앱 상태

관련 파일:

- `lib/app/lingko_app.dart`

핵심 클래스:

- `LingKoShell`
- `_LingKoShellState`

`LingKoShell`은 앱의 현재 상태를 관리한다.

상태 값:

```dart
int selectedTab = 0;
PracticeSentence selectedSentence = mockSentences.first;
bool hasResult = false;
```

의미:

- `selectedTab`: 현재 하단 탭. `0 = Home`, `1 = Practice`, `2 = Profile`
- `selectedSentence`: 현재 연습할 문장
- `hasResult`: Practice 탭에서 연습 화면 대신 결과 화면을 보여줄지 여부

현재 화면 선택:

```text
selectedTab == 0 -> HomeScreen
selectedTab == 1 -> PracticeScreen 또는 ResultScreen
selectedTab == 2 -> ProfileScreen
```

`hasResult`가 `false`면 Practice 화면을 보여준다.

```dart
PracticeScreen(sentence: selectedSentence, onResult: showResult)
```

`hasResult`가 `true`면 Result 화면을 보여준다.

```dart
ResultScreen(sentence: selectedSentence, onTryAgain: ...)
```

## 4. 홈에서 문장 선택

관련 파일:

- `lib/app/lingko_app.dart`
- `lib/screens/home_screen.dart`
- `lib/widgets/sentence_card.dart`

흐름:

```text
HomeScreen
  -> SentenceCard tap
      -> onSelect(sentence)
          -> LingKoShell.openPractice(sentence)
```

`HomeScreen`은 문장을 직접 선택한 뒤 무엇을 할지 모른다. 대신 `onSelect` 콜백을 받는다.

```dart
HomeScreen(
  sentences: mockSentences,
  onSelect: openPractice,
)
```

`SentenceCard`를 누르면 `onSelect(sentence)`가 실행된다.

```dart
onTap: () => onSelect(sentence)
```

그 결과 `LingKoShell.openPractice()`가 실행된다.

```dart
void openPractice(PracticeSentence sentence) {
  setState(() {
    selectedSentence = sentence;
    selectedTab = 1;
    hasResult = false;
  });
}
```

이 함수가 하는 일:

- 선택한 문장을 `selectedSentence`에 저장
- Practice 탭으로 이동
- 이전 결과 화면이 남아 있지 않게 `hasResult` 초기화

이 구조의 장점:

- `HomeScreen`은 상태 관리 방법을 몰라도 된다.
- `SentenceCard`는 탭 UI만 담당한다.
- 실제 이동/상태 변경은 `LingKoShell`이 담당한다.

## 5. 직접 입력한 문장으로 연습

관련 파일:

- `lib/app/lingko_app.dart`
- `lib/screens/practice_screen.dart`
- `lib/models/practice_sentence.dart`

흐름:

```text
PracticeScreen
  -> TextField 입력
      -> Use this sentence
          -> PracticeSentence.custom(text)
              -> LingKoShell.useCustomSentence(sentence)
                  -> selectedSentence 교체
```

`PracticeScreen`은 입력 UI를 보여주지만 앱의 현재 문장을 직접 바꾸지는 않는다. 사용자가 입력을 확정하면 `onCustomSentence` 콜백으로 새 문장 모델을 상위에 전달한다.

```dart
PracticeScreen(
  sentence: selectedSentence,
  onResult: showResult,
  onCustomSentence: useCustomSentence,
)
```

`PracticeSentence.custom()`은 사용자가 입력한 문자열을 임시 연습 데이터로 바꾼다.

현재는 백엔드 분석 전 단계라 아래 값은 더미다.

- 표준 발음
- 번역
- 학습 포인트
- 글자별 가이드

나중에 실제 구현에서는 사용자가 입력한 문장을 백엔드로 보내고, 백엔드가 표준 발음과 분석용 글자 데이터를 내려주는 구조로 바꾼다.

## 6. Practice 화면 표시

관련 파일:

- `lib/screens/practice_screen.dart`
- `lib/widgets/shared_widgets.dart`

`PracticeScreen`은 선택된 `PracticeSentence`를 받아 화면에 표시한다.

표시하는 정보:

- 원문
- 표준 발음
- 번역
- 듣기 버튼 자리
- 글자별 가이드 칩
- 학습 포인트
- 녹음 버튼
- 직접 입력 폼

`PracticeScreen`은 실제 녹음이나 평가 로직을 모른다. 녹음 버튼을 눌렀을 때 실행할 콜백만 받는다.

```dart
PracticeScreen(
  sentence: selectedSentence,
  onResult: showResult,
)
```

버튼:

```dart
FilledButton.icon(
  onPressed: onResult,
  icon: const Icon(Icons.mic),
  label: const Text('Record and score'),
)
```

지금은 디자인 확인 단계라 버튼을 누르면 바로 결과 화면으로 넘어간다.

## 7. 결과 화면 전환

관련 파일:

- `lib/app/lingko_app.dart`
- `lib/screens/result_screen.dart`

흐름:

```text
Record and score
  -> onResult()
      -> LingKoShell.showResult()
          -> hasResult = true
              -> Practice 탭 body가 ResultScreen으로 변경
```

`showResult()`:

```dart
void showResult() {
  setState(() {
    hasResult = true;
  });
}
```

중요한 점:

- 하단 탭은 여전히 Practice 탭이다.
- 화면 내용만 `PracticeScreen`에서 `ResultScreen`으로 바뀐다.
- 그래서 Result는 별도 탭이 아니라 연습 완료 후 도달하는 상태다.

## 8. Result 화면 구성

관련 파일:

- `lib/screens/result_screen.dart`
- `lib/widgets/score_breakdown.dart`
- `lib/widgets/result_tile.dart`

`ResultScreen`은 선택된 문장의 더미 점수를 보여준다.

취약 글자 계산:

```dart
final weak = sentence.characters.where((item) => item.score < 80).toList();
```

현재 기준:

- 80점 미만이면 취약 글자
- 취약 글자만 `Weak sounds` 목록에 표시

화면 구성:

```text
ResultScreen
  -> TopBar
  -> 전체 점수
  -> 한 줄 피드백
  -> ScoreBreakdown
  -> Weak sounds
      -> ResultTile
  -> Try again
```

`Try again` 버튼을 누르면 `onTryAgain`이 실행된다.

```dart
onTryAgain: () {
  setState(() {
    hasResult = false;
  });
}
```

그 결과 Practice 탭 안에서 다시 `PracticeScreen`이 보인다.

## 9. 글자별 가이드 열기

관련 파일:

- `lib/widgets/result_tile.dart`
- `lib/widgets/guide_sheet.dart`
- `lib/widgets/guide_painter.dart`

흐름:

```text
ResultTile tap
  -> showModalBottomSheet()
      -> GuideSheet
          -> GuidePainter
```

`ResultTile`은 취약 글자 하나를 보여준다. 누르면 `showModalBottomSheet`로 하단 패널을 연다.

```dart
showModalBottomSheet<void>(
  context: context,
  builder: (_) => GuideSheet(result: result),
)
```

`GuideSheet`는 하단 패널 UI를 담당한다.

현재 표시:

- 글자
- Mouth 또는 Tongue guide 제목
- 임시 그림
- 짧은 피드백
- Play / Repeat 버튼 자리

`GuidePainter`는 임시 그림을 그린다. 실제 구현에서는 이 부분이 S3 이미지 또는 영상 위젯으로 바뀔 수 있다.

## 10. Profile 탭

관련 파일:

- `lib/screens/profile_screen.dart`
- `lib/widgets/settings_row.dart`

현재는 설정 화면 자리만 잡아 둔 상태다.

표시 항목:

- Display language
- Native language
- Target level

실제 로그인, 언어 설정 저장, 사용자 프로필 API는 아직 연결하지 않았다.

## 11. 데이터 흐름 요약

현재 데이터 흐름은 단순하다.

```text
mockSentences
  -> LingKoShell.selectedSentence
      -> PracticeScreen
      -> ResultScreen
          -> ResultTile
              -> GuideSheet
```

직접 입력 흐름은 다음과 같다.

```text
TextField input
  -> PracticeSentence.custom
      -> LingKoShell.selectedSentence
          -> PracticeScreen
          -> ResultScreen
```

백엔드 연동 후 예상 흐름:

```text
GET /api/sentences/recommended
  -> PracticeSentence list
      -> HomeScreen

POST /api/pronunciation/evaluate
  -> Assessment result
      -> ResultScreen
```

## 12. 콜백 흐름 요약

```text
SentenceCard.onTap
  -> HomeScreen.onSelect
      -> LingKoShell.openPractice

Use this sentence.onPressed
  -> PracticeScreen.onCustomSentence
      -> LingKoShell.useCustomSentence

Record button.onPressed
  -> PracticeScreen.onResult
      -> LingKoShell.showResult

Try again.onPressed
  -> ResultScreen.onTryAgain
      -> hasResult = false

ResultTile.onTap
  -> showModalBottomSheet
      -> GuideSheet
```

콜백을 쓰는 이유:

- 하위 위젯이 상위 상태를 직접 바꾸지 않는다.
- 화면 위젯은 자신의 역할에 집중한다.
- 나중에 API 호출이나 상태 관리 라이브러리를 붙이기 쉽다.

## 13. 변경할 때 보는 파일

자주 바꿀 가능성이 높은 위치:

- 추천 문장 더미 데이터 수정: `lib/data/mock_sentences.dart`
- 직접 입력 문장 기본값 수정: `lib/models/practice_sentence.dart`
- 홈 UI 수정: `lib/screens/home_screen.dart`
- 연습 UI 수정: `lib/screens/practice_screen.dart`
- 결과 UI 수정: `lib/screens/result_screen.dart`
- 하단 탭/화면 전환 수정: `lib/app/lingko_app.dart`
- 색상/폰트 수정: `lib/app/app_theme.dart`
- 가이드 임시 그림 수정: `lib/widgets/guide_painter.dart`

## 14. 현재 의도적으로 단순하게 둔 부분

- 상태 관리 라이브러리 없음
- 라우터 없음
- API client 없음
- 실제 녹음 없음
- 실제 오디오 재생 없음
- 실제 이미지/영상 asset 없음

MVP 디자인 검증 단계에서는 이 단순함이 낫다. 기능이 붙기 시작하면 상태 관리와 API 계층을 추가한다.
