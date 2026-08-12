# 작업 이력

## 2026-08-12 - AdMob 공개키 provider 생성자 주입 수정

- 변경 파일: `GoogleAdMobPublicKeyProvider.java`, `WORK_LOG.md`
- 내용: 테스트용 보조 생성자와 운영 생성자가 공존해도 Spring이 운영 의존성 생성자를 명확히 선택하도록 `@Autowired`를 지정했다.
- 검증: Spring 컨테이너 bean 생성 회귀 테스트 및 Backend 전체 테스트
- 리스크: 없음

## 2026-08-12 - Google AdMob SSV 검증 adapter 추가

- 변경 파일: `AdMobPublicKeyProvider.java`, `GoogleAdMobPublicKeyProvider.java`, `AdMobSsvVerifier.java`, `WORK_LOG.md`
- 내용: raw query ECDSA-SHA256 검증과 23시간 rotating key cache를 구현했다.
- 검증: 서명 변조·parameter 중복·cache miss 테스트 통과
- 리스크: Google key server 장애 시 callback은 503으로 재시도됨
