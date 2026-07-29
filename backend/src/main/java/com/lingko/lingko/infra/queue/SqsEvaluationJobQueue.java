package com.lingko.lingko.infra.queue;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Optional;

/**
 * SQS 표준 Queue의 at-least-once 전달을 평가 작업 Queue 계약으로 변환한다.
 */
@Component
@ConditionalOnExpression(
        "'${evaluation.worker.mode:database}'.equalsIgnoreCase('sqs')"
)
public class SqsEvaluationJobQueue implements EvaluationJobQueue {

    private final SqsClient sqsClient;
    private final EvaluationJobSettings.Queue settings;
    private final String queueUrl;

    public SqsEvaluationJobQueue(
            SqsClient sqsClient,
            EvaluationJobSettings evaluationSettings
    ) {
        this.sqsClient = sqsClient;
        settings = evaluationSettings.getQueue();
        queueUrl = requireQueueUrl(settings.getUrl());
    }

    @Override
    public void publish(String jobId) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(jobId)
                .build());
    }

    @Override
    public Optional<Message> receive() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(settings.getReceiveWaitSeconds())
                        .visibilityTimeout(settings.getVisibilityTimeoutSeconds())
                        .build()
        );
        return response.messages().stream()
                .findFirst()
                .map(message -> new Message(
                        message.body(),
                        message.receiptHandle()
                ));
    }

    @Override
    public void acknowledge(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }

    @Override
    public void release(Message message, int delaySeconds) {
        if (delaySeconds < 0 || delaySeconds > 43_200) {
            throw new IllegalArgumentException(
                    "SQS visibility delay must be between 0 and 43200 seconds"
            );
        }
        sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .visibilityTimeout(delaySeconds)
                .build());
    }

    private String requireQueueUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("evaluation.queue.url must not be blank");
        }
        return value.trim();
    }
}
