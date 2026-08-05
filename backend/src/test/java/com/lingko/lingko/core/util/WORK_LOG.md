# 작업 이력

## 2026-08-05 - 로마자 변환 규칙 테스트

- 변경 파일: `KoreanRomanizationUtilTest.java`, `WORK_LOG.md`
- 내용: 음절·단어 경계, 문장부호 정규화, 빈 입력 계약을 고정했다.
- 검증: `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-07-30 - `김` 프레임 전환 mapping 검증

- 변경 파일: `SyllableMappingUtilTest.java`, `WORK_LOG.md`
- 내용: 실제 mapping에서 `김`이 `ㄱ·ㅣ·ㅁ`으로 분해되고 입 1개·혀 2개 전환 pair를 만드는 계약을 고정했다.
- 검증: 대상 테스트와 Backend 단위 199개·통합 11개 통과
- 리스크: asset mapping 추가 시 전환 pair 기대값 재검토 필요

## 2026-07-30 - `맛있겠다` 발음·정규화 회귀 테스트

- 변경 파일: `KoreanPhonemeUtilTest.java`, `PracticeSentenceNormalizerTest.java`, `WORK_LOG.md`
- 내용: `맛있겠다 → 마싣껟따` 대표음·경음화와 Unicode 문장부호·기호 제거 계약을 고정했다.
- 검증: 구현 전 실패 확인, Backend 단위 테스트 전체 190개 통과
- 리스크: 음운 예외 회귀 corpus는 지속 확장 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `KoreanPhonemeUtilTest.java`, `SyllableMappingUtilTest.java`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: Java 21에서 `./gradlew test integrationTest` 통과
- 리스크: 동작 변경 없음


## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
