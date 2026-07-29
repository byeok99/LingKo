package com.lingko.lingko.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

/**
 * SQS Worker 모드에서만 Queue client를 생성해 DB fallback 실행에는 추가 인프라를 요구하지 않는다.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${evaluation.worker.mode:database}'.equalsIgnoreCase('sqs')"
)
public class SqsConfig {

    private final AwsSettings awsSettings;
    private final EvaluationJobSettings evaluationSettings;

    @Bean
    public SqsClient sqsClient() {
        requireQueueUrl();
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(awsSettings.getS3().getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                awsSettings.getCredentials().getAccessKey(),
                                awsSettings.getCredentials().getSecretKey()
                        )
                ));
        String endpoint = evaluationSettings.getQueue().getEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }
        return builder.build();
    }

    private void requireQueueUrl() {
        String queueUrl = evaluationSettings.getQueue().getUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException(
                    "evaluation.queue.url is required in SQS worker mode"
            );
        }
    }
}
