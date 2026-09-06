# LingKo App Store Screenshot Set V3

실제 LingKo Flutter UI를 화면 대부분에 배치하고, 운영 서버가 반환한 `해` 입·혀 가이드를 사용한 권장 업로드 세트입니다.

- 크기: `1320 × 2868 px`
- 형식: PNG, RGB, alpha channel 없음
- 디자인: 앱의 흰색·Soft Blue·Primary Blue 팔레트만 사용
- 가이드 출처: `POST https://lingko-api.duckdns.org/api/pronunciation/prepare`
- 요청 문장: `저는 한국어를 공부해요`
- 서버 표준 발음: `저는 한구거를 공부해요`

## 권장 업로드 순서

1. `01-practice-what-matters.png`
2. `02-hear-the-exact-pronunciation.png`
3. `03-record-with-confidence.png`
4. `04-know-what-every-score-means.png`
5. `05-use-the-real-mouth-and-tongue-guide.png`

## 서버에서 확인한 `해` 가이드

- 음절: `해`
- 로마자: `hae`
- 입 정면: `guides/mouth/semi-vowel-y.png`
- 혀 측면: `guides/tongue/vowel-e-ae.png`
- 앱 표시: 실제 `GuideSheet`가 두 이미지를 세로로 함께 표시

V3는 생성형 입·혀 이미지를 사용하지 않습니다. 5번 원본은 서버 PNG 두 장을 실제 `GuideSheet`에 주입해 Flutter golden 캡처로 생성했습니다.

## App Store Connect 제출 문구

아래 문구는 현재 앱에 구현된 기능만 설명합니다. App Store Connect의 설명은 일반 텍스트만 지원하므로 각 코드 블록의 내용만 복사합니다. 대괄호 placeholder는 제출 전에 실제 값으로 바꾸고, 심사용 접근 코드는 저장소에 커밋하지 않습니다.

Apple 입력 제한:

