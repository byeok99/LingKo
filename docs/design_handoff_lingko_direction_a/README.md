# Handoff: LingKo — Direction A (전체 화면 리디자인)

## Overview
외국인 한국어 학습자를 위한 발음 교정 앱 LingKo의 화면 전체 리디자인이다.
기존 `docs/design/preview.html`의 정보 구조(Home · Practice · Recording · Evaluating · Result · Review · Profile)는 유지하고,
시각 위계 · 버튼 시스템 · 색 규칙 · 로마자 병기를 재정의했다.

대상 사용자는 **한글을 아직 읽지 못하는 학습자**다. 따라서:
- UI 언어는 영어로 고정한다. 한국어는 학습 콘텐츠일 때만 화면에 등장한다.
- 모든 한국어 문장·어절·음절에 로마자를 병기한다 (Revised Romanization, 음절은 하이픈, 어절은 공백).
- 어절 점수는 평가 API 응답을 그대로 쓰고 음절에 복제하지 않는다 (기존 `docs/design/README.md` 규칙 유지).

## About the Design Files
이 번들의 HTML은 **디자인 레퍼런스**다. 프로덕션 코드가 아니다.
LingKo는 Flutter 앱이므로, 이 HTML을 그대로 옮기지 말고 `app/lib/` 의 기존 구조
(`app/app_theme.dart`, `app/app_palette.dart`, `screens/*.dart`)에 맞춰 위젯으로 재현한다.
색·타이포 값은 `app_palette.dart` / `app_theme.dart` 토큰으로 흡수한 뒤 화면에서 참조한다.

## Fidelity
**High-fidelity.** 색상 hex, 폰트 크기·굵기, 여백, 반경, 터치 영역이 모두 확정값이다.
디바이스 기준 프레임은 402 × 772 px (상태바 28px 제외한 본문 영역).

## Design Tokens

### Color
| Role | Hex | Usage |
|---|---|---|
| bg | `#FCFCFB` | 화면 배경 |
| surface | `#FFFFFF` | 카드 배경 (Result 점수 카드, Review 카드, 07 시트) |
| ink | `#1B2228` | 본문·제목 |
| textSecondary | `#5C7386` | 로마자·보조 텍스트 (대비 4.94:1) |
| textMuted | `#627585` | 비활성 탭·메타 정보 |
| line | `#E8E6E1` | 섹션 구분선 |
| lineSubtle | `#EFEDEA` | 리스트 행 구분선 |
| border | `#E0DED9` / `#D5D3CE` | 카드 테두리 / 버튼 테두리 |
| interactive | `#2F73B9` | **누를 수 있는 것에만** (primary 채움, 텍스트 버튼, 재생 아이콘) |
| interactiveDeep | `#245F9B` | 활성 탭·아이콘 |
| interactiveTint | `#EDF6FD` | 활성 탭 pill 배경, 충전(+) 배경 |
| neutralFill | `#F1F0ED` | 재생 원형 버튼 배경 |
| scorePass | `#27735A` / bg `#EEF4F1` | 점수 80 이상 |
| scoreFail | `#B94A4A` / bg `#FDEEEE` | 점수 80 미만 |
| recordAccent | `#C0453A` | 녹음 정지 사각형 |

**색 규칙 두 가지 (반드시 지킬 것)**
1. `#2F73B9` 계열은 인터랙티브 요소에만 쓴다. 비인터랙티브 텍스트·표준 발음 표기·상태 문구에 쓰지 않는다.
2. 점수 색은 **80 기준 2단계**다. 80 이상 `#27735A`, 80 미만 `#B94A4A`. 중간(노랑) 단계는 없다.
   낮은 점수의 어절은 숫자와 어절 텍스트를 같은 붉은색으로 칠한다.

### Typography
폰트: Noto Sans KR (한국어·영어 공용). 굵기는 400 / 500 / 600 / 700 네 단계만 쓴다 (기존 750~950 혼용 폐지).

| Token | Value | Usage |
|---|---|---|
| wordmark | 700 / 24px / -0.045em | LingKo |
| h1 | 700 / 26px / -0.035em | Home 인사, Sign in 헤드라인(31px) |
| screenTitle | 700 / 22px / -0.03em | Practice, Review, Profile 탭 제목 |
| navTitle | 700 / 17px / -0.025em | Recording, Result 상단 가운데 제목 |
| sentence | 700 / 19~22px / -0.02em | 문장 목록·문장 카드의 한국어 |
| word | 700 / 17px | Result 어절 |
| romanization | 500 / 10.5~13px / letter-spacing 0.12em | 로마자 (색 `#5C7386`) |
| eyebrow | 600 / 10.5px / 0.16em / uppercase | 섹션 라벨 (색 `#5C7386`) |
| body | 400 / 12.5~13px / 1.55~1.75 | 보조 설명 |
| button | 700 / 15px | 모든 버튼 라벨 |
| score | 700 / 60px / -0.05em, tabular-nums | Result 총점 |

