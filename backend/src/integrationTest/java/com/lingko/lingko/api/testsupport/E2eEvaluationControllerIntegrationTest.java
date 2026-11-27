package com.lingko.lingko.api.testsupport;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E 연습 기록 시드가 앱이 실제로 읽는 조회 API에서 기대한 기록으로 보이는지 검증한다.
 * 시드 응답만 보면 저장 없이도 통과하므로, 앱과 같은 {@code GET /api/evaluations/me}·취약 음절 API로 다시 읽는다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles({"integration", "e2e"})
class E2eEvaluationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void givenEmptyHistory() throws Exception {
        String body = mockMvc.perform(post("/test-support/sessions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(body, "$.accessToken");
        mockMvc.perform(delete("/test-support/evaluations")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("오래된 것부터 넣은 기록이 앱 조회에서 최신순으로 보이고 점수가 그대로다")
    void seededRecordsAppearNewestFirst() throws Exception {
        seed("""
                {"records":[
                  {"text":"감사합니다","score":60,"accuracy":61,"fluency":62,"completeness":63,"wordScores":[60]},
                  {"text":"음악을 들어요","score":90,"accuracy":91,"fluency":92,"completeness":93,"wordScores":[85,95]}
                ]}""");

        mockMvc.perform(get("/api/evaluations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].originalText").value("음악을 들어요"))
                .andExpect(jsonPath("$.items[0].overallScore").value(90))
                .andExpect(jsonPath("$.items[0].scoreBreakdown.fluency").value(92))
                .andExpect(jsonPath("$.items[0].standardPronunciation").value("으마글 드러요"))
                .andExpect(jsonPath("$.items[0].words[0].text").value("으마글"))
                .andExpect(jsonPath("$.items[0].words[0].score").value(85))
                .andExpect(jsonPath("$.items[1].originalText").value("감사합니다"));
    }

    @Test
    @DisplayName("단어 점수에 null이 있으면 운영 저장 규칙대로 단어 점수 없이 '점수 없음'으로 보인다")
    void unreliableWordScoresAreUnavailable() throws Exception {
        seed("""
                {"records":[
                  {"text":"음악을 들어요","score":70,"accuracy":70,"fluency":70,"completeness":70,"wordScores":[80,null]}
                ]}""");

        mockMvc.perform(get("/api/evaluations/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].words[0].scoreStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.items[0].words[1].scoreStatus").value("UNAVAILABLE"));
    }

    @Test
    @DisplayName("표준 발음을 지정하면 현재 규칙 대신 그 snapshot이 저장된다")
    void storesGivenStandardPronunciationSnapshot() throws Exception {
        seed("""
                {"records":[
                  {"text":"음악을 들어요","standardPronunciation":"음악을 들어요","score":70,"accuracy":70,"fluency":70,"completeness":70,"wordScores":[70,70]}
                ]}""");

        mockMvc.perform(get("/api/evaluations/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].standardPronunciation").value("음악을 들어요"));
    }

    @Test
    @DisplayName("같은 음절을 2회 이상 낮은 점수로 넣으면 취약 음절 목록에 오른다")
    void seededWordScoresFeedWeakSounds() throws Exception {
        seed("""
                {"records":[
                  {"text":"밥","score":40,"accuracy":40,"fluency":40,"completeness":40,"wordScores":[40]},
                  {"text":"밥","score":40,"accuracy":40,"fluency":40,"completeness":40,"wordScores":[40]}
                ]}""");

        mockMvc.perform(get("/api/evaluations/me/weak-sounds").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].text").value("밥"));
    }

    @Test
    @DisplayName("기록을 비우면 앱 조회가 0건이다")
    void clearRemovesHistory() throws Exception {
        seed("""
                {"records":[{"text":"감사합니다","score":60,"accuracy":60,"fluency":60,"completeness":60,"wordScores":[60]}]}""");

        mockMvc.perform(delete("/test-support/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(1));
        mockMvc.perform(get("/api/evaluations/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("어절 수와 wordScores 개수가 다르면 400이고 기록을 만들지 않는다")
    void rejectsWordScoreCountMismatch() throws Exception {
        mockMvc.perform(post("/test-support/evaluations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"records":[{"text":"음악을 들어요","score":60,"accuracy":60,"fluency":60,"completeness":60,"wordScores":[60]}]}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/evaluations/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("점수가 0~100을 벗어나면 400이다")
    void rejectsOutOfRangeScore() throws Exception {
        mockMvc.perform(post("/test-support/evaluations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"records":[{"text":"감사합니다","score":101,"accuracy":60,"fluency":60,"completeness":60,"wordScores":[60]}]}"""))
                .andExpect(status().isBadRequest());
    }

    private void seed(String json) throws Exception {
        mockMvc.perform(post("/test-support/evaluations").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }
}
