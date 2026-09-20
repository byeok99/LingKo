package com.lingko.lingko.api.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AdMob 크롤러가 공개 루트에서 앱의 승인된 판매자 정보와 크롤링 허용 정책을 읽을 수 있는지 검증한다.
 *
 * <p>보장 대상: 인증 없는 HTTP 200, {@code text/plain} 호환 Content-Type,
 * AdMob 계정과 일치하는 publisher 레코드 및 {@code Google-adstxt} 허용 정책의 정확한 본문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppAdsStaticResourceIntegrationTest {

    private static final String EXPECTED_SELLER_RECORD =
            "google.com, pub-5081228614816629, DIRECT, f08c47fec0942fa0\n";
    private static final String EXPECTED_ROBOTS_POLICY =
            "User-agent: Google-adstxt\nDisallow:\n";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("app-ads.txt는 인증 없이 plain text 판매자 레코드를 반환한다")
    void appAds는_공개_plainText로_열린다() throws Exception {
        mockMvc.perform(get("/app-ads.txt").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(EXPECTED_SELLER_RECORD));
    }

    @Test
    @DisplayName("robots.txt는 AdMob 크롤러의 접근을 허용한다")
    void robots는_AdMob_크롤러를_허용한다() throws Exception {
        mockMvc.perform(get("/robots.txt").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(EXPECTED_ROBOTS_POLICY));
    }
}
