# Flutter and Dart Basics

이 문서는 LingKo 앱 코드를 읽기 위해 필요한 Flutter 컨셉과 Dart 문법을 설명한다.

Flutter를 처음 볼 때는 아래 순서로 이해하면 된다.

```text
상태가 바뀐다
  -> setState()를 호출한다
      -> build()가 다시 실행된다
          -> 새 상태에 맞는 Widget 트리가 그려진다
```

## 1. Flutter의 핵심 컨셉

Flutter는 화면을 Widget 트리로 만든다.

텍스트, 버튼, 여백, 리스트, 화면 전체가 모두 Widget이다.

예:

```dart
Column(
  children: [
    Text('Practice'),
    FilledButton(
      onPressed: () {},
      child: Text('Start'),
    ),
  ],
)
```

LingKo 앱의 큰 구조는 다음과 같다.

```text
main.dart
  -> LingKoApp
      -> MaterialApp
          -> LingKoShell
              -> Scaffold
                  -> body
                  -> bottomNavigationBar
```

## 2. Widget 트리

Flutter 화면 코드는 Widget을 중첩해서 만든다.

```dart
Scaffold(
  body: SafeArea(
    child: ListView(
      children: [
        Text('Practice'),
        TextField(),
      ],
    ),
  ),
)
```

의미:

- `Scaffold`: 앱 화면의 기본 뼈대
- `SafeArea`: 노치, 상태바 영역을 피해 배치
- `ListView`: 스크롤 가능한 세로 목록
- `Text`: 글자
- `TextField`: 입력창

## 3. StatelessWidget

`StatelessWidget`은 상태가 없는 화면 조각이다. 외부에서 받은 값을 그대로 화면에 그릴 때 사용한다.

```dart
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Text('Profile');
  }
}
```

LingKo 예:

- `HomeScreen`
- `ProfileScreen`
- `ResultScreen`
- `SentenceCard`

## 4. StatefulWidget

`StatefulWidget`은 화면 안에서 값이 바뀌어야 할 때 사용한다.

LingKo 예:

- `LingKoShell`
- `PracticeScreen`

`LingKoShell`은 현재 탭, 선택 문장, 결과 화면 여부를 관리한다.

```dart
int selectedTab = 0;
PracticeSentence? selectedSentence;
bool hasResult = false;
```

값이 바뀌면 `setState()`를 호출한다.

```dart
setState(() {
  selectedTab = 1;
  selectedSentence = sentence;
});
```

`setState()`는 Flutter에게 "상태가 바뀌었으니 화면을 다시 그려라"라고 알려준다.

## 5. build()

`build()`는 화면을 그리는 함수다.

```dart
@override
Widget build(BuildContext context) {
  return Text('LingKo');
}
```

중요한 점:

- `build()`는 여러 번 호출될 수 있다.
- `build()` 안에서 오래 걸리는 작업을 하면 안 된다.
- API 호출, 파일 저장, 녹음 시작 같은 작업은 별도 함수나 상태 관리 계층에서 처리한다.

## 6. Nullable 문법

Dart에서 `?`는 null이 될 수 있다는 뜻이다.

```dart
PracticeSentence? selectedSentence;
```

이 값은 문장이 있을 수도 있고 없을 수도 있다.

```dart
if (selectedSentence != null) {
  ResultScreen(sentence: selectedSentence!)
}
```

`!`는 "여기서는 null이 아님을 보장한다"는 뜻이다.

## 7. final

`final`은 한 번 값이 정해지면 다시 바꾸지 않는 변수다.

```dart
final PracticeSentence? sentence;
```

Widget의 입력값은 대부분 `final`로 둔다.

## 8. const

`const`는 컴파일 시점에 고정 가능한 값을 만들 때 사용한다.

```dart
const Text('Practice')
const SizedBox(height: 12)
```

## 9. required

`required`는 생성자에서 반드시 받아야 하는 값을 표시한다.

```dart
const PracticeScreen({
  required this.sentence,
  required this.onResult,
  required this.onCustomSentence,
});
```

## 10. 콜백

콜백은 함수를 값처럼 전달하는 방식이다. 하위 Widget이 상위 상태를 직접 바꾸지 않도록 하기 위해 사용한다.

```dart
final VoidCallback onResult;
```

```dart
FilledButton.icon(
  onPressed: onResult,
  icon: const Icon(Icons.mic),
  label: const Text('Record and score'),
)
```

## 11. ValueChanged

`ValueChanged<T>`는 값 하나를 받는 콜백 타입이다.

```dart
final ValueChanged<PracticeSentence> onCustomSentence;
```

의미:

```dart
void Function(PracticeSentence)
```

## 12. TextEditingController

`TextEditingController`는 입력창의 값을 읽거나 바꿀 때 사용한다.

```dart
final TextEditingController customSentenceController =
    TextEditingController();
```

입력값 읽기:

```dart
final text = customSentenceController.text.trim();
```

입력값 바꾸기:

```dart
customSentenceController.text = widget.sentence?.text ?? '';
```

## 13. FocusNode

`FocusNode`는 입력창의 포커스를 제어한다.

```dart
final FocusNode customSentenceFocusNode = FocusNode();
```

```dart
customSentenceFocusNode.requestFocus();
```

## 14. dispose()

컨트롤러나 포커스 노드처럼 리소스를 쓰는 객체는 화면이 사라질 때 정리해야 한다.

```dart
@override
void dispose() {
  customSentenceController.dispose();
  customSentenceFocusNode.dispose();
  super.dispose();
}
```

## 15. factory 생성자

`factory` 생성자는 입력값을 가공해서 객체를 만들 때 사용한다.

```dart
factory PracticeSentence.custom(String text) {
  return PracticeSentence(
    text: text,
    pronunciation: 'Custom sentence',
    translation: 'Practice with your own sentence.',
    level: 'Custom',
    category: 'Free practice',
    point: 'Record your voice to receive pronunciation feedback.',
    score: 0,
    characters: [],
  );
}
```

## 16. 현재 LingKo 앱 흐름

하단 Practice 탭으로 바로 들어간 경우:

```text
Practice tab
  -> selectedSentence == null
      -> 빈 입력창 표시
      -> 연습 상세/녹음 버튼 숨김
```

Home에서 추천 문장을 선택한 경우:

```text
Home
  -> 추천 문장 선택
      -> openPractice(sentence)
          -> selectedSentence 저장
          -> Practice 탭 이동
          -> 입력창에 추천 문장 자동 입력
```

Practice에서 직접 입력한 경우:

```text
Practice
  -> TextField 입력
      -> Use this sentence
          -> PracticeSentence.custom(text)
              -> selectedSentence 교체
              -> 연습 상세 표시
```

녹음 버튼을 누른 경우:

```text
Record and score
  -> showResult()
      -> hasResult = true
          -> Practice 탭 내부에서 ResultScreen 표시
```

## 17. 자주 보는 파일

- `lib/main.dart`: 앱 시작점
- `lib/app/lingko_app.dart`: 앱 루트, 하단 탭, 주요 상태
- `lib/screens/practice_screen.dart`: 직접 입력, 연습 화면
- `lib/models/practice_sentence.dart`: 연습 문장 데이터 구조
- `lib/data/mock_sentences.dart`: 추천 문장 더미 데이터
- `lib/widgets/`: 재사용 UI 부품
