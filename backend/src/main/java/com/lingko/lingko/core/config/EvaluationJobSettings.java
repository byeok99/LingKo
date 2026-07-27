package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 평가 업로드 서명과 DB Worker의 lease·재시도 한계를 환경 설정으로 관리한다.
 */
@Configuration
@ConfigurationProperties(prefix = "evaluation")
@Getter
@Setter
public class EvaluationJobSettings {

    private int uploadUrlExpireMinutes = 10;
    private Worker worker = new Worker();

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = true;
        private long pollDelayMs = 1000;
        private int leaseSeconds = 60;
        private int retryDelaySeconds = 5;
        private int maxAttempts = 3;
    }
}
