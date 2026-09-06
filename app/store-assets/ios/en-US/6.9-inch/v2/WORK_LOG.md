# 작업 이력

## 2026-09-05 - 상위 앱 패턴 기반 App Store 이미지 V2 제작

- 변경 파일: `README.md`, `compose.swift`, `01-speak-korean-know-what-to-fix.png`, `02-hear-before-you-speak.png`, `03-ten-second-focused-take.png`, `04-understand-your-score.png`, `05-see-how-hae-is-made.png`, `WORK_LOG.md`
- 내용: 한 장당 하나의 메시지와 확대된 기능 증거를 중심으로 5장 구성을 다시 설계했다. 점수 세부 기준과 정면 입술·측면 혀 가이드를 구체적인 문구로 설명했다.
- 검증: `rtk proxy swift .../v2/compose.swift` 재생성 성공. `ffprobe`로 5장 모두 1320×2868, `rgb24` 확인. 전체 이미지 방향·문구·기능 일치 시각 검수 완료.
- 리스크: 입·혀 이미지는 마케팅용 교육 일러스트이므로 출시 시 서버가 제공하는 실제 가이드 미디어와 최종 대조가 필요하다.
