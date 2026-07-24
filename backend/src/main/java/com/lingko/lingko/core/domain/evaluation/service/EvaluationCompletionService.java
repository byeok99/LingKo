package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 평가 결과 저장과 쿼터 예약 확정을 하나의 DB 트랜잭션으로 완료한다.
 *
 * 둘 중 하나만 반영되는 상태를 막기 위해 외부 평가가 끝난 뒤의 DB 변경만 이 경계에서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationCompletionService {

    private final EvaluationPersistenceService persistenceService;
    private final PracticeQuotaService quotaService;

    @Transactional
    public void complete(
            EvaluationPersistenceService.SaveEvaluationResultCommand command,
            PracticeQuotaService.PracticeQuotaReservation reservation
    ) {
        persistenceService.saveResult(command);
        quotaService.confirmPractice(reservation);
    }
}
