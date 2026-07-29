package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 같은 image가 설정만으로 API와 독립 DB Worker 역할을 배타적으로 구성하는지 검증한다.
 */
class EvaluationWorkerDeploymentConditionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("API 배포는 설정으로 DB Worker를 실행하지 않는다")
    void disablesWorkerInApiDeployment() {
        contextRunner
                .withPropertyValues("evaluation.worker.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(EvaluationJobWorker.class)
                );
    }

    @Test
    @DisplayName("독립 Worker 배포는 DB polling Worker 한 개를 실행한다")
    void enablesWorkerInIndependentDeployment() {
        contextRunner
                .withPropertyValues("evaluation.worker.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(EvaluationJobWorker.class)
                );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EvaluationJobSettings.class)
    @Import(EvaluationJobWorker.class)
    static class TestConfiguration {

        @Bean
        EvaluationJobProcessingService processingService() {
            return mock(EvaluationJobProcessingService.class);
        }

        @Bean
        EvaluationJobExecutor executor() {
            return mock(EvaluationJobExecutor.class);
        }
    }
}
