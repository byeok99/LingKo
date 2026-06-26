package com.lingko.lingko.api.common;

import com.lingko.lingko.api.evaluation.EvaluationController;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @Test
    @DisplayName("validation 실패는 ErrorResponse 구조와 VALIDATION_FAILED code를 반환한다")
    void validationFailureReturnsErrorResponse() throws Exception {
        mockMvc.perform(post("/api/pronunciation/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("text"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    @Test
    @DisplayName("외부 연동 실패는 내부 URL을 노출하지 않는 고정 메시지를 반환한다")
    void videoGenerationFailureDoesNotExposeInternalDetails() throws Exception {
        when(evaluationService.convertToStandardPronunciation(anyString()))
                .thenThrow(new VideoGenerationException(
                        "외부 미디어 URL 연결 실패: https://replicate.delivery/private/result.mp4"
                ));

        mockMvc.perform(post("/api/pronunciation/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"맛있겠다\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EVALUATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Pronunciation evaluation failed. Please try again."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("replicate.delivery"))));
    }

    @Test
    @DisplayName("multipart 크기 초과 예외는 413 AUDIO_TOO_LARGE로 반환한다")
    void maxUploadSizeExceededReturnsPayloadTooLarge() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(10 * 1024 * 1024));

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(413);
        org.assertj.core.api.Assertions.assertThat(response.getBody()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(response.getBody().code()).isEqualTo("AUDIO_TOO_LARGE");
        org.assertj.core.api.Assertions.assertThat(response.getBody().message()).isEqualTo("Audio file is too large");
    }
}
