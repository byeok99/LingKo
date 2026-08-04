# 작업 이력

## 2026-08-04 - 단어 선택형 Result·Review 디자인

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: 음절 점수 grid를 단어 점수 선택과 하위 음절 가이드 카드로 교체했다.
- 검증: Flutter widget test 및 preview markup 키워드 확인
- 리스크: 실제 기기 시각 검수 필요

## 2026-08-03 - 에너지 우측 슬롯과 내부 여백 분리

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: 166px 투명 우측 슬롯과 내용 기반 capsule을 분리하고 capsule padding 8px을 명시했다.
- 검증: prototype script 구문 검사 통과
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 내부 여백 반영

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: 가변 폭 capsule의 좌우 padding을 8px로 맞췄다.
- 검증: prototype script 구문 검사 통과
- 리스크: 없음

## 2026-08-03 - MAX 캡슐 가변 폭 시안

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: 고정 grid를 내용 기반 flex로 바꿔 `+`가 숨겨질 때 오른쪽 빈 열이 남지 않게 했다.
- 검증: prototype script 구문 검사 통과
- 리스크: 없음

## 2026-08-03 - 에너지 횟수 표기 정리

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: capsule의 현재/최대 횟수 예시와 prototype을 공백 없는 `5/5` 형식으로 맞췄다.
- 검증: prototype script 구문 검사 통과
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 디자인 재검토

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: capsule을 166×40px로 줄이고 흰 표면·soft blue accent로 Home 카드 계층과 맞췄다.
- 검증: 500×924 Chrome 렌더 육안 확인 및 prototype script 구문 검사 통과
- 리스크: 실제 기기 최종 확인 필요

## 2026-08-03 - 소형 에너지 시안 재복원

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: 전체 폭 시안 변경을 취소하고 LingKo 우측 48px capsule 디자인으로 되돌렸다.
- 검증: prototype script 구문 검사 및 Flutter 전체 test 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - 에너지 캡슐 시안 롤백

- 변경 파일: `README.md`, `preview.html`, `WORK_LOG.md`
- 내용: Home 상단 우측 소형 배치를 인사말 아래의 전체 폭 capsule 시안으로 복원했다.
- 검증: prototype script 구문 검사 및 Flutter widget test 통과
- 리스크: 브라우저·실기기 최종 시각 확인 필요

## 2026-08-03 - Home 상황별 추천 시안 반영

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: 여섯 상황 칩, 카테고리별 3개 미리보기·전체 보기, 직접 입력 진입과 진행 중 평가 우선 배치를 프로토타입과 디자인 기준에 반영했다.
- 검증: inline JavaScript 구문 검사, 500×924 Chrome 렌더링 확인, Flutter 전체 71개 테스트와 대조
- 리스크: 실제 iPhone의 시스템 글꼴과 가로 칩 스크롤 감각은 실기기 확인 필요

## 2026-08-03 - Profile 목표 레벨 시안 제거

- 변경 파일: `preview.html`, `WORK_LOG.md`
- 내용: Profile 시안에서 learner 배지와 Target level 설정을 제거하고 설정 제목을 Language preferences로 변경했다.
- 검증: inline JavaScript 구문 검사, Flutter 전체 70개 widget·unit test 통과
- 리스크: 브라우저 픽셀 렌더링은 미확인

## 2026-07-30 - 평가·Result 정보형 시안 복원

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: 아이콘 중심으로 축소했던 Evaluating과 Result를 설명형 단계, summary, 점수 범례와 상세 feedback이 있는 직전 시안으로 복원했다.
- 검증: inline JavaScript 구문 검사, Flutter 전체 70개 테스트와 대조
- 리스크: 모바일 브라우저와 실제 iPhone의 설명 밀도는 수동 확인 필요

## 2026-07-30 - 가이드 URL 매체 표시 기준

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 점수와 독립된 입·혀 가이드가 영상 URL이면 반복 재생하고 PNG 등 정적 URL이면 이미지로 표시하는 기준을 추가했다.
- 검증: Flutter widget test와 대조
- 리스크: 현재 서버 mapping은 PNG

