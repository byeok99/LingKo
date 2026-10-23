package com.lingko.lingko.core.config;

import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 application.yaml 기본값이 TLS proxy 뒤의 실제 클라이언트 IP를 복원하도록 RemoteIpValve를 켜는지 고정한다.
 *
 * <p>이 설정이 빠지면 review 로그인 rate limit이 proxy 주소 하나로 모든 사용자에게 공유되어,
 * 누군가 5번 틀리면 심사자까지 차단되는 회귀가 생긴다. 테스트 context runner는 MockServletContext 때문에
 * WAR 배포로 판정되어 Tomcat customizer가 빠지므로, Spring Boot customizer를 직접 적용해 검증한다.</p>
 */
class ForwardedHeadersConfigurationTest {

    @Test
    @DisplayName("기본 설정은 forwarded header를 해석하는 RemoteIpValve를 등록한다")
    void defaultConfigurationRegistersRemoteIpValve() {
        TomcatServletWebServerFactory factory = customizedFactory(new MockPropertySource());

        assertThat(factory.getEngineValves())
                .anySatisfy(valve -> assertThat(valve).isInstanceOf(RemoteIpValve.class));
    }

    @Test
    @DisplayName("proxy 없는 환경은 none으로 header 해석을 끌 수 있다")
    void noneStrategyDisablesRemoteIpValve() {
        TomcatServletWebServerFactory factory = customizedFactory(
                new MockPropertySource().withProperty("SERVER_FORWARD_HEADERS_STRATEGY", "none")
        );

        assertThat(factory.getEngineValves())
                .noneSatisfy(valve -> assertThat(valve).isInstanceOf(RemoteIpValve.class));
    }

    private TomcatServletWebServerFactory customizedFactory(MockPropertySource overrides) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(overrides);
        environment.getPropertySources().addLast(applicationYaml());
        ServerProperties serverProperties = Binder.get(environment)
                .bindOrCreate("server", ServerProperties.class);
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        new TomcatWebServerFactoryCustomizer(environment, serverProperties).customize(factory);
        return factory;
    }

    private static org.springframework.core.env.PropertySource<?> applicationYaml() {
        try {
            return new YamlPropertySourceLoader()
                    .load("application.yaml", new ClassPathResource("application.yaml"))
                    .get(0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
