package com.lingko.lingko.infra.observability;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * CloudWatch 비용과 cardinality를 제한하면서 운영 판단에 필요한 지표만 허용한다.
 *
 * <p>allowlist는 CloudWatch로 나가는 StatsD registry에만 건다. {@code MeterFilter} bean으로 등록하면
 * Spring Boot가 모든 registry에 적용해, 로컬 부하 측정용 Prometheus registry까지 같은 9개 지표로 줄어든다.
 * 운영에는 StatsD registry만 있으므로 CloudWatch로 전송되는 지표는 bean 방식과 같다.
 */
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

    /** StatsD registry가 만들어질 때 allowlist를 적용한다. 다른 registry에는 영향을 주지 않는다. */
    @Bean
    public MeterRegistryCustomizer<StatsdMeterRegistry> statsdMetricAllowlist() {
        return registry -> registry.config().meterFilter(cloudWatchMetricAllowlist());
    }

    /** 사용자·URI별 고카디널리티 지표가 우발적으로 전송되는 것을 기본 거부한다. */
    public MeterFilter cloudWatchMetricAllowlist() {
        return MeterFilter.denyUnless(id -> EXPORTED_METRICS.stream().anyMatch(id.getName()::startsWith));
    }
}
