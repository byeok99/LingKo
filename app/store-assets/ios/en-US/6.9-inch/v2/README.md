# LingKo App Store Screenshot Set V2

상위 발음·언어 학습 앱의 현재 App Store 소개 이미지 패턴을 참고해 다시 설계한 권장 업로드 세트입니다.

- 크기: `1320 × 2868 px`
- 형식: PNG, RGB, alpha channel 없음
- 방향: 한 장당 하나의 메시지, 크게 확대한 실제 기능, 짧고 구체적인 설명
- 화면 데이터: 실제 사용자가 아닌 마케팅용 가상 학습 데이터

## 권장 업로드 순서

1. `01-speak-korean-know-what-to-fix.png`
2. `02-hear-before-you-speak.png`
3. `03-ten-second-focused-take.png`
4. `04-understand-your-score.png`
5. `05-see-how-hae-is-made.png`

## 참고한 상위 앱 패턴

- [ELSA Speak](https://apps.apple.com/us/app/elsa-speak-english-learning/id1083804886): 기능명을 큰 헤드라인으로 먼저 전달하고, 말하기 속도·멈춤·발음처럼 결과 데이터를 화면보다 크게 시각화한다.
- [BoldVoice](https://apps.apple.com/us/app/boldvoice-accent-training/id1567841142): 한 장에 하나의 이점을 두고, 발음 결과와 조음 방법을 확대된 UI 및 구체적인 교정 문장으로 증명한다.
- [Teuida](https://apps.apple.com/us/app/teuida-learn-languages/id1457532562): 첫 장에서 서비스 정체성을 짧고 큰 문구로 끝내고, 강한 단색 브랜드 배경과 실제 말하기 장면을 결합한다.

V2는 이를 그대로 복제하지 않고 `큰 한 문장 → 실제 LingKo 기능 → 구체적 사용 결과`의 흐름으로 적용했다.

## 핵심 설명

- Accuracy: 목표 발음과 비교한 소리의 정확도
- Fluency: 문장 전체의 리듬과 속도가 얼마나 자연스럽게 이어지는지
- Full sentence: 빠뜨린 어절 없이 문장을 얼마나 완전하게 말했는지
- Word score: 다음 연습 대상을 찾기 위한 신뢰 가능한 점수 단위
- Syllable guide: 점수 단위가 아니라, 선택한 음절의 정면 입술과 측면 혀 위치를 확인하는 탐색 단위

`compose.swift`는 실제 Flutter 캡처와 텍스트 없는 가이드 이미지를 합성하고, `ffmpeg`로 1320×2868 RGB PNG에 맞춥니다. 설명 문구는 생성형 이미지에 포함하지 않고 코드로 렌더링해 철자와 기능 의미를 고정합니다.

Apple 기준:

- [App Store Connect 스크린샷 사양](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)
- [App Review Guidelines 2.3 Accurate Metadata](https://developer.apple.com/app-store/review/guidelines/#accurate-metadata)
