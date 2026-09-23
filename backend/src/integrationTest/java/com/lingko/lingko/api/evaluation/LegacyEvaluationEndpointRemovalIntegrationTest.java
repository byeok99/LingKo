package com.lingko.lingko.api.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 현재 앱이 사용하지 않는 구형 multipart 평가 endpoint가 다시 활성화되지 않는 계약을 검증한다.
 *
 * <p>과거 운영 환경변수가 남아 있어도 동기 평가 경로가 열리면 API·Worker 이중 흐름이 부활하므로,
 * stale 설정을 강제로 주입한 상태에서 실제 Spring MVC mapping 부재를 확인한다.
 */
@SpringBootTest(properties = "evaluation.legacy-multipart-enabled=true")
class LegacyEvaluationEndpointRemovalIntegrationTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("과거 활성화 설정이 남아 있어도 multipart 평가 endpoint를 등록하지 않는다")
    void doesNotRegisterLegacyMultipartEvaluationEndpoint() {
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .noneMatch(this::isLegacyEvaluationPost);
    }

    private boolean isLegacyEvaluationPost(RequestMappingInfo mapping) {
        return mapping.getPatternValues().contains("/api/evaluations")
                && mapping.getMethodsCondition().getMethods().contains(RequestMethod.POST);
    }
}
