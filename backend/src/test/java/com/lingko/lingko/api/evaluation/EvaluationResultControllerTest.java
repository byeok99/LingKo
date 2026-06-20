package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationResultController.class)
class EvaluationResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @Test
    @DisplayName("multipart audio와 text를 받아 발음 평가 결과를 반환한다")
    void evaluateWithText() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                new byte[]{1, 2, 3}
        );
        PracticeResultResponse response = PracticeResultResponse.builder()
                .overallScore(87)
                .gradeLabel("Good")
                .summary("Good pronunciation.")
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(88)
                        .fluency(86)
                        .completeness(90)
                        .build())
                .weakCharacters(List.of())
                .characters(List.of())
                .build();
        when(evaluationService.isSupportedAudio(any())).thenReturn(true);
        when(evaluationService.evaluatePronunciation(any(), eq(null), eq("안녕하세요."))).thenReturn(response);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(87))
                .andExpect(jsonPath("$.gradeLabel").value("Good"))
                .andExpect(jsonPath("$.scoreBreakdown.accuracy").value(88));
    }

    @Test
    @DisplayName("audio가 없으면 400을 반환한다")
    void audioIsRequired() throws Exception {
        mockMvc.perform(multipart("/api/evaluations")
                        .param("text", "안녕하세요."))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("practiceToken 단독 요청은 Phase 5에서 지원하지 않고 400을 반환한다")
    void practiceTokenOnlyIsRejected() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("practiceToken", "prep_test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("sentenceId or text is required"));
    }

    @Test
    @DisplayName("지원하지 않는 audio 형식이면 415를 반환한다")
    void unsupportedAudio() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "recording.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3}
        );
        when(evaluationService.isSupportedAudio(any())).thenReturn(false);

        mockMvc.perform(multipart("/api/evaluations")
                        .file(audio)
                        .param("text", "안녕하세요."))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }
}