### Spacing / Shape
- 화면 좌우 패딩 20px, 상단 16px
- 섹션 간격 14~16px, 구분선 위아래 12px
- 버튼: 반경 12px / 높이 54px
- 카드: 반경 16px / 테두리 1px `#E0DED9` / **그림자 없음**
- 리스트 행: 상하 패딩 12~15px + 1px 구분선
- 하단 탭바: 높이 76px, 상단 1px 구분선
- 최소 터치 영역 44 × 44px (시각 요소가 더 작아도 히트 영역은 44px 유지 — 충전 + 버튼이 그 예)

## Button System (기존 디자인의 최대 문제였던 부분)
**규칙: 그라디언트 금지 · 그림자 금지 · 반경 12px 고정 · 높이 54px 고정 · 라벨 700/15px 고정.**
위계는 재질이 아니라 *채움 → 선 → 글자* 세 단계로만 표현한다.

| Level | Spec | Rule |
|---|---|---|
| 1 Primary | bg `#2F73B9`, 흰 글자 | **화면당 하나만** |
| 2 Secondary | 투명 + 1px `#D5D3CE`, ink 글자 | 동등한 선택지 (Normal / Slow 등) |
| 3 Text | 글자만, `#2F73B9` | 부수적 이동 |
| Disabled | bg `#F1F0ED`, 글자 `#627585` | 형태 유지, 채움만 약화 |
| Destructive | 투명 + 1px `#DCC3C3`, 글자 `#B94A4A` | 채우지 않는다 |

**원형 컨트롤은 두 가지 예외만 허용**한다:
- 재생: 44 × 44, 반경 50%, bg `#F1F0ED`, 아이콘 `#2F73B9`
- 녹음 정지: 68 × 68, 반경 50%, 1px `#D5D3CE` 테두리, 내부 24px 사각형 `#C0453A`

아이콘은 텍스트 글리프(⌂ ● ▣ ◉)를 쓰지 않고 **1.7px stroke SVG**를 쓴다 (탭바 4개, 필터 등).

## Changes since the first handoff
- 화면이 9개 → **11개**로 늘었다 (10 Sound detail, 11 Saved sentences 추가).
- **음성 재생은 03 Practice에서만 한다.** 문장 목록(02/10/11)과 06 Result의 재생 컨트롤을 모두 제거했다. 07 Sound guide 시트의 음절 재생만 예외로 남아 있다 (도해와 소리가 함께 있어야 가이드가 성립).
- 01 Sign in에 **Continue with Google**(흰 배경 + 컬러 G 마크) / **Continue with Apple**(검정 배경 + 흰 로고) 두 버튼을 둔다. 브랜드 가이드를 따르되 반경 12 / 높이 54는 유지한다.
- 02 Home의 취약 음절은 **하나 → 셋**으로 늘려 가로 타일 3개로 보여준다. 타일을 누르면 10 Sound detail로 간다.
- 문장에 **북마크(저장)** 를 붙였다. 02 Home 행과 06 Result 헤더에서 켜고, 11 Saved의 행에서 다시 누르면 해제된다. 모아보기는 09 Profile → Your content → Saved sentences 로 들어간다. Home에는 Saved 필터를 두지 않는다.
- 05 Scoring에서 "Continue in background" 버튼을 없앴다. 아래로 스와이프해 빠져나가는 방식이다.

## Screens / Views

### 01 Sign in
- **Purpose**: Google 로그인 진입.
- **Layout**: flex column. 상단 워드마크, 중앙 정렬 영역(헤드라인 31px + 보조문 1줄 + 구분선 사이 `안녕하세요` 40px + 로마자), 하단 primary 버튼 + 각주.
- **Copy**: "Fix your Korean pronunciation, one syllable at a time." / "Say a sentence. See which syllables drifted, and hear how they should sound." / 버튼 "Continue with Google" / 각주 "Recordings are deleted right after scoring."

