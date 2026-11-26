package com.lingko.lingko.api.evaluation;

import com.jayway.jsonpath.JsonPath;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 자유 문장 100자 상한이 평가 작업 생성 경계에서도 지켜지는지 검증한다.
 * 앱은 100자를 넘으면 요청을 보내지 않고 안내를 보이지만(Notion TC-018), 서버 검증이 최종 경계라 따로 보호한다.
 *
 * <p>준비 API 경계는 {@code EvaluationControllerPrepareTest}가 맡는다. 여기서는 앱을 거치지 않고 101자를
 * 평가 작업 요청으로 직접 보냈을 때 거부되고, 그 거부가 작업 생성이나 평가 기회 차감으로 이어지지 않는지 본다.
 * 세션·평가 기회 사전조건은 e2e 프로필의 test-support API로 맞춘다.</p>
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles({"integration", "e2e"})
class EvaluationJobTextLimitIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EvaluationJobRepository jobRepository;

    @Test
    @DisplayName("101자 자유 문장 평가 요청은 400으로 거부되고 작업 생성·기회 차감이 없다")
    void rejectsTextOver100CharactersWithoutSideEffects() throws Exception {
        String token = issueSessionToken();
        mockMvc.perform(put("/test-support/quota").param("remaining", "3"))
                .andExpect(status().isOk());
        long jobsBefore = jobRepository.count();

        // 요청 본문 검증이 먼저 실패해야 하므로 objectKey는 형식만 맞춘다(업로드·소유권 확인까지 가지 않는다)
        mockMvc.perform(post("/api/evaluations/jobs")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "tc019-text-limit-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"evaluations/tc019.wav\",\"text\":\"" + "가".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(jobRepository.count()).as("거부된 요청은 평가 작업을 만들지 않는다").isEqualTo(jobsBefore);
        mockMvc.perform(get("/api/quota/today").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPractices").value(3));
    }

    private String issueSessionToken() throws Exception {
        String body = mockMvc.perform(post("/test-support/sessions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
