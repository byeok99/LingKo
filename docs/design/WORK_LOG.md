# 작업 이력

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
