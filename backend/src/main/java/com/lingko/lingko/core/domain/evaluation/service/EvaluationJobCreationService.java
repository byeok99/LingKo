package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobConflictException;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * 사용자별 Idempotency 판정, 쿼터 예약과 작업 생성을 하나의 DB transaction으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationJobCreationService {

    private final EvaluationJobRepository jobRepository;
    private final UserRepository userRepository;
    private final PracticeQuotaService quotaService;
    private final Clock clock;
    private final com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService aiConsentService;

    @Transactional
    public EvaluationJob create(
            Long userId,
            String idempotencyKey,
            String requestHash,
            String objectKey,
            EvaluationJobService.EvaluationTarget target
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
        // 철회와 같은 사용자 lock 아래에서 동의를 확정한 뒤에만 기회를 예약한다.
        Long consentId = aiConsentService.requireGranted(userId);
        EvaluationJob existing = jobRepository
                .findByUserUserIdxAndIdempotencyKey(userId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!existing.hasSameRequestHash(requestHash)) {
                throw new EvaluationJobConflictException();
            }
            return existing;
        }

        PracticeQuotaService.PracticeQuotaReservation reservation =
                quotaService.reservePractice(userId);
        EvaluationJob job = EvaluationJob.create(
                UUID.randomUUID().toString(),
                user,
                idempotencyKey,
                requestHash,
                objectKey,
                target.source(),
                target.sentenceId(),
                target.originalText(),
                target.standardPronunciation(),
                reservation.quotaDate(),
                reservation.source(),
                clock.instant()
        );
        job.recordAiConsent(consentId);
        return jobRepository.save(job);
    }
}
