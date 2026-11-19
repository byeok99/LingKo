package com.lingko.lingko.api.testsupport;

import com.jayway.jsonpath.JsonPath;
import com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService;
import com.lingko.lingko.core.domain.legal.service.LegalConsentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/** e2e 프로필에서 테스트 세션이 실제 로그인과 같은 형식으로 발급되는지 검증한다. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles({"integration", "e2e"})   // integration을 빼면 기존 통합 테스트 설정이 사라진다
class E2eSessionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private LegalConsentService legalConsentService;
    @Autowired private AiProcessingConsentService aiProcessingConsentService;

    @Test
    @DisplayName("기본 호출은 같은 테스트 사용자에게 매번 새 세션을 주고 동의까지 기록한다")
    void issuesNewSessionForSameUserWithConsents() throws Exception {
        String first = issue("/test-support/sessions");
        String second = issue("/test-support/sessions");

        long firstUserId = userId(first);
        assertThat(userId(second)).isEqualTo(firstUserId);
        assertThat(token(second)).isNotEqualTo(token(first));

        assertThat(legalConsentService.getStatus(firstUserId).required()).isFalse();
        assertThat(aiProcessingConsentService.getStatus(firstUserId).granted()).isTrue();
    }

    @Test
    @DisplayName("consented=false는 동의 기록 없는 새 사용자를 매번 만든다")
    void createsFreshUserWithoutConsents() throws Exception {
        long first = userId(issue("/test-support/sessions?consented=false"));
        long second = userId(issue("/test-support/sessions?consented=false"));

        assertThat(second).isNotEqualTo(first);
        assertThat(legalConsentService.getStatus(first).required()).isTrue();
    }

    private String issue(String path) throws Exception {
        return mockMvc.perform(post(path))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
    }

    private long userId(String body) {
        Number id = JsonPath.read(body, "$.user.userId");
        return id.longValue();
    }

    private String token(String body) {
        return JsonPath.read(body, "$.accessToken");
    }
}