### 02 Home
- **Purpose**: 오늘 연습할 문장 고르기 + 남은 시도 확인.
- **Header**: 워드마크(24px) 좌, 우측에 **에너지 캡슐** — 1px `#D5D3CE` 테두리 pill 안에 [마이크 SVG + `3/5` + 세로 구분선 + `42:18` + 충전(+)]. + 는 시각 28px / 히트 44px.
- **Body**: 인사 h1 26px 2줄 → 구분선 사이 "Sound to fix this week" (52px 라운드 사각형에 취약 음절 `씨`, 붉은 tint bg, 로마자 + 점수 + 교정 지시 1줄) → 카테고리 탭 5개(밑줄 2px 활성) → 문장 목록.
- **문장 행**: grid `1fr 44px`. 좌측 [한국어 19px / 로마자 / 영어 번역], 우측 44px **북마크 토글**. 취약 음절이 포함된 로마자는 해당 음절만 붉게.
  - 재생 컨트롤은 두지 않는다. 문장 재생은 03 Practice에서만 한다.
- **Pending 행**: 채점 중인 문장은 회색 한국어 + 로마자 + 우측에 [7px 파란 점 + "Scoring"]. 결과가 오면 그 자리에서 점수로 교체된다.
- **하단**: 텍스트 버튼 2개 ("Show 2 more" / "Type my own sentence") + 탭바.

### 03 Practice (탭)
- **Purpose**: 문장 확정 후 표준 발음 확인 → 녹음 시작.
- **주의**: 탭 화면이므로 Close 버튼이 없고 **탭바가 있다**. 제목은 좌측 정렬 22px.
- **Body** (상단 정렬): "Your sentence · tap to edit" 라벨 + 입력 카드(1px 테두리, 흰 배경, 한국어 21px + 캐럿) → 구분선 사이 "How it should sound"(표준 발음 24px ink + 로마자 13px) → Normal / Slow secondary 버튼 2개.
- **번역문은 표시하지 않는다.**
- **CTA**: primary "Record", 탭바 위 `bottom: 94px`.

### 04 Recording
- **Purpose**: 10초 안에 문장 전체를 말한다.
- **Layout**: 상단 `‹` + 가운데 "Recording". 중앙에 문장 카드(한국어 22px + 로마자 + 번역) → 194px 원형 타이머(conic-gradient `#2F73B9` 진행 / `#E7E5E0` 잔여, 내부 172px 흰 원에 `0:04` 40px + "/ 10 sec") → 웨이브폼 16개 바(4px, 반경 999px).
- **하단**: grid `1fr 1.2fr 1fr` — Cancel(48px 원형 ×) / Stop(68px 원형, 내부 붉은 사각형) / Restart(48px 원형 ↻). 라벨 600/11px.

### 05 Scoring
- **Purpose**: 채점 대기. **사용자를 붙잡아두지 않는 것이 설계 목표다.**
- **Layout**: 좌상단 `‹` + 가운데 "Scoring"(600/13px) → 헤드라인 "Listening to your pronunciation…" 25px → 안내 1줄 "Swipe down to keep practising — your score lands on Home." → 4단계 체크리스트(업로드 ✓ / 표준 비교 ✓ / 음절 채점 ● 진행 중 / 입·혀 가이드 준비 대기).
- 헤더는 04·06과 같은 `44px 1fr 44px` grid를 쓰되 제목 크기는 600/13px로 둔다. 실제 헤드라인이 25px이므로 navTitle(700/17px)을 쓰면 둘이 경쟁한다.
- **하단 CTA는 없다.** 벗어나는 방법은 좌상단 `‹`와 아래로 스와이프 두 가지이며, 둘 다 동작이 같다. 채점은 백그라운드에서 계속되고 02 Home의 pending 행이 진행 상태를 이어받는다.
  - 스와이프만 두면 발견 가능성이 낮아 이탈 방법을 모르는 사용자가 생긴다. `‹`가 보이는 탈출구 역할을 한다.
- 10초를 넘길 수 있으면 결과 도착 시 로컬 알림을 보낸다.

### 06 Result
- **Purpose**: 점수 확인 → 무엇이 틀렸는지 → 다시 말하기.
- **Header**: `‹` + 가운데 "Result". 공유 버튼 없음.
- **점수 카드**: 흰 배경 / 1px `#E0DED9` / 반경 16 / 패딩 16. grid `auto 1fr` — 좌측 총점 60px(색은 80 기준), 우측에 Accuracy · Fluency · Full sentence 게이지 3개(라벨 11px + 5px 바 + 숫자). 카드 하단 구분선 아래 판정 문구 1줄 600/13px.
  - "out of 100" 같은 표기는 쓰지 않는다.
