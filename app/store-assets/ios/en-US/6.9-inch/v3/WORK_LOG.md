# 작업 이력

## 2026-09-05 - App Store 제출용 앱 설명과 심사 메모 추가

- 변경 파일: `README.md`, `WORK_LOG.md`
- 내용: 현재 앱 기능에 맞춘 ko-KR/en-US 부제·프로모션 문구·설명·키워드와 심사팀용 접근 절차를 추가했다. 심사용 코드와 연락처는 저장소에 남기지 않도록 placeholder로 유지했다.
- 검증: Apple 공식 메타데이터 제한과 현재 Flutter 기능·심사용 로그인·보상형 광고 흐름을 대조하고 문자·byte 제한을 확인함
- 리스크: 제출 전 운영 심사용 코드, 연락처, 공개 Privacy Policy URL·Support URL을 App Store Connect에서 입력하고 실기기 전체 흐름을 다시 확인해야 함

## 2026-09-05 - 04번 WordSyllableExplorer를 현재 앱 디자인으로 교체

- 변경 파일: `04-know-what-every-score-means.png`, `compose.swift`, `WORK_LOG.md`
- 내용: 04번의 이전 Result 원본을 현재 Flutter 구현에서 다시 캡처한 `04-result-current.png`로 교체했다. 선택된 `한구거를` 행 아래에 네 음절 칩이 모두 표시된다.
- 검증: Flutter golden 캡처 테스트 통과, V3 재합성 성공, 1320×2868 `rgb24` 및 현재 앱과의 시각 일치 확인 완료
- 리스크: 없음

## 2026-09-05 - 실제 앱 UI 및 운영 서버 가이드 기반 V3 제작

- 변경 파일: `README.md`, `compose.swift`, V3 PNG 5장, `WORK_LOG.md`
- 내용: 마케팅 장식과 생성형 조음 이미지를 제외하고 실제 Flutter UI가 화면 대부분을 차지하도록 재구성했다. 5번은 운영 서버가 `해`에 반환한 입·혀 PNG를 실제 `GuideSheet`로 캡처해 사용했다.
- 검증: 운영 `/api/pronunciation/prepare` 응답 확인, 전용 Flutter golden 캡처 테스트 통과, Swift 합성 재생성 성공. 5장 전체 시각 검수와 1320×2868 `rgb24` 검증 완료.
- 리스크: 운영 서버의 가이드 mapping이 변경되면 저장한 이미지와 응답 기록을 다시 동기화해야 함
