package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.config.FfmpegSettings;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.config.S3Config;
import com.lingko.lingko.core.config.WebClientConfig;
import com.lingko.lingko.core.domain.evaluation.service.GuideMediaPrewarmOptions;
import com.lingko.lingko.core.domain.evaluation.service.GuideMediaPrewarmPlanner;
import com.lingko.lingko.core.domain.evaluation.service.GuideMediaPrewarmReport;
import com.lingko.lingko.core.domain.evaluation.service.GuideMediaPrewarmService;
import com.lingko.lingko.core.domain.evaluation.service.VideoGenerator;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import com.lingko.lingko.infra.storage.ExternalMediaUrlValidator;
import com.lingko.lingko.infra.storage.S3Uploader;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * DB·HTTP 서버·평가 Worker 없이 가이드 S3 cache만 사전 생성하는 운영 entry point다.
 *
 * 기본 실행은 dry-run이며 실제 외부 호출에는 {@code --prewarm-dry-run=false}가 필요하다.
 */
public final class GuideMediaPrewarmApplication {

    private GuideMediaPrewarmApplication() {
    }

    /** dry-run 또는 profile 격리된 실제 사전 생성을 실행한다. */
    public static void main(String[] args) {
        GuideMediaPrewarmOptions options = GuideMediaPrewarmOptions.fromArgs(args);
        if (options.dryRun()) {
            GuideMediaPrewarmReport report = dryRun(options);
            System.out.printf(
                    "guide_prewarm planned=%d planned_segments=%d selected=%d selected_segments=%d dry_run=true%n",
                    report.planned(),
                    report.plannedSegments(),
                    report.selected(),
                    report.selectedSegments()
            );
            return;
        }
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PrewarmConfiguration.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .profiles("guide-prewarm")
                .run(args)) {
            GuideMediaPrewarmReport report = context.getBean(GuideMediaPrewarmService.class).prewarm(options);
            System.out.printf(
                    "guide_prewarm planned=%d planned_segments=%d selected=%d selected_segments=%d "
                            + "succeeded=%d failed=%d dry_run=false%n",
                    report.planned(),
                    report.plannedSegments(),
                    report.selected(),
                    report.selectedSegments(),
                    report.succeeded(),
                    report.failed()
            );
            if (report.failed() > 0) {
                throw new IllegalStateException("Guide prewarm completed with failures: " + report.failed());
            }
        }
    }

    private static GuideMediaPrewarmReport dryRun(GuideMediaPrewarmOptions options) {
        SyllableMappingUtil mappingUtil = new SyllableMappingUtil(null);
        mappingUtil.loadMapping();
        GuideMediaPrewarmPlanner planner = new GuideMediaPrewarmPlanner(mappingUtil);
        // dry-run 서비스는 generator를 호출하지 않는 계약이므로 외부 client graph를 만들 필요가 없다.
        VideoGenerator forbiddenGenerator = (pairs, syllable, type) -> {
            throw new IllegalStateException("dry-run must not call the external video generator");
        };
        return new GuideMediaPrewarmService(planner, forbiddenGenerator).prewarm(options);
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("guide-prewarm")
    @EnableConfigurationProperties({AwsSettings.class, ReplicateSettings.class, FfmpegSettings.class})
    @Import({
            S3Config.class,
            WebClientConfig.class,
            SyllableMappingUtil.class,
            ExternalMediaUrlValidator.class,
            S3Uploader.class,
            ReplicateApiClient.class,
            VideoMerger.class,
            VideoPlaybackNormalizer.class,
            FrameInterpolationVideoGenerator.class,
            GuideMediaPrewarmPlanner.class,
            GuideMediaPrewarmService.class
    })
    static class PrewarmConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