- **How it should sound**: 원문 한국어 21px + 표준 발음 21px(`#5C7386`, 발음이 바뀌는 부분만 ink 700) + 로마자.
  - **사용자의 실제 발음을 문자로 재현해 보여주지 않는다** (보정 없이 정확히 추출할 수 없음).
- **By word**: "By word · tap to see its syllables" 라벨 + 어절 행 3개(어절 / 로마자 / 점수). 80 미만 어절은 텍스트와 점수 모두 `#B94A4A`.
  - 어절을 누르면 그 아래 **음절 칩**이 펼쳐진다(반경 11px, 문제 음절만 붉은 tint). 음절에는 점수를 표시하지 않는다.
  - 표기는 **발음형**을 쓴다 (좋아해요 → 조아해요, 좋 → 조).
- **CTA**: primary "Say it again" 하나만 둔다. 재생은 03 Practice로 일원화했으므로 Result에 "Hear it slow"를 두지 않는다.

### 07 Sound guide (bottom sheet over Result)
- **Purpose**: 음절 하나의 입·혀 모양 확인. 06의 음절 칩에서 진입한다.
- **Sheet**: 상단 반경 22px, 1px `#E0DED9` 상단선, 흰 배경, 40 × 4 그랩 핸들. 뒤 Result는 opacity 0.32.
- **Header**: 음절 44px + 로마자 24px(`#5C7386`) + 우측 44px 재생 원형. 자모 분해(ㅈ + ㅗ)는 표시하지 않는다.
- **Body**: 세로 스택 도해 2장 (각 214px, 반경 12px) — **위가 입 모양(LIPS · FRONT VIEW), 아래가 혀(TONGUE · SIDE VIEW)**. 가로 2열 배치는 너무 작아 따라할 수 없으므로 쓰지 않는다.
- 조음 팁 문장·최소대립쌍(자/차/짜)·CTA는 없다.
- **Assets**: 현재 해치 패턴 플레이스홀더. `docs/design/README.md` 규칙대로 지원 영상 URL이면 음소 단위 무음 반복 재생, 정적 PNG면 이미지로 표시한다.

### 10 Sound detail
- **Purpose**: 취약 음절 하나를 파고든다. 02 Home의 취약 음절 타일에서 진입한다.
- **Header**: `‹` + 가운데 "Sound".
- **음절 카드**: 64px 반경 16 타일(붉은 tint) + 로마자 15px + "Average 62 across 8 tries". 재생 버튼 없음.
- **secondary "See mouth guide"** → 07 시트를 띄운다.
- **탭**: Practiced / Suggested. 활성 탭의 내용만 렌더한다 (두 목록을 동시에 보여주지 않는다).
- **행**: grid `1fr 44px`. 문장에서 **대상 음절만 붉게**, 로마자에서도 해당 음절만 700으로 굵게 — 세 요소를 모든 행에서 동일하게 처리한다. 우측은 점수 + 날짜(Practiced) 또는 저장/이동(Suggested).
- **CTA**: primary "Practice 씨" (`bottom: 94px`, 탭바 위). 목록 마지막 행이 CTA를 침범하지 않도록 콘텐츠 영역 하단 여백을 확보한다.

### 11 Saved sentences
- **Purpose**: 북마크한 문장 모아보기. **09 Profile → Your content → Saved sentences** 로 진입한다.
- **Header**: `‹` + 가운데 "Saved", 그 아래 좌측에 개수("4 sentences"). 개수는 실제 목록 길이와 반드시 일치시킨다.
- **필터**: All / Daily / Travel / My own.
- **행**: grid `1fr 44px` — [한국어 19px + 로마자 + 영어 번역] + 북마크(채움 상태, 누르면 해제). 재생 없음.
- **탭바**: **Profile이 활성**이다 (Home이 아니다).

### 08 Review (탭)
- **Purpose**: 점수 추세와 과거 시도 확인. 행을 누르면 해당 Result로 간다.
- **Header**: "Review" 22px 좌측 정렬. **우측 버튼 없음.**
- **Trend 카드**: eyebrow "Your progress · Last 7 tries" + SVG 라인 차트(viewBox 320×118, 기준선 3개, polyline 2.4px `#2F73B9`, 지점 원 7개 중 최신만 채움 4.5r, 최신 점수 라벨, 좌우에 Jul 28 / Today).
  - 데이터 계약: API 최신순 기록 중 최근 7개를 **오래된 → 최신** 순으로 표시한다.
