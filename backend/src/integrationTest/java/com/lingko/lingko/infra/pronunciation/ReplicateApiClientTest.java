package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.config.WebClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReplicateApiClientTest {
    @Autowired
    private ReplicateApiClient replicateApiClient;

    @Test
    @DisplayName("Frame Interpolation Test")
    void interpolate_test() throws InterruptedException {
        // Given
        String frame1 = "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/bilabial-consonants.png";
        String frame2 = "https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/vowel-a.png";

        // When
        String result = replicateApiClient.interpolate(frame1, frame2);

        // Then
        System.out.println(result);

    }
}
