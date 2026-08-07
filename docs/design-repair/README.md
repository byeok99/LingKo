# LingKo Blue 디자인 복구 기준

이 폴더는 [LingKo Blue Merged.dc.html](<LingKo Blue Merged.dc.html>)을 앱 UI의 현재 기준으로 사용한다. 기존 인증·평가·쿼터·저장 기능은 유지하고, Direction A에서 평면화됐던 화면을 LingKo의 파란 카드형 디자인 언어로 복구한다.

## 공통 디자인 계약

- 배경은 흰색, 본문 ink는 `#17324A`, primary는 `#2F73B9`를 사용한다.
- 카드 반경은 18px이며 `#DCE7EF` 테두리와 낮은 파란 회색 그림자로 면을 구분한다.
- 핵심 CTA는 높이 52px, 반경 15px, `#4387CA → #286EAE` 그라디언트를 사용한다.
- 화면 좌우 여백은 18px로 통일한다.
- 제목과 핵심 값은 900 굵기까지 사용하고, 로마자는 음절 경계를 읽을 수 있도록 굵기와 자간을 유지한다.
- 카테고리와 상태 선택은 밑줄 대신 파란 tint pill로 표현한다.
- 평가 점수는 80점 이상 파랑, 60점 이상 80점 미만 주황, 60점 미만 빨강으로 표시한다. 숫자·막대·점수에 종속된 강조 면은 같은 구간 의미를 공유한다.

## 화면별 적용 내용

| 화면 | 적용 기준 |
|---|---|
| Sign in | 파란 pronunciation sample 카드, 28px 헤드라인, 브랜드 로그인 버튼 |
| Home | compact 인사, 취약 음절 tile, 상황 pill, 하나로 묶은 추천 문장 카드, 우측 energy capsule |
| Practice | 문장 입력 카드, 파란 표준 발음 카드, Normal/Slow, gradient Record CTA |
| Recording | 파란 문장 카드, 실제 녹음 시간 ring·waveform, Cancel/Stop/Restart |
| Scoring | 실제 서버 단계 기반 ring과 단계 카드; 가짜 백분율 숫자는 표시하지 않음 |
| Result | 3단계 점수 카드, compact standard pronunciation 카드, 세로 어절 목록과 선택 행의 음절 guide, 하단 고정 재연습 CTA |
| Sound guide | 입·혀 media를 세로로 함께 표시하는 bottom sheet |
| Review | progress chart 카드, 하나로 묶은 최근 기록, Result와 같은 어절·음절 탐색 |
| Profile | account 카드, Your content/About 그룹 카드, 계정 동작 |
| Sound detail | sound summary 카드, Practiced/Suggested pill과 그룹 목록 |
| Saved | All/Daily/Travel/My own pill과 그룹 문장 카드 |

## 데이터와 기능 경계

- energy 수량과 countdown은 서버의 quota와 다음 충전 시각만 사용한다.
- 광고 SDK가 연결되기 전까지 `+`는 주입된 callback만 호출하며 성공을 가정하지 않는다.
- 어절 점수는 API 값을 표시하지만 음절에는 측정되지 않은 점수를 만들지 않는다.
- 채점 API가 백분율을 주지 않으므로 Scoring ring은 서버 단계만 표현하고 숫자를 표시하지 않는다.
- Apple 로그인은 구현 전까지 비활성 상태를 유지한다.
- Sound detail 모델에 입·혀 media 계약이 없으므로 동작하지 않는 guide 버튼을 추가하지 않는다. 기존 Result/Review의 실제 음절 guide 경로는 유지한다.

## 검증 기준

- `flutter analyze`
- `flutter test --coverage`
- 작은 화면·큰 글자 위젯 테스트
- 실제 기기에서 light/dark, 녹음 permission, TTS, Google 로그인, media 재생 수동 확인
