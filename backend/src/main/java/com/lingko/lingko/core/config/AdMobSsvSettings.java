package com.lingko.lingko.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** AdMob SSV가 신뢰할 광고 단위와 보상 정책을 서버 환경 설정으로 제한한다. */
@Configuration
@ConfigurationProperties(prefix = "admob.ssv")
@Validated
@Getter
@Setter
public class AdMobSsvSettings {

    private boolean enabled = true;
    private String allowedAdUnitIds = "";
    private String rewardItem = "pronunciation_chance";

    @Min(1)
    private int rewardAmount = 1;

    @Min(1)
    @Max(60)
    private int sessionExpiryMinutes = 15;

    public Set<String> allowedAdUnitIdSet() {
        return Arrays.stream(allowedAdUnitIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
