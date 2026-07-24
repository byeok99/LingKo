package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.PronunciationPrepareResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evaluation 컨트롤러 Prepare Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@WebMvcTest(EvaluationController.class)
class EvaluationControllerPrepareTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @Test
    @DisplayName("POST /api/pronunciation/prepare는 준비된 문장을 반환한다")
    void prepareCustomSentence() throws Exception {
        PronunciationPrepareResponse response = PronunciationPrepareResponse.builder()
                .sentence(PronunciationPrepareResponse.SentenceResponse.builder()
                        .source("CUSTOM")
                        .originalText("한국어를 배우고 있어요.")
                        .standardPronunciation("한구거를 배우고 이써요.")
                        .translation("Practice with your own sentence.")
                        .categoryLabel("Free practice")
                        .learningPoint("Linking across syllables")
                        .initialScore(0)
                        .characters(List.of())
                        .build())
                .build();
        when(evaluationService.prepareCustomSentence("한국어를 배우고 있어요.")).thenReturn(response);

        mockMvc.perform(post("/api/pronunciation/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CUSTOM\",\"text\":\"한국어를 배우고 있어요.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practiceToken").doesNotExist())
                .andExpect(jsonPath("$.sentence.source").value("CUSTOM"))
                .andExpect(jsonPath("$.sentence.standardPronunciation").value("한구거를 배우고 이써요."));
    }

    @Test
    @DisplayName("CUSTOM prepare text가 비어 있으면 기존 validation 에러 포맷을 반환한다")
    void prepareCustomSentenceValidatesBlankText() throws Exception {
        mockMvc.perform(post("/api/pronunciation/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CUSTOM\",\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("customTextValid"));
    }

    @Test
    @DisplayName("Phase 3 prepare는 RECOMMENDED 요청을 지원하지 않고 400을 반환한다")
    void prepareRecommendedSentenceIsNotSupportedInPhase3() throws Exception {
        mockMvc.perform(post("/api/pronunciation/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"RECOMMENDED\",\"sentenceId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Only CUSTOM prepare requests are supported"));
    }

    @Test
    @DisplayName("Phase 3 CUSTOM prepare text가 100자를 넘으면 validation 400을 반환한다")
    void prepareCustomSentenceValidatesLongTextAsBadRequest() throws Exception {
        String longText = "가".repeat(101);

        mockMvc.perform(post("/api/pronunciation/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CUSTOM\",\"text\":\"" + longText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("customTextLengthValid"));
    }

    @Test
    @DisplayName("convert text가 30자를 넘으면 공통 validation 에러를 반환한다")
    void convertValidatesLongTextAsBadRequest() throws Exception {
        String longText = "가".repeat(31);

        mockMvc.perform(post("/api/pronunciation/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + longText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("text"));
    }

    @Test
    @DisplayName("malformed JSON은 공통 invalid request 에러를 반환한다")
    void malformedJsonReturnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/pronunciation/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CUSTOM\",\"text\":\"맛있겠다\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details.length()").value(0));
    }
}
