package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueue;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueueDispatcher;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueueWorker;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 같은 image가 설정만으로 API dispatcher와 독립 Worker 역할을 배타적으로 구성하는지 검증한다.
 */
class EvaluationWorkerModeConditionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("SQS API 모드는 dispatcher만 실행하고 평가 Worker는 실행하지 않는다")
    void configuresApiDispatcherWithoutWorker() {
        contextRunner
                .withPropertyValues(
                        "evaluation.worker.mode=sqs",
                        "evaluation.worker.enabled=false",
                        "evaluation.queue.dispatcher-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EvaluationJobQueueDispatcher.class);
                    assertThat(context).doesNotHaveBean(EvaluationJobQueueWorker.class);
                    assertThat(context).doesNotHaveBean(EvaluationJobWorker.class);
                });
    }

    @Test
    @DisplayName("SQS Worker 모드는 Queue Worker만 실행하고 dispatcher는 실행하지 않는다")
    void configuresIndependentQueueWorker() {
        contextRunner
                .withPropertyValues(
                        "evaluation.worker.mode=sqs",
                        "evaluation.worker.enabled=true",
                        "evaluation.queue.dispatcher-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EvaluationJobQueueWorker.class);
                    assertThat(context).doesNotHaveBean(EvaluationJobQueueDispatcher.class);
                    assertThat(context).doesNotHaveBean(EvaluationJobWorker.class);
                });
    }

    @Test
    @DisplayName("database fallback 모드는 DB polling Worker만 실행한다")
    void configuresDatabaseFallbackWorker() {
        contextRunner
                .withPropertyValues(
                        "evaluation.worker.mode=database",
                        "evaluation.worker.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EvaluationJobWorker.class);
                    assertThat(context).doesNotHaveBean(EvaluationJobQueueWorker.class);
                    assertThat(context).doesNotHaveBean(EvaluationJobQueueDispatcher.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EvaluationJobSettings.class)
    @Import({
            EvaluationJobWorker.class,
            EvaluationJobQueueDispatcher.class,
            EvaluationJobQueueWorker.class
    })
    static class TestConfiguration {

        @Bean
        EvaluationJobRepository jobRepository() {
            return mock(EvaluationJobRepository.class);
        }

        @Bean
        EvaluationJobQueue evaluationJobQueue() {
            return mock(EvaluationJobQueue.class);
        }

        @Bean
        EvaluationJobProcessingService processingService() {
            return mock(EvaluationJobProcessingService.class);
        }

        @Bean
        EvaluationJobExecutor executor() {
            return mock(EvaluationJobExecutor.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
