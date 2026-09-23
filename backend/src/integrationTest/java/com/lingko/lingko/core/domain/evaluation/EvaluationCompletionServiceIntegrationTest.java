package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationCompletionService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationPersistenceService;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 현재 비동기 Worker가 사용하는 결과 저장·quota 확정 transaction의 원자성을 검증한다.
 */
@SpringBootTest
class EvaluationCompletionServiceIntegrationTest {

    @Autowired
    private EvaluationCompletionService completionService;
    @Autowired
    private EvaluationLogRepository evaluationLogRepository;
    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        evaluationLogRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("quota 확정 실패 시 같은 transaction의 평가 결과 저장도 rollback한다")
    void rollsBackResultWhenQuotaConfirmationFails() {
        User user = userRepository.save(User.builder()
                .socialId("confirmation-failure-user")
                .socialType(User.SocialType.GOOGLE)
                .build());
        PracticeQuotaService.PracticeQuotaReservation missingReservation =
                new PracticeQuotaService.PracticeQuotaReservation(
                        user.getUserIdx(),
                        LocalDate.now(PracticeQuotaService.SERVICE_ZONE),
                        PracticeQuotaService.QuotaSource.FREE
                );
        EvaluationPersistenceService.SaveEvaluationResultCommand command =
                EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                        .user(user)
                        .source(EvaluationLog.PracticeSource.CUSTOM)
                        .originalText("안녕하세요.")
                        .standardPronunciation("안녕하세요.")
                        .result(PracticeResultResponse.builder()
                                .overallScore(91)
                                .characters(List.of())
                                .weakCharacters(List.of())
                                .build())
                        .build();

        assertThatThrownBy(() -> completionService.complete(command, missingReservation))
                .isInstanceOf(IllegalStateException.class);

        assertThat(evaluationLogRepository.count()).isZero();
    }
}
