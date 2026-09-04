package com.lingko.lingko.core.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 외부 비용을 발생시키는 guide-jobs HTTP surface의 공개 여부와 admission 한도를 관리한다.
 */
@Configuration
@ConfigurationProperties(prefix = "guide-generation.jobs")
@Validated
@Getter
@Setter
public class GuideGenerationJobSettings {

    private boolean apiEnabled = false;

    /** 실제 값은 환경 Secret에서만 주입하며 저장소에는 빈 기본값만 둔다. */
    private String internalToken = "";

    @Min(1)
    @Max(60)
    private int requestsPerMinute = 2;

    @Min(1)
    @Max(4)
    private int maxConcurrent = 1;

    /** API를 명시적으로 열 때 추측하기 어려운 내부 token 없이는 시작하지 않는다. */
    @AssertTrue(message = "guide-jobs API requires an internal token of at least 32 characters")
    public boolean isSecureWhenEnabled() {
        return !apiEnabled || (internalToken != null && internalToken.length() >= 32);
    }
}