## 2026-07-30 - Result 점수·가이드 독립 노출 기준

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 음절 점수가 없어도 가이드 URL이 있으면 `—` 타일과 입·혀 가이드 진입을 제공하는 Result 정보 계층을 명시했다.
- 검증: `flutter analyze`, `flutter test` 전체 66개 통과 및 활성 문서 문구 대조
- 리스크: 프로토타입은 모든 음절에 예시 점수를 사용하므로 점수 없음 시각 상태는 Flutter에서 확인 필요

## 2026-07-30 - Practice 안내 밀도 축소 시안

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: 진행률·자동 준비 설명·완료 배지·번역·표준 발음 토글을 제거하고, 준비된 표준 발음을 항상 표시하는 간결한 Practice 기준으로 갱신했다.
- 검증: 이전 토글 참조 제거 및 `node --check` JavaScript 구문 통과
- 리스크: 실제 Flutter의 긴 표준 발음 높이는 iPhone에서 추가 확인 필요

## 2026-07-30 - Practice 단일 문장 입력 시안

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: 추천·직접 입력 내부 탭과 수동 확정 버튼을 제거하고, 하나의 편집 가능한 문장 카드에서 700ms 후 표준 발음을 자동 준비한 뒤 듣기·녹음을 여는 기준으로 갱신했다.
- 검증: 프로토타입의 이전 탭·수동 제출 참조 제거, `node --check` JavaScript 구문 통과
- 리스크: 실제 표준 발음 문자열은 앱의 API 응답으로 확인 필요

## 2026-07-30 - Practice 표준 발음 선택 노출 시안

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: Practice에서 표준 발음을 자동 노출하지 않고 확인·숨김 토글로 선택 노출하는 시안과 녹음 진입 시 재숨김 동작을 반영했다.
- 검증: Chrome headless 500x924 캔버스에서 402px phone 펼침 상태 렌더링 확인
- 리스크: 없음

## 2026-07-30 - Result 표준 발음 카드 시안 반영

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: Practice 시안에서 로마자·가이드 칩을 제거하고 Result에 Sentence·Standard pronunciation·Normal·Slow 카드만 배치하는 기준을 반영했다.
- 검증: Chrome headless 500x924 캔버스에서 402px phone Result 렌더링 확인
- 리스크: 실제 Flutter 시스템 글꼴의 긴 문장 줄바꿈은 iPhone에서 추가 확인 필요

## 2026-07-30 - Practice 듣기 동작 명시

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 장식용으로 제외했던 재생 속도를 실제 기기 TTS 기반 Normal·Slow 동작으로 전환하고 별도 음원을 보존하지 않는 기준을 기록했다.
- 검증: 앱 전체 widget test와 iOS·Android Debug build
- 리스크: 없음

## 2026-07-30 - Review 그래프 데이터 기준 명시

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 최근 7개 평가 점수를 오래된 순서에서 최신 순서로 표시하는 Review 그래프 계약을 디자인 기준에 추가했다.
- 검증: 앱 최근 점수 정렬 단위 테스트 통과
- 리스크: 없음

## 2026-07-30 - Flutter 앱 디자인 기준 연결

- 변경 파일: `preview.html`, `README.md`, `WORK_LOG.md`
- 내용: 새 프로토타입을 Flutter 앱의 시각 기준으로 명시하고 실제 API가 없는 장식 기능을 제외하는 구현 원칙을 기록했다.
- 검증: 화면별 Chrome 렌더링 7종 확인, Flutter 디자인 토큰 테스트 통과
- 리스크: 실제 iPhone Safe Area·키보드·네트워크 상태는 수동 확인 필요
## 2026-08-03 - Home 에너지 캡슐 시안 갱신

- 변경 파일: `README.md`, `preview.html`
- 내용: LingKo와 같은 줄 우측에 작은 마이크·횟수·timer/MAX·plus 캡슐을 배치했다.
- 검증: preview JS 구문 및 렌더 이미지 육안 확인
- 리스크: 실제 기기별 시각 확인 필요
