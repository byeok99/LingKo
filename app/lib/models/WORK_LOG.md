# Work Log

## 2026-08-12 - 광고 보상 session 모델 추가

- 변경 파일: `ad_reward_session.dart`, `WORK_LOG.md`
- 내용: SSV token, 만료 시각과 pending/completed/expired 상태를 엄격하게 매핑한다.
- 검증: API·widget 테스트 통과
- 리스크: 없음

## 2026-08-07 - 서버 법무 동의 상태 모델 추가

- 변경 파일: `legal_consent_status.dart`
- 내용: 서버가 판정한 현재 문서 버전과 재동의 필요 여부를 엄격하게 파싱하는 불변 모델을 추가했다.
- 검증: `flutter analyze`, `flutter test --coverage` 113개 통과(라인 81.26%)
- 리스크: 없음

## 2026-08-07 - 동의 선택 값 모델 추가

- 변경 파일: `consent_selection.dart`
- 내용: 동의 항목 3건과 문서 버전·동의 시각을 담는 불변 값 `ConsentSelection`과 문서 종류 enum `ConsentDocument`를 추가했다. 약관 개정 시 재동의 대상을 가리려면 동의 사실만으로 부족해 문서 버전을 함께 남긴다. 기기 시각은 변경 가능하므로 단독 증거로 쓰지 않는다는 점을 주석에 적었다. `consentDocumentVersion`은 `docs/legal/` 문서의 시행일과 같은 값을 유지한다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동의 저장 API가 없어 값이 서버로 전송되지 않는다

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `weak_word.dart` → `weak_sound.dart`
- 내용: `WeakWord`/`WordDetail`을 `WeakSound`/`SoundDetail`로 바꿨다. `averageScore`가 음절 측정값이 아니라 그 음절이 든 어절 점수들의 가중 평균이라는 점을 주석으로 남겼다. 앱은 서버 값을 다시 계산하지 않는다.
- 검증: `flutter analyze`, `flutter test` 87개 통과
- 리스크: 없음

## 2026-08-06 - 취약 어절 모델 추가

- 변경 파일: `weak_word.dart`, `practice_sentence.dart`
- 내용: WeakWord와 WordDetail을 정의하고, 추천 문장 응답을 목록과 같은 형태로 바꾸는 변환을 추가했다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 북마크를 켜는 진입점(Home·Result)은 아직 서버와 연결되지 않아 목록이 비어 보일 수 있음

## 2026-08-06 - 어절·음절 로마자 매핑

- 변경 파일: `practice_result.dart`, `practice_sentence.dart`
- 내용: 서버가 어절·음절마다 내려주는 romanization을 모델에 연결했다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 위젯 테스트는 이전 레이아웃 기준이라 갱신이 필요할 수 있음

## 2026-08-06 - 문장 모델에 로마자 추가

- 변경 파일: `practice_sentence.dart`
- 내용: 서버의 romanizedPronunciation을 받아 목록·결과 어디서나 병기할 수 있게 했다.
- 검증: `flutter analyze`, `flutter test` 84개 통과
- 리스크: 06 Result·03 Practice·09 Profile 리디자인과 신규 화면(10·11)은 후속 작업

## 2026-08-05 - 로마자 발음 모델 계약 추가

- 변경 파일: `practice_sentence.dart`, `practice_history.dart`, `WORK_LOG.md`
- 내용: 준비·추천·기록 응답의 `romanizedPronunciation`을 불변 모델로 매핑하고 재연습에도 보존한다.
- 검증: `flutter analyze`, `flutter test --coverage` 통과
- 리스크: 기존 실행 isolate는 class shape 변경으로 hot restart가 필요함

## 2026-08-04 - UserPreferences 모델 삭제

- 변경 파일: `user_preferences.dart` (삭제)
- 내용: displayLanguage에 이어 nativeLanguage도 소비처가 없어 모델 자체를 제거했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - UserPreferences에서 displayLanguage 제거

- 변경 파일: `user_preferences.dart`
- 내용: 다국어 지원 계획이 없어 UI 언어 필드를 제거했다. 학습 콘텐츠 기준 언어인 nativeLanguage는 성격이 달라 유지하고 주석으로 구분을 명시했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-04 - ScoreStatus enum과 fail-closed 파싱

- 변경 파일: `score_status.dart`, `practice_result.dart`, `practice_sentence.dart`
- 내용: 상태 문자열을 enum으로 승격하고, 모르는 서버 값은 모두 unavailable로 닫아 구버전 앱이 신뢰할 수 없는 점수를 노출하지 않게 했다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: 서버가 새 상태값을 추가하면 구버전 앱은 해당 점수를 감춘 채 동작함

## 2026-08-04 - scoreStatus 기본값 fail-open 제거

- 변경 파일: `practice_sentence.dart`, `practice_result.dart`
- 내용: CharacterResult의 scoreStatus 기본값을 AVAILABLE에서 UNAVAILABLE로 바꿔 미지정 생성이 신뢰 불가 점수를 노출하지 않게 하고, PracticeWordResult의 상태값 계약을 문서화했다.
- 검증: `./gradlew compileJava test`, `./gradlew integrationTest`, `flutter analyze`, `flutter test` 74개 통과
- 리스크: 기존 세 factory는 모두 값을 명시하고 있어 현재 화면 동작은 동일함

## 2026-08-04 - 단어 중심 평가 모델

- 변경 파일: `practice_result.dart`, `practice_history.dart`, `WORK_LOG.md`
- 내용: 단어 점수와 guide-only 음절 목록을 Result·Review 공통 모델로 매핑했다.
- 검증: `flutter test test/evaluation_api_test.dart` 통과
- 리스크: 실제 Azure 한국어 응답은 운영 환경 E2E 확인 필요

## 2026-08-04 - Review 음절 기록 응답 매핑 수정

- 변경 파일: `practice_sentence.dart`, `practice_history.dart`, `WORK_LOG.md`
- 내용: history API의 `text`, `feedback`, nullable `score`를 공통 `CharacterResult`로 변환하는 전용 mapping을 추가해 음절과 점수가 `—`로 표시되던 문제를 수정했다.
- 검증: history API mapping 회귀 테스트, `flutter analyze`, `flutter test --coverage` 전체 73개 통과, line coverage 85.20%
- 리스크: 과거 기록에 `score` 값이 없으면 기존 정책대로 점수 미제공 표시

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
## 2026-08-03 - 시간 충전 쿼터 모델 반영

- 변경 파일: `practice_quota.dart`
- 내용: 서버 기준 `nextRefillAt`, `serverTime`으로 남은 충전 시간을 계산하도록 변경했다.
- 검증: API 및 widget test 통과
- 리스크: 없음
