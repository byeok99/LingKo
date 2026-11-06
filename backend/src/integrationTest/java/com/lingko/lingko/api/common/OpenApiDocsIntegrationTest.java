package com.lingko.lingko.api.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OpenAPI 문서와 Swagger UI가 로컬에서 명시적으로 켰을 때만 열리는지 검증한다.
 *
 * <p>보장 대상: 기본 설정(운영과 같은 조건)에서는 {@code /v3/api-docs}와 {@code /swagger-ui.html}이 404이고,
 * {@code SPRINGDOC_ENABLED=true}에 해당하는 설정에서는 문서가 열리며 Swagger UI에서 토큰을 보낼 수 있도록
 * bearer security scheme이 포함된다. 기본값이 바뀌어 운영에 문서가 노출되는 회귀를 막는다.
 */
class OpenApiDocsIntegrationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("기본 설정")
    class 기본값 {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("API 문서와 Swagger UI는 404를 반환한다")
        void 문서는_닫혀_있다() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "springdoc.api-docs.enabled=true",
            "springdoc.swagger-ui.enabled=true"
    })
    @AutoConfigureMockMvc
    @DisplayName("로컬에서 켠 설정")
    class 활성화 {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("API 문서가 열리고 bearer 인증 scheme을 포함한다")
        void 문서와_인증_scheme이_열린다() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("LingKo API"))
                    .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                    .andExpect(jsonPath("$.paths['/api/auth/token/refresh']").exists());
        }

        @Test
        @DisplayName("Swagger UI 진입 경로가 UI 페이지로 이동한다")
        void swaggerUi가_열린다() throws Exception {
            mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        }
    }
}