- 부제: 30자 이하
- 프로모션 문구: 170자 이하
- 앱 설명: 4,000자 이하
- 키워드: 쉼표를 포함해 100 bytes 이하이며 앱 이름과 회사 이름은 반복하지 않음
- 참고: [App information](https://developer.apple.com/help/app-store-connect/reference/app-information/app-information), [Platform version information](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information/)

### 한국어 (`ko-KR`)

#### 부제

```text
한국어 발음을 듣고 녹음하고 교정하세요
```

#### 프로모션 문구

```text
한국어 문장을 듣고 직접 녹음해 보세요. 발음 정확도, 유창성, 문장 완성도와 단어별 결과를 확인하고 입·혀 모양 가이드로 어려운 음절을 다시 연습할 수 있습니다.
```

#### 앱 설명

```text
LingKo는 한국어 학습자가 자신의 발음을 듣고, 녹음하고, 구체적인 피드백으로 다시 연습할 수 있도록 돕는 발음 학습 앱입니다.

연습할 문장을 고르세요
추천 문장을 선택하거나 연습하고 싶은 한국어 문장을 직접 입력할 수 있습니다. LingKo는 실제 발음 규칙을 반영한 표준 발음과 로마자 표기를 함께 보여 줍니다.

듣고 녹음하세요
표준 발음을 들으며 문장의 소리와 리듬을 익힌 뒤, 앱에서 바로 자신의 발음을 녹음할 수 있습니다.

점수를 구체적으로 이해하세요
녹음을 제출하면 전체 점수와 함께 다음 항목을 확인할 수 있습니다.
• 정확도: 기대한 한국어 소리에 얼마나 가깝게 발음했는지
• 유창성: 문장을 얼마나 자연스러운 속도와 리듬으로 말했는지
• 문장 완성도: 문장의 예상 단어를 얼마나 빠짐없이 발음했는지
• 단어별 점수: 다시 연습할 단어를 찾기 위한 세부 결과

입과 혀의 움직임을 확인하세요
단어를 펼쳐 음절을 선택하면 해당 소리를 만들 때 참고할 입 정면과 혀 측면 가이드를 확인할 수 있습니다. 음절 가이드는 발음 방법을 연습하기 위한 자료이며 단어 점수를 음절 점수처럼 표시하지 않습니다.

학습 기록을 이어 가세요
이전 연습 결과를 다시 확인하고, 표시 언어와 학습 설정을 관리할 수 있습니다.

이용 안내
• 발음 평가에는 계정과 인터넷 연결이 필요합니다.
• 녹음 기능을 사용할 때 마이크 권한이 필요합니다.
• 평가 기회가 부족한 경우 선택적으로 보상형 광고를 시청해 기회를 받을 수 있습니다.
• 발음 평가는 학습을 돕기 위한 피드백이며 공인 언어 능력 평가가 아닙니다.
```

#### 키워드

```text
한국어,발음,말하기,받아쓰기,언어학습,스피킹,한글,회화
```

### English (`en-US`)

#### Subtitle

```text
Korean Pronunciation Coach
```

#### Promotional text

```text
Listen, record, and understand your Korean pronunciation with sentence scores, word-level feedback, and real mouth and tongue guides for each syllable.
```

#### Description

```text
LingKo helps Korean learners listen to their pronunciation, record their voice, and practice again with specific feedback.

CHOOSE WHAT TO PRACTICE
Select a recommended sentence or enter your own Korean text. LingKo shows a standard pronunciation that reflects Korean sound rules, together with romanization.

LISTEN AND RECORD
Listen to the standard pronunciation to learn the sounds and rhythm, then record your own pronunciation directly in the app.

UNDERSTAND YOUR RESULTS
After you submit a recording, LingKo provides an overall result with:
• Accuracy — how closely your pronunciation matches the expected Korean sounds
• Fluency — how naturally you speak the sentence in terms of pace and rhythm
• Full sentence — how completely the expected words were pronounced
• Word scores — detailed results that help you decide what to retry

SEE HOW EACH SYLLABLE IS MADE
Expand a word and select a syllable to view front-of-mouth and side-of-tongue guidance for producing that sound. Syllable guides explain articulation; they do not present a word score as if it were a measured syllable score.

KEEP YOUR PRACTICE HISTORY
Review previous results and manage your display language and learning preferences.

PLEASE NOTE
• An account and internet connection are required for pronunciation assessment.
• Microphone permission is required when you use the recording feature.
• If you need another assessment opportunity, you may optionally watch a rewarded ad.
• Results are learning feedback and are not an official language proficiency assessment.
```

#### Keywords

```text
korean,pronunciation,speaking,language,learning,hangul,conversation,accent,practice
```

## App Review Notes 초안

App Review 정보는 사용자에게 공개되는 앱 설명과 별도입니다. Apple은 로그인이나 특별 설정이 필요한 경우 심사팀이 전체 기능에 접근할 수 있는 정보를 이 필드에 제공하도록 안내합니다. 아래 `[REVIEW_ACCESS_CODE]`만 제출 직전에 운영 Secret과 일치하는 실제 코드로 교체합니다.

```text
LingKo is a Korean pronunciation practice app. An account and an internet connection are required to access the full evaluation flow.

REVIEW ACCESS
1. Launch the app and remain on the sign-in screen.
2. Tap the LingKo wordmark five times within three seconds.
3. Enter this review access code: [REVIEW_ACCESS_CODE]
4. Accept the required Terms of Service and Privacy Policy if the consent screen appears.

CORE FLOW
1. On Home, choose a recommended Korean sentence, or open Practice and enter custom Korean text.
2. Listen to the standard pronunciation.
3. Tap Record. Microphone permission is requested only when recording is used.
4. Stop the recording and submit it for assessment. Processing requires a network connection and may take some time.
5. On Result, review the overall, Accuracy, Fluency, and Full sentence scores.
6. Under Pronunciation by word, expand a word and tap a syllable to open its mouth and tongue guide.
7. Previous assessments are available from the Review tab. Account deletion is available from Profile.

REWARDED ADS
The plus button beside the assessment quota opens an optional rewarded ad. A completed reward grants an additional assessment opportunity through server-side verification. The app does not require an in-app purchase to access this flow.

AUDIO AND NETWORK USE
Recorded audio is uploaded only after the user submits it for pronunciation assessment. The app uses the network to load practice content, process assessments, display articulation guides, and retrieve practice history.

If review access does not work, please contact: [REVIEW_CONTACT_EMAIL]
```

심사 제출 직전에는 다음을 확인합니다.

- 실제 기기에서 심사용 로그인 코드, 약관 동의, 녹음, 평가 완료, 가이드 열기까지 실행
- `[REVIEW_ACCESS_CODE]`와 `[REVIEW_CONTACT_EMAIL]` 교체
- App Store Connect의 Privacy Policy URL과 Support URL이 공개 HTTPS에서 열리는지 확인
- 보상형 광고가 심사 환경에서 로드되지 않더라도 기본 평가 흐름을 검토할 수 있도록 심사 계정의 평가 기회 확인
