package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 완료된 평가 작업의 Idempotency 응답 보존 기간을 적용하고 제한된 batch로 레코드를 정리한다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationJobCleanupService {

    private static final List<EvaluationJob.Status> TERMINAL_STATUSES = List.of(
            EvaluationJob.Status.SUCCEEDED,
            EvaluationJob.Status.FAILED
    );

    private final EvaluationJobRepository jobRepository;
    private final EvaluationJobSettings settings;
    private final Clock clock;

    /**
     * 진행 중 작업과 quota 예약은 건드리지 않고 완료 시점이 보존 기간을 지난 작업만 삭제한다.
     *
     * @return 이번 batch에서 삭제한 작업 수
     */
    @Transactional
    public int cleanupExpired() {
        EvaluationJobSettings.Cleanup cleanup = settings.getCleanup();
        Instant cutoff = clock.instant().minus(Duration.ofDays(cleanup.getRetentionDays()));
        List<String> expiredJobIds = jobRepository.findExpiredTerminalJobIds(
                TERMINAL_STATUSES,
                cutoff,
                PageRequest.of(0, cleanup.getBatchSize())
        );
        if (expiredJobIds.isEmpty()) {
            return 0;
        }

        jobRepository.deleteAllByIdInBatch(expiredJobIds);
        return expiredJobIds.size();
    }
}
