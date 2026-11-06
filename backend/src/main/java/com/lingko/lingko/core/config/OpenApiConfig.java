package com.lingko.lingko.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 로컬 개발용 OpenAPI 문서의 공통 메타데이터와 인증 방식을 정의한다.
 *
 * <p>{@code springdoc.api-docs.enabled=true}(로컬 {@code SPRINGDOC_ENABLED=true})일 때만 등록한다.
 * 운영에서는 문서 자체가 꺼져 있으므로 이 설정도 만들지 않는다.
 *
 * <p>Controller는 Spring Security 없이 {@code @RequestHeader("Authorization")}로 Bearer 토큰을 직접 받는다.
 * OpenAPI 규격은 이름이 {@code Authorization}인 header parameter를 무시하므로, Swagger UI에서 토큰을 보내려면
 * bearer security scheme을 전역으로 선언해야 한다. 전역 선언이라 인증이 필요 없는 endpoint에도 자물쇠가
 * 표시되지만, 실제 인증 여부는 서버가 판단하며 문서 표시는 동작에 영향을 주지 않는다.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    /** Swagger UI의 Authorize 버튼과 요청 header 이름을 연결하는 scheme 식별자다. */
    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI lingkoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LingKo API")
                        .description("로컬 개발용 자동 생성 문서. 계약의 기준은 docs/api/api-reference.md 이다."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
