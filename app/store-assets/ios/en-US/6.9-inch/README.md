# LingKo App Store Screenshot Set

> 권장 업로드본은 실제 앱 UI와 운영 서버 가이드를 사용한 [`v3/`](./v3/)입니다. `v2/`는 생성형 조음 이미지가 포함된 거절된 비교본이며 업로드하지 않습니다. 아래 파일은 1차 비교본으로 보존합니다.

App Store Connect의 iPhone 6.9-inch portrait 슬롯에 바로 업로드할 수 있는 영문 스크린샷 세트입니다.

- 크기: `1320 × 2868 px`
- 형식: PNG, RGB, alpha channel 없음
- 순서: Home → Practice → Recording → Result → Sound Guide
- 화면 데이터: 실제 사용자가 아닌 마케팅용 가상 학습 데이터

## 업로드 파일

1. `01-speak-with-confidence.png`
2. `02-hear-the-right-pronunciation.png`
3. `03-record-speak-improve.png`
4. `04-know-what-to-fix.png`
5. `05-master-every-syllable.png`

## 제작 기준

실제 Flutter 위젯을 6.9-inch 해상도로 렌더링하고, LingKo의 Blue·Mint 팔레트와 텍스트 없는 생성형 음성 비주얼을 배경으로 합성했습니다. 마케팅 문구는 이미지 생성 모델에 맡기지 않고 별도로 렌더링해 철자와 가독성을 보장했습니다.

배경 생성 prompt:

> Premium abstract Korean pronunciation-practice background; airy off-white canvas, soft sky-blue and mint gradients, flowing sound-wave ribbons, subtle acoustic dots and Hangul-inspired non-readable geometry; clean negative space; no text, letters, logos, UI, phones, people, or watermark.

Apple 기준:

- 스크린샷은 실제 앱 사용 화면을 중심으로 보여주며 텍스트 overlay는 기능 설명에만 사용합니다.
- 출시 시 앱 UI나 기능이 바뀌면 대응하는 원본 화면과 문구도 함께 갱신합니다.
- [App Store Connect 스크린샷 사양](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)
- [App Review Guidelines 2.3 Accurate Metadata](https://developer.apple.com/app-store/review/guidelines/#accurate-metadata)
