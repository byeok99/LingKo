# 작업 이력

## 2026-08-03 - User Preferences 목표 레벨 계약 제거

- 변경 파일: `user_preferences.dart`, `WORK_LOG.md`
- 내용: 사용되지 않는 `LearningLevel`과 `targetLevel` JSON·불변 model 필드를 제거해 표시 언어와 모국어만 관리하도록 단순화했다.
- 검증: `flutter analyze`, `flutter test --coverage` 전체 70개 통과, line coverage 83.20%
- 리스크: 제거 전 API 응답의 추가 `targetLevel` field는 앱 parsing에서 사용되지 않음

## 2026-07-30 - Practice 문장 모델 정규화

- 변경 파일: `practice_sentence.dart`, `WORK_LOG.md`
- 내용: 추천·준비 API 응답과 앱 내부 문장이 Practice 상태에 들어오기 전에 문장부호·기호 제거 규칙을 적용하고 불변 복사본을 생성한다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 없음

## 2026-07-28 - 평가 진행 UI 상태 모델 추가

- 변경 파일: `evaluation_progress.dart`, `WORK_LOG.md`
- 내용: 업로드, 작업 생성, 분석, 피드백, 완료·실패 단계를 탭 간 공유하는 불변 상태를 추가했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 프로세스 종료 후 상태 영속화는 후속 작업 필요

## 2026-07-27 - 평가 작업 상태 모델 추가

- 변경 파일: `evaluation_job.dart`, `WORK_LOG.md`
- 내용: 업로드 티켓과 평가 작업 상태·결과 JSON을 앱 계약으로 표현하는 모델을 추가했다.
- 검증: `flutter analyze`, 대상 API 테스트 통과
- 리스크: 서버 오류 코드별 사용자 메시지는 후속 세분화 가능

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `auth_session.dart`, `practice_history.dart`, `practice_quota.dart`, `practice_result.dart`, `practice_sentence.dart`, `user_preferences.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - quota 모델 이력 이전

- 변경 파일: `practice_quota.dart`, `WORK_LOG.md`
- 내용: 일일 연습 quota API 응답을 Flutter에서 사용하는 모델로 변환하는 구조를 추가했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 백엔드 응답 계약 변경 시 모델 동기화 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
