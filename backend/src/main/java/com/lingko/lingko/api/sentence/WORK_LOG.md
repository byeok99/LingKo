# 작업 이력

## 2026-08-06 - 저장 토글을 PATCH로 변경

- 변경 파일: `SavedSentenceController.java`
- 내용: 앱 HTTP client에 PUT 전송 경로가 없어 기존 PATCH 경로를 쓰도록 맞췄다. 상태를 일부 바꾸는 동작이라 의미도 어긋나지 않는다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 북마크를 켜는 진입점(Home·Result)은 아직 서버와 연결되지 않아 목록이 비어 보일 수 있음

## 2026-08-06 - 저장 문장 API 추가

- 변경 파일: `SavedSentenceController.java`
- 내용: 저장 목록 조회와 토글 endpoint를 추가했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 취약 목록은 어절 단위다. 디자인의 음절 단위 표기는 화면에서 함께 조정해야 함

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `SentenceController.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
