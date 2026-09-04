package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Apple identity token의 허용 audience인 iOS App ID를 설정에서 주입한다.
 */
@Configuration
@ConfigurationProperties(prefix = "apple")
@Getter
@Setter
public class AppleOAuthSettings {
    /** Apple token의 {@code aud} claim과 일치해야 하는 공개 iOS bundle identifier다. */
    private String clientId;
}
