package com.lingko.lingko.api.common;

import com.lingko.lingko.api.evaluation.EvaluationController;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
}
