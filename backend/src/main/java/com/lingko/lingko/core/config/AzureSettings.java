package com.lingko.lingko.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 외부 Azure 설정을 type-safe application 설정으로 binding한다.
 *
 * 환경값 조회를 코드 곳곳에 분산하지 않고 시작 시 검증과 비밀정보 처리를 중앙화하기 위해 형식이 지정된 binding을 선택했다.
 */
@Configuration
@ConfigurationProperties(prefix = "azure")
@Validated
@Getter @Setter
public class AzureSettings {
    private String secretKey;
    private String region;
    private String language = "ko-KR";

    /**
     * 발음 평가 한 건의 결과를 기다리는 최대 시간(초)이다.
     *
     * <p>평가 Worker는 작업을 한 번에 하나씩 처리하므로, 응답이 오지 않는 호출 하나가 무기한 대기하면
     * 다른 모든 사용자의 평가가 멈춘다. 업로드 음성은 최대 10초 녹음이라 정상 평가는 이보다 훨씬 짧다.
     * 상한 120초는 작업 lease(기본 600초) 안에서 재시도 여유를 남기기 위한 값이다.</p>
     */
    @Min(1)
    @Max(120)
    private int recognitionTimeoutSeconds = 30;
}
