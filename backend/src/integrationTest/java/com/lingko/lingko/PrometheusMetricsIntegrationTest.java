package com.lingko.lingko;

import io.micrometer.statsd.StatsdMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * docker-compose.observability.yml이 주는 설정으로 켰을 때 Prometheus scrape 경로가 열리는지 검증한다.
 *
 * <p>보장 대상:
 * <ul>
 *   <li>Grafana dashboard가 조회하는 JVM·Tomcat·HikariCP·HTTP 지표가 Prometheus에 모두 노출된다.</li>
 *   <li>CloudWatch로 가는 StatsD registry에는 여전히 allowlist 지표만 남는다. allowlist를 StatsD 전용으로
 *       옮기면서 운영 CloudWatch 비용·cardinality 계약이 깨지지 않았는지 확인하는 회귀 테스트다.</li>
 * </ul>
 * 기본값에서 닫혀 있는 계약은 {@link ActuatorHealthIntegrationTest}가 검증한다.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "management.prometheus.metrics.export.enabled=true",
        "management.endpoints.web.exposure.include=health,prometheus",
        // StatsD는 UDP 전송이라 수신자가 없어도 실패하지 않는다. registry를 만들어 filter 적용만 확인한다.
        "management.statsd.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
class PrometheusMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatsdMeterRegistry statsdMeterRegistry;

    @Test
    void exposesDashboardMetricsWhenEnabled() throws Exception {
        // Given: http_server_requests 지표는 요청이 한 번 이상 들어온 뒤에 생긴다.
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        // When & Then
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("http_server_requests_seconds")))
                // 아래는 CloudWatch allowlist 밖이라, 필터가 Prometheus에 새면 사라지는 지표다.
                .andExpect(content().string(containsString("process_cpu_usage")))
                .andExpect(content().string(containsString("hikaricp_connections_max")))
                .andExpect(content().string(containsString("jvm_threads_peak_threads")));
    }

    @Test
    void keepsCloudWatchAllowlistOnStatsdRegistry() {
        // Given: StatsD export를 켠 context (class 설정)

        // Then: allowlist 지표는 남는다
        assertThat(statsdMeterRegistry.find("jvm.memory.used").meters()).isNotEmpty();
        assertThat(statsdMeterRegistry.find("hikaricp.connections.active").meters()).isNotEmpty();

        // Then: allowlist 밖 지표는 StatsD에서 걸러진다
        assertThat(statsdMeterRegistry.find("process.cpu.usage").meters()).isEmpty();
        assertThat(statsdMeterRegistry.find("hikaricp.connections.max").meters()).isEmpty();
        assertThat(statsdMeterRegistry.find("jvm.threads.peak").meters()).isEmpty();
    }
}
