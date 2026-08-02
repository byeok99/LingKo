package com.lingko.lingko.api.sentence;

import com.lingko.lingko.api.sentence.dto.PracticeSentenceResponse;
import com.lingko.lingko.api.sentence.dto.RecommendedSentencesResponse;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.service.SentenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sentence 컨트롤러 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@WebMvcTest(SentenceController.class)
class SentenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SentenceService sentenceService;

    @Test
    @DisplayName("추천 문장 목록을 반환한다")
    void getRecommendedSentences() throws Exception {
        PracticeSentenceResponse sentence = PracticeSentenceResponse.builder()
                .sentenceId(1L)
                .source("RECOMMENDED")
                .originalText("맛있겠다")
                .standardPronunciation("마싣껟따")
                .translation("It looks delicious.")
                .categoryLabel("Food")
                .learningPoint("Final consonant linking and tense sound")
                .initialScore(0)
                .characters(List.of())
                .build();
        when(sentenceService.findRecommendedSentences(20, "FOOD"))
                .thenReturn(new RecommendedSentencesResponse(List.of(sentence)));

        mockMvc.perform(get("/api/sentences/recommended")
                        .param("limit", "20")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sentenceId").value(1L))
                .andExpect(jsonPath("$.items[0].source").value("RECOMMENDED"))
                .andExpect(jsonPath("$.items[0].originalText").value("맛있겠다"))
                .andExpect(jsonPath("$.items[0].standardPronunciation").value("마싣껟따"));
    }

    @Test
    @DisplayName("추천 문장 limit은 50 이하로 제한한다")
    void limitMustBeAtMost50() throws Exception {
        mockMvc.perform(get("/api/sentences/recommended")
                        .param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("추천 문장 단건을 반환한다")
    void getSentence() throws Exception {
        PracticeSentenceResponse sentence = PracticeSentenceResponse.builder()
                .sentenceId(1L)
                .source("RECOMMENDED")
                .originalText("맛있겠다")
                .standardPronunciation("마싣껟따")
                .translation("It looks delicious.")
                .categoryLabel("Food")
                .learningPoint("Final consonant linking and tense sound")
                .initialScore(0)
                .characters(List.of())
                .build();
        when(sentenceService.getSentence(1L)).thenReturn(sentence);

        mockMvc.perform(get("/api/sentences/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentenceId").value(1L))
                .andExpect(jsonPath("$.source").value("RECOMMENDED"));
    }

    @Test
    @DisplayName("active 추천 문장이 없으면 404를 반환한다")
    void getSentenceReturnsNotFound() throws Exception {
        when(sentenceService.getSentence(404L)).thenThrow(new SentenceNotFoundException(404L));

        mockMvc.perform(get("/api/sentences/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SENTENCE_NOT_FOUND"));
    }
}
