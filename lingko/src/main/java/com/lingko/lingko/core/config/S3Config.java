package com.lingko.lingko.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 설정
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3Config {
    private final AwsSettings awsSettings;

    @Bean
    public S3Client s3Client() {
        log.info("S3Client 초기화: region={}, bucket={}",
                awsSettings.getS3().getRegion(),
                awsSettings.getS3().getBucket());

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                awsSettings.getCredentials().getAccessKey(),
                awsSettings.getCredentials().getSecretKey()
        );

        return S3Client.builder()
                .region(Region.of(awsSettings.getS3().getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}