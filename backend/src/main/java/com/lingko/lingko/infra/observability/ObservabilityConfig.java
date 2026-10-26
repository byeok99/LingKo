package com.lingko.lingko.infra.observability;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** CloudWatch 비용과 cardinality를 제한하면서 운영 판단에 필요한 지표만 허용한다. */
@Configuration
public class ObservabilityConfig {

    private static final List<String> EXPORTED_METRICS = List.of(
            "lingko.",
            "jvm.memory.used",
            "jvm.memory.max",
            "jvm.gc.pause",
            "jvm.threads.live",
            "http.server.requests",
            "hikaricp.connections.active",
            "hikaricp.connections.idle",
            "hikaricp.connections.pending"
    );

    /** 사용자·URI별 고카디널리티 지표가 우발적으로 전송되는 것을 기본 거부한다. */
    @Bean
    public MeterFilter cloudWatchMetricAllowlist() {
        return MeterFilter.denyUnless(id -> EXPORTED_METRICS.stream().anyMatch(id.getName()::startsWith));
    }
}
