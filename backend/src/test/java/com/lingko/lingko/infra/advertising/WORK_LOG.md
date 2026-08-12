# 작업 이력

## 2026-08-12 - AdMob 공개키 provider 생성자 주입 회귀 테스트

- 변경 파일: `GoogleAdMobPublicKeyProviderTest.java`, `WORK_LOG.md`
- 내용: 실제 Spring 컨테이너가 운영용 생성자를 선택해 provider bean을 만들 수 있는지 검증한다.
- 검증: 수정 전 bean 생성 실패(RED), 수정 후 대상 테스트 및 Backend 전체 테스트
- 리스크: 없음

## 2026-08-12 - AdMob SSV cryptographic 테스트 추가

- 변경 파일: `AdMobSsvVerifierTest.java`, `GoogleAdMobPublicKeyProviderTest.java`, `WORK_LOG.md`
- 내용: 정상 서명, 원문 변조, 중복 parameter, key cache fetch 증폭 방지를 검증한다.
- 검증: 대상 Backend 테스트 통과
- 리스크: 없음
