package com.lingko.lingko.core.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** App Review 전용 로그인 경계를 기본 비활성화하고 서버 Secret과 시도 한도로 제한한다. */
@Configuration
@ConfigurationProperties(prefix = "review-access")
@Validated
@Getter
@Setter
public class ReviewAccessSettings {

    private boolean enabled = false;

    /** 원문 코드는 앱·저장소에 두지 않고 운영 Secret에 SHA-256 hex 형태로만 주입한다. */
    private String codeSha256 = "";

    /** null은 심사용 계정이 아직 운영 DB에 준비되지 않았다는 뜻이다. */
    private Long userId;

    @Min(1)
    @Max(20)
    private int maxAttempts = 5;

    @Min(60)
    @Max(3_600)
    private int windowSeconds = 300;

    /** 기능을 열 때만 64자리 hash와 실제 사용자 ID를 강제해 잘못 열린 인증 우회를 막는다. */
    @AssertTrue(message = "review access requires a SHA-256 code hash and positive user ID")
    public boolean isSecureWhenEnabled() {
        return !enabled || (codeSha256 != null
                && codeSha256.matches("^[0-9a-fA-F]{64}$")
                && userId != null
                && userId > 0);
    }
}
