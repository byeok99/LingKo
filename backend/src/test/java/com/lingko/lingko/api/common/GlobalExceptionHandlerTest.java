package com.lingko.lingko.api.common;

import com.lingko.lingko.api.evaluation.EvaluationController;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Global 예외 Handler Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@WebMvcTest(EvaluationController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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

}
