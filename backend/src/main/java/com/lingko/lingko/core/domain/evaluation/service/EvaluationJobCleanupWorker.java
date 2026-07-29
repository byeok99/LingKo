package com.lingko.lingko.core.domain.evaluation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 완료된 평가 작업을 주기적으로 정리해 Idempotency 저장소가 무제한 증가하지 않게 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "evaluation.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class EvaluationJobCleanupWorker {

    private final EvaluationJobCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${evaluation.cleanup.interval-ms:3600000}")
    public void cleanupExpired() {
        int deleted = cleanupService.cleanupExpired();
        if (deleted > 0) {
            log.info("Expired evaluation jobs cleaned: count={}", deleted);
        }
    }
}
