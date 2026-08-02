# 작업 이력

## 2026-07-30 - 변환 API 원문 정규화

- 변경 파일: `EvaluationController.java`, `WORK_LOG.md`
- 내용: 발음 변환 응답의 원문도 서비스와 같은 Unicode 문장부호·기호 제거 결과를 반환하도록 맞췄다.
- 검증: Backend 단위 190개·통합 11개 통과
- 리스크: 없음

## 2026-07-27 - 비동기 평가 작업 API 추가

- 변경 파일: `EvaluationJobController.java`, `EvaluationResultController.java`, `WORK_LOG.md`
- 내용: 업로드 티켓·작업 생성·상태 조회 endpoint를 추가하고 기존 multipart endpoint는 기본 비활성화했다.
- 검증: Controller 테스트 및 Backend 전체 테스트 통과
- 리스크: 기존 클라이언트는 호환 설정 없이는 multipart endpoint를 사용할 수 없음

## 2026-07-24 - 인증 평가 통합 유스케이스 연결

- 변경 파일: `EvaluationResultController.java`, `WORK_LOG.md`
- 내용: 평가 생성 API가 활성 Bearer 세션의 사용자 ID로 평가·쿼터·결과 저장 통합 유스케이스를 호출하도록 변경했다.
- 검증: `EvaluationResultControllerTest`, `EvaluationApplicationFlowIntegrationTest`
- 리스크: 중복 요청 멱등성은 #39에서 추가 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `EvaluationController.java`, `EvaluationHistoryController.java`, `EvaluationResultController.java`, `GuideGenerationJobController.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 학습 기록 인증 목적 주석 보완

- 변경 파일: `EvaluationHistoryController.java`, `WORK_LOG.md`
- 내용: 활성 세션 사용자 소유 기록만 조회하는 컨트롤러 목적을 명시했다.
- 검증: Backend 전체 테스트
- 리스크: 동작 변경 없음

## 2026-07-23 - 활성 로그인 세션 인증 적용

- 변경 파일: `EvaluationHistoryController.java`, `WORK_LOG.md`
- 내용: 학습 기록 조회의 Bearer Token 검증을 공통 활성 세션 인증기로 위임했다.
- 검증: `EvaluationHistoryControllerTest`
- 리스크: 평가 생성 API 인증은 별도 요구사항 범위

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
