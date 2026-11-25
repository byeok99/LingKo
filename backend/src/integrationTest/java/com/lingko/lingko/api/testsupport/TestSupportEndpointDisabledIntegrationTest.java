package com.lingko.lingko.api.testsupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** e2e 프로필이 아니면 테스트 지원 경로(세션 발급, 평가 기회 설정)가 아예 존재하지 않는지 검증한다. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
class TestSupportEndpointDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("e2e 프로필이 없으면 테스트 세션 발급 경로는 404다")
    void testSupportEndpointDoesNotExistWithoutE2eProfile() throws Exception {
        mockMvc.perform(post("/test-support/sessions"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("e2e 프로필이 없으면 평가 기회 설정 경로는 404다")
    void quotaEndpointDoesNotExistWithoutE2eProfile() throws Exception {
        mockMvc.perform(put("/test-support/quota").param("remaining", "3"))
                .andExpect(status().isNotFound());
    }
}