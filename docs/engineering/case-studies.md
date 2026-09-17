# 기술적 문제 해결 사례

구현에서 마주친 문제와 선택한 해결 방법을 정리합니다. 운영 로그나 개인 체크리스트 대신 코드·테스트로 확인할 수 있는 설계에 초점을 맞춥니다.

## 동시 요청의 평가 기회 경쟁

잔여량을 읽고 객체 값을 바꾸는 방식은 두 요청이 마지막 기회를 동시에 사용하게 만들 수 있습니다. 예약·확정·복구는 조건부 UPDATE의 영향 행 수로 판정하고, 최초 생성과 시간 충전에는 짧은 행 잠금을 사용했습니다. 외부 음성 평가를 기다리는 동안 잠금을 유지하지 않는 것이 핵심입니다.

검증 근거: [PracticeQuotaConcurrencyTest](../../backend/src/test/java/com/lingko/lingko/core/domain/quota/PracticeQuotaConcurrencyTest.java). 테스트 DB에서의 정합성 검증과 운영 MySQL 성능은 구분합니다.

[설계 결정](../architecture/adr/0006-atomic-practice-quota-transitions.md)

## 재시도로 중복 등록되는 평가

모바일에서 응답을 받지 못했다고 서버 작업까지 실패한 것은 아닙니다. 사용자별 Idempotency 키로 기존 요청을 확인하고, 작업 생성과 쿼터 예약을 같은 트랜잭션에 묶었습니다. 같은 키의 다른 입력은 충돌로 거부합니다.

검증 근거: [EvaluationJobIdempotencyIntegrationTest](../../backend/src/integrationTest/java/com/lingko/lingko/core/domain/evaluation/EvaluationJobIdempotencyIntegrationTest.java). DB 등록의 멱등성은 외부 평가 호출의 exactly-once 보장과 다릅니다.

## native UPDATE 이후 작업 상태 저장 누락

쿼터의 native UPDATE가 영속성 컨텍스트를 비우는 경계에서는 관리 중이던 작업 엔티티의 변경 감지가 유지된다고 가정할 수 없습니다. 완료 흐름에서 결과 저장과 작업 성공 상태를 먼저 반영한 뒤 쿼터를 확정하도록 순서를 맞췄습니다.

검증 근거: [EvaluationJobProcessingServiceTest](../../backend/src/test/java/com/lingko/lingko/core/domain/evaluation/EvaluationJobProcessingServiceTest.java)와 작업 처리 통합 테스트. 트랜잭션 유무뿐 아니라 flush·clear 시점도 정합성의 일부입니다.

## 가이드 생성 비용과 실패 격리

반복되는 음절을 매번 생성하지 않도록 현재 조음 매핑 버전과 일치하는 MP4와 결정적 S3 키를 우선 조회합니다. 매핑이나 자산 의미가 바뀌면 버전을 올려 과거 영상을 자동으로 무효화하며, 생성 실패는 이미지 가이드로 대체해 발음 평가 결과 자체를 잃지 않도록 분리했습니다.

검증 근거: [GuideMediaResolverTest](../../backend/src/test/java/com/lingko/lingko/core/domain/evaluation/service/GuideMediaResolverTest.java). 비용 절감률이나 캐시 적중률은 실측 없이 수치로 주장하지 않습니다.

[전체 평가 흐름](../architecture/evaluation-flow.md)
