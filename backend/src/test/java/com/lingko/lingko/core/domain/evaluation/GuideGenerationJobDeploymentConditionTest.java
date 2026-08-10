package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.GuideGenerationJobAccessGuard;
import com.lingko.lingko.api.evaluation.GuideGenerationJobController;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 비용을 발생시키는 guide-jobs HTTP surface가 명시적 설정 없이는 등록되지 않는지 검증한다.
 */
class GuideGenerationJobDeploymentConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("기본 운영 설정은 guide-jobs Controller를 등록하지 않는다")
    void disablesGuideJobsApiByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(GuideGenerationJobController.class)
        );
    }

    @Test
    @DisplayName("명시적으로 활성화한 환경만 guide-jobs Controller를 등록한다")
    void enablesGuideJobsApiExplicitly() {
        contextRunner
                .withPropertyValues("guide-generation.jobs.api-enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(GuideGenerationJobController.class)
                );
    }

    @Configuration(proxyBeanMethods = false)
    @Import(GuideGenerationJobController.class)
    static class TestConfiguration {

        @Bean
        GuideGenerationJobService guideGenerationJobService() {
            return mock(GuideGenerationJobService.class);
        }

        @Bean
        GuideGenerationJobAccessGuard guideGenerationJobAccessGuard() {
            return mock(GuideGenerationJobAccessGuard.class);
        }
    }
}
