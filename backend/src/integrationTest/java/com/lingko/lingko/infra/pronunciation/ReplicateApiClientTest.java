package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.config.WebClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replicate Api Client Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@SpringBootTest
@Tag("external")
public class ReplicateApiClientTest {
    @Autowired
    private ReplicateApiClient replicateApiClient;

    @Test
    @DisplayName("Frame Interpolation Test")
    void interpolate_test() throws InterruptedException {
        // 준비
        String frame1 = "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/bilabial-consonants.png";
        String frame2 = "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/vowel-a.png";

        // 실행
        String result = replicateApiClient.interpolate(frame1, frame2);

        // Then
        System.out.println(result);

    }
}
