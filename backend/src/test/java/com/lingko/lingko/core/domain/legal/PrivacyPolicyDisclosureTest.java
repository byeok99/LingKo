package com.lingko.lingko.core.domain.legal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** 운영 리전과 로그 보관 계약이 사용자에게 제공되는 처리방침에서 누락되지 않도록 검증한다. */
class PrivacyPolicyDisclosureTest {

    @Test
    void 한국어_처리방침은_미국_운영_리전과_로그_보관기간을_공개한다() throws IOException {
        String policy = read("legal/privacy-policy.ko.md");

        assertThat(LegalConsentPolicy.CURRENT_DOCUMENT_VERSION).isEqualTo("2026-09-24");
        assertThat(policy)
                .contains("미국 동부(버지니아 북부, us-east-1)")
                .contains("14일");
    }

    @Test
    void 영어_처리방침은_미국_운영_리전과_로그_보관기간을_공개한다() throws IOException {
        String policy = read("legal/privacy-policy.en.md");

        assertThat(policy)
                .contains("US East (N. Virginia, us-east-1)")
                .contains("14 days");
    }

    private String read(String resourcePath) throws IOException {
        try (var input = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
