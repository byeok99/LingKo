package com.lingko.lingko.api.testsupport;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E 사전조건용 평가 기회 설정이 앱이 실제로 조회하는 quota와 같은 값을 만드는지 검증한다.
 * 설정 응답만 보면 저장 없이 응답만 맞춰도 통과하므로, 앱과 같은 {@code GET /api/quota/today}로 다시 읽는다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles({"integration", "e2e"})
class E2eQuotaControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("기회를 3회로 맞추면 앱 조회에도 3/5회와 다음 충전 시각이 보인다")
    void setsRemainingPracticesSeenByApp() throws Exception {
        String token = issueSessionToken();

        mockMvc.perform(put("/test-support/quota").param("remaining", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPractices").value(3))
                .andExpect(jsonPath("$.freeLimit").value(5))
                .andExpect(jsonPath("$.rewardedAvailable").value(0))
                .andExpect(jsonPath("$.nextRefillAt").isNotEmpty());

        mockMvc.perform(get("/api/quota/today").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPractices").value(3))
                .andExpect(jsonPath("$.nextRefillAt").isNotEmpty());
    }

    @Test
    @DisplayName("기회를 5회로 맞추면 충전 대기가 없어 nextRefillAt이 null이다")
    void fullQuotaHasNoRefillTimer() throws Exception {
        issueSessionToken();

        mockMvc.perform(put("/test-support/quota").param("remaining", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPractices").value(5))
                .andExpect(jsonPath("$.nextRefillAt").doesNotExist());
    }

    @Test
    @DisplayName("0~5 밖의 값은 400으로 거부한다")
    void rejectsOutOfRangeValue() throws Exception {
        issueSessionToken();

        mockMvc.perform(put("/test-support/quota").param("remaining", "6"))
                .andExpect(status().isBadRequest());
    }

    private String issueSessionToken() throws Exception {
        String body = mockMvc.perform(post("/test-support/sessions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
