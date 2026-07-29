package com.lingko.lingko.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 평가 업로드, DB Worker와 완료 작업 보존 정책을 환경 설정으로 관리한다.
 */
@Configuration
@ConfigurationProperties(prefix = "evaluation")
@Validated
@Getter
@Setter
public class EvaluationJobSettings {

    private int uploadUrlExpireMinutes = 10;

    @Valid
    private Worker worker = new Worker();

    @Valid
    private Queue queue = new Queue();

    @Valid
    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = true;

        private WorkerMode mode = WorkerMode.DATABASE;

        @Min(100)
        private long pollDelayMs = 1000;

        @Min(1)
        @Max(43_200)
        private int leaseSeconds = 60;

        @Min(0)
        @Max(43_200)
        private int retryDelaySeconds = 5;

        @Min(1)
        @Max(100)
        private int maxAttempts = 3;
    }

    /**
     * SQS 전달과 DB 기반 재발행 복구의 polling·visibility 경계를 관리한다.
     */
    @Getter
    @Setter
    public static class Queue {
        private boolean dispatcherEnabled = true;

        private String url;

        private String endpoint;

        @Min(100)
        private long dispatchDelayMs = 500;

        @Min(1)
        @Max(10_000)
        private int dispatchBatchSize = 100;

        @Min(1)
        private int redispatchSeconds = 120;

        @Min(0)
        @Max(20)
        private int receiveWaitSeconds = 10;

        @Min(1)
        @Max(43_200)
        private int visibilityTimeoutSeconds = 120;
    }

    /**
     * 완료된 작업 응답을 재사용할 기간과 한 번에 삭제할 최대 행 수를 제한한다.
     */
    @Getter
    @Setter
    public static class Cleanup {
        private boolean enabled = true;

        @Min(1)
        private int retentionDays = 7;

        @Min(60_000)
        private long intervalMs = 3_600_000;

        @Min(1)
        @Max(10_000)
        private int batchSize = 1_000;
    }

    public enum WorkerMode {
        DATABASE,
        SQS
    }
}
