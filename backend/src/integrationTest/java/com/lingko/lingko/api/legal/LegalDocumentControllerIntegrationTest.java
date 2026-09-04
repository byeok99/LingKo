package com.lingko.lingko.api.legal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 약관·처리방침 공개 URL이 실제 HTTP 경로로 열리는지 검증한다.
 *
 * <p>보장 대상: 인증 없이 열릴 것(가입 전 사용자와 스토어 심사자가 읽어야 한다),
 * 언어 파라미터가 문서를 바꿀 것, 알 수 없는 경로가 404일 것, HTML로 응답할 것.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LegalDocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("이용약관은 Authorization 헤더 없이 HTML로 열린다")
    void 이용약관은_인증_없이_열린다() throws Exception {
        mockMvc.perform(get("/legal/terms"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("LingKo 이용약관")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("개인정보 처리방침도 같은 규칙으로 열린다")
    void 처리방침은_인증_없이_열린다() throws Exception {
        mockMvc.perform(get("/legal/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("개인정보 처리방침")));
    }

    @Test
    @DisplayName("lang=en이면 영문 문서를 반환한다")
    void 언어_파라미터로_영문을_고를_수_있다() throws Exception {
        mockMvc.perform(get("/legal/terms").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("LingKo Terms of Service")));
    }

    @Test
    @DisplayName("지원하지 않는 언어는 오류가 아니라 한국어로 되돌린다")
    void 지원하지_않는_언어는_기본_언어로_열린다() throws Exception {
        // 약관은 어떤 요청으로도 읽을 수 있어야 한다. 400을 주면 열람 자체가 막힌다.
        mockMvc.perform(get("/legal/terms").param("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("LingKo 이용약관")));
    }

    @Test
    @DisplayName("정의되지 않은 문서 경로는 404다")
    void 알_수_없는_문서는_404다() throws Exception {
        mockMvc.perform(get("/legal/cookie-policy"))
                .andExpect(status().isNotFound());
    }
}