- **Recent history**: eyebrow + 카드 리스트. 행은 grid `44px 1fr 22px` — 44px 반경 14px 점수 배지(80 기준 색) / [한국어 16px + 로마자 + 날짜] / `›`.

### 09 Profile (탭)
- **Purpose**: 계정 확인, 정책 열람, 로그아웃·탈퇴. **아직 학습 환경설정 기능은 없다.**
- **Layout**: "Profile" 22px → 계정 블록(58px 반경 16 이니셜 타일 `#EDF6FD`/`#245F9B` + 이름 18px + 이메일 + Google 배지 pill) → "Your content" 섹션 → "Legal & privacy" 섹션 4행(Terms of Service / Privacy Policy / Ad privacy settings / Contact us) → "About" 섹션 1행(About LingKo 1.0.0) → 하단 secondary "Sign out" + destructive "Delete account" → 탭바. 각 행 우측 `›`.
- **Legal & privacy**: 약관·처리방침은 백엔드가 서빙하는 공개 URL을 앱 내부 WebView로 연다. 광고 설정과 문의는 아직 연결할 기능이 없어 눌리지 않는 흐린 상태로 둔다. 원래 시안의 "Audio & privacy" 행은 실제 처리방침 행으로 대체됐다.
- "Your content" 섹션에 **Saved sentences** 행(북마크 아이콘 + 개수 + `›`)을 두고 11로 연결한다.
- Language preferences 섹션은 넣지 않는다 (계획 없음). 저장만 되고 읽는 코드가 없어 `V16`에서 컬럼과 API까지 제거했다.

## Interactions & Behavior
- Home 문장 행 탭 → 03 Practice. 행 우측 북마크 탭 → 저장 토글(행 이동 없음).
- 03 Record → 04 Recording. Stop 탭 즉시 05로 전환되고 채점이 시작된다.
- 05는 좌상단 `‹` 또는 스와이프 다운으로 벗어날 수 있고, 채점은 백그라운드에서 계속된다. Home pending 행이 상태를 이어받는다.
- 06 어절 탭 → 음절 칩 펼침(아코디언, 한 번에 하나). 음절 칩 탭 → 07 시트 상승.
- 07 시트: 그랩 핸들 드래그 또는 배경 탭으로 닫힌다.
- 08 history 행 탭 → 해당 시도의 06 Result.
- 02 Home 취약 음절 타일 탭 → 10 Sound detail. 10의 "See mouth guide" → 07 시트.
- 02 / 06의 북마크 탭 → 저장 토글(화면 이동 없음). 09 Profile의 Saved sentences 행 → 11.
- 남은 시도가 0이면 03의 Record는 disabled 스타일이 되고, 헤더 캡슐의 충전(+) 동선으로 유도한다.

## State Management
- `remainingTries` / `rechargeAt` — 헤더 캡슐(3/5, 42:18)
- `selectedSentence` (기존 `state.selected` 대응), `customDraft`
- `homeCategory` — 카테고리 탭
- `recordSeconds`, `isRecording`
- `pendingEvaluationId` — 05에서 벗어난 뒤 Home pending 행을 그리기 위한 값
- `evaluation` — 총점, Accuracy/Fluency/Completeness, 어절 점수 배열, 어절별 음절 배열, 표준 발음, 로마자
- `expandedWordIndex` — 06 음절 펼침
- `guideSheet` — 07에 띄울 음절 + 도해 URL
- `history[]` — 08 리스트 및 최근 7개 추세
- `weakSounds[]` — 02 타일 3개(평균 점수 오름차순) 및 10의 데이터 원본
- `savedSentenceIds[]` — 북마크 상태 및 11 목록

## Assets
- 탭바 아이콘 4종(Home / Practice / Review / Profile)과 필터·재생 아이콘은 인라인 SVG, stroke 1.7px, 21px 박스. Flutter에서는 동일 형태의 Material outlined 아이콘으로 대체 가능하다.
- 입·혀 도해는 플레이스홀더다. 실제 자료(영상 또는 PNG) URL 규칙을 확정해야 한다.

## Files
- `LingKo Direction A Full.dc.html` — 11개 화면 전체 + 상단에 색 규칙·버튼 시스템 명세 카드
- `support.js` — 위 HTML 실행에 필요한 런타임 (브라우저에서 그냥 열면 된다)

기준으로 읽은 저장소 파일: `docs/design/preview.html`, `docs/design/README.md`, `app/lib/app/app_theme.dart`, `app/lib/app/app_palette.dart`, `app/lib/screens/home_screen.dart` (develop 브랜치).
