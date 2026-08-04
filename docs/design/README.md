# LingKo 앱 디자인 기준

`preview.html`은 Flutter 앱의 모바일 시각 기준이다. 색상, 타이포그래피, 여백, 카드 밀도, 하단 내비게이션과 Home·Practice·Recording·Evaluating·Result·Review·Profile의 정보 계층을 이 파일에 맞춘다.

## 구현 원칙

- 실제 API와 상태가 있는 정보만 화면에 표시한다.
- 프로토타입의 XP, 알림, 즐겨찾기, 앱 설정처럼 현재 데이터나 동작이 없는 요소는 장식용으로 만들지 않는다.
- Home 평가 기회는 `LingKo` wordmark와 같은 상단 행의 가장 오른쪽에 166px 투명 정렬 영역을 두고, 실제 40px 높이 capsule을 그 안의 오른쪽에 붙인다. 흰 카드 표면과 얇은 테두리, soft blue vector 마이크로 다른 control과 결을 맞추고 `현재/최대`, 서버 기준 countdown을 표시한다. capsule 내부 좌우에는 8px padding을 둔다. 최대치에는 `MAX`, 5개 미만에는 광고 adapter용 `+`를 표시하며 SDK가 없으면 비활성화한다. `+`가 숨겨진 MAX 상태에서는 실제 capsule만 내용 너비로 줄어든다.
- Home 추천은 Daily, Food, Travel, Study, Work, Health 상황 칩으로 구분하고 선택한 카테고리의 문장 3개만 먼저 보여준다. 더 많은 문장은 전체 보기로 확장하며 별도 버튼으로 빈 직접 입력 Practice에 진입한다.
- 진행 중이거나 실패한 평가는 새 추천보다 우선해 Home의 상황별 추천 영역 위에 표시한다.
- Practice는 추천 문장과 직접 입력을 별도 탭으로 나누지 않는다. 하나의 편집 가능한 문장 입력에서 직접 작성하거나 Home 추천 문장을 이어서 수정할 수 있으며, 입력을 멈추면 표준 발음을 자동 준비한다.
- 표준 발음 준비가 끝난 현재 문장에는 표준 발음을 항상 바로 보여주고 `Listen`과 녹음을 제공한다. Practice에서는 번역·학습 팁·완료 배지·표준 발음 토글을 두지 않으며, 음절별 상세 가이드는 평가 후 Result에서만 제공한다.
- Result의 `Pronunciation guide`는 `Sentence`와 `Standard pronunciation`만 비교해 보여주며 사용자 인식 문장은 표시하지 않는다. 표준 발음은 기기 TTS의 `Normal`과 `Slow`로 들을 수 있고 음원 파일은 별도로 보존하지 않는다.
- Result와 Review는 점수가 있는 단어 chip을 먼저 표시한다. 단어를 선택하면 그 단어에 속한 음절만 아래 카드에 펼치며, 음절은 점수 없이 입·혀 가이드 진입점으로만 사용한다. 선택한 URL이 지원 영상이면 음소 단위로 음소거 반복 재생하고, 현재 PNG mapping 등 정적 URL이면 이미지로 표시한다.
- Recording과 Evaluating은 하단 내비게이션을 숨기며, 평가 중에는 `Continue in background`로 Home에 돌아갈 수 있다.
- Result와 Review의 단어 점수·음절 grouping은 평가 API 응답을 그대로 사용하며 단어 점수를 음절에 복제하지 않는다.
- Review 그래프는 API가 최신순으로 제공한 기록 중 최근 7개를 선택해 오래된 점수부터 최신 점수 순으로 표시한다.
- Profile은 서버가 지원하는 학습 환경설정, 로그아웃과 회원 탈퇴만 제공한다.

## 검증

- 디자인 토큰은 `app/test/design_system_test.dart`에서 고정한다.
- 화면 기능과 작은 화면·큰 글자 회귀는 `app/test/widget_test.dart`에서 검증한다.
- 실제 iPhone Safe Area, 키보드와 네트워크 연동은 시뮬레이터 또는 실기기에서 별도로 확인한다.
