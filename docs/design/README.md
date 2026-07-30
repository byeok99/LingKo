# LingKo 앱 디자인 기준

`preview.html`은 Flutter 앱의 모바일 시각 기준이다. 색상, 타이포그래피, 여백, 카드 밀도, 하단 내비게이션과 Home·Practice·Recording·Evaluating·Result·Review·Profile의 정보 계층을 이 파일에 맞춘다.

## 구현 원칙

- 실제 API와 상태가 있는 정보만 화면에 표시한다.
- 프로토타입의 XP, 알림, 즐겨찾기, 앱 설정처럼 현재 데이터나 동작이 없는 요소는 장식용으로 만들지 않는다.
- 일일 진행률은 실제 practice quota의 사용량과 제한으로 계산한다.
- Practice는 추천 문장과 직접 입력을 별도 탭으로 나누지 않는다. 하나의 편집 가능한 문장 입력에서 직접 작성하거나 Home 추천 문장을 이어서 수정할 수 있으며, 입력을 멈추면 표준 발음을 자동 준비한다.
- 표준 발음 준비가 끝난 현재 문장에는 표준 발음을 항상 바로 보여주고 `Listen`과 녹음을 제공한다. Practice에서는 번역·학습 팁·완료 배지·표준 발음 토글을 두지 않으며, 음절별 상세 가이드는 평가 후 Result에서만 제공한다.
- Result의 `Pronunciation guide`는 `Sentence`와 `Standard pronunciation`만 비교해 보여주며 사용자 인식 문장은 표시하지 않는다. 표준 발음은 기기 TTS의 `Normal`과 `Slow`로 들을 수 있고 음원 파일은 별도로 보존하지 않는다.
- Recording과 Evaluating은 하단 내비게이션을 숨기며, 평가 중에는 `Continue in background`로 Home에 돌아갈 수 있다.
- Result의 점수, 음절과 상세 피드백은 평가 API 응답을 그대로 사용한다.
- Review 그래프는 API가 최신순으로 제공한 기록 중 최근 7개를 선택해 오래된 점수부터 최신 점수 순으로 표시한다.
- Profile은 서버가 지원하는 학습 환경설정, 로그아웃과 회원 탈퇴만 제공한다.

## 검증

- 디자인 토큰은 `app/test/design_system_test.dart`에서 고정한다.
- 화면 기능과 작은 화면·큰 글자 회귀는 `app/test/widget_test.dart`에서 검증한다.
- 실제 iPhone Safe Area, 키보드와 네트워크 연동은 시뮬레이터 또는 실기기에서 별도로 확인한다.
