package com.lingko.lingko.infra.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalMediaUrlValidatorTest {

    @Test
    @DisplayName("허용된 HTTPS host와 공인 IP는 통과한다")
    void validateAllowsKnownHttpsHost() throws Exception {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                host -> new InetAddress[]{uncheckedAddress("203.0.113.10")}
        );

        assertThatCode(() -> validator.validate("https://replicate.delivery/output/video.mp4"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HTTP URL은 거부한다")
    void validateRejectsHttpUrl() {
        ExternalMediaUrlValidator validator = validatorWithPublicAddress();

        assertThatThrownBy(() -> validator.validate("http://replicate.delivery/output/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS URL만 허용");
    }

    @Test
    @DisplayName("allowlist 밖 host는 거부한다")
    void validateRejectsUnknownHost() {
        ExternalMediaUrlValidator validator = validatorWithPublicAddress();

        assertThatThrownBy(() -> validator.validate("https://example.com/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않은 외부 미디어 host");
    }

    @Test
    @DisplayName("허용 host라도 private IP로 해석되면 거부한다")
    void validateRejectsPrivateResolvedAddress() throws Exception {
        ExternalMediaUrlValidator validator = new ExternalMediaUrlValidator(
                host -> new InetAddress[]{uncheckedAddress("127.0.0.1")}
        );

        assertThatThrownBy(() -> validator.validate("https://replicate.delivery/output/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내부망 외부 미디어 URL");
    }

    private ExternalMediaUrlValidator validatorWithPublicAddress() {
        return new ExternalMediaUrlValidator(host -> new InetAddress[]{uncheckedAddress("203.0.113.10")});
    }

    private static InetAddress uncheckedAddress(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
