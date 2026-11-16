package com.lingko.lingko;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 운영 health endpoint의 최소 공개 계약과 DB 장애 전파를 검증한다. */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "management.endpoints.web.discovery.enabled=false"
})
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HikariDataSource dataSource;

    @Test
    void exposesOnlyMinimalHealthResponse() throws Exception {
        // Given: 기본 설정(health만 노출, discovery 비활성)으로 뜬 context

        // When & Then: health는 상세 없이 UP만 응답한다
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));

        // When & Then: 그 외 Actuator 경로는 모두 닫혀 있다
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound());
        // Prometheus registry가 classpath에 있어도 기본 설정에서는 scrape 경로가 열리지 않아야 한다.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void reportsDownWithoutDatabaseDetails() throws Exception {
        // Given: DB connection pool을 닫아 DB 장애 상태를 만든다
        dataSource.close();

        // When & Then: 503 DOWN만 응답하고 DB 상세는 노출하지 않는다
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
