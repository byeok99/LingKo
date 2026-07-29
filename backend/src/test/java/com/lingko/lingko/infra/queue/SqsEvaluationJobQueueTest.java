package com.lingko.lingko.infra.queue;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SQS adapter가 jobId 외 업무·개인정보를 메시지에 넣지 않고 ACK·재노출을 정확히 위임하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SqsEvaluationJobQueueTest {

    private static final String QUEUE_URL =
            "https://sqs.ap-northeast-2.amazonaws.com/123/evaluation";

    @Mock
    private SqsClient sqsClient;

    private SqsEvaluationJobQueue queue;
    private EvaluationJobSettings settings;

    @BeforeEach
    void setUp() {
        settings = new EvaluationJobSettings();
        settings.getQueue().setUrl(QUEUE_URL);
        queue = new SqsEvaluationJobQueue(sqsClient, settings);
    }

    @Test
    @DisplayName("Queue 메시지 본문에는 jobId만 발행한다")
    void publishesOnlyJobId() {
        queue.publish("job-id");

        ArgumentCaptor<SendMessageRequest> captor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(captor.getValue().messageBody()).isEqualTo("job-id");
    }

    @Test
    @DisplayName("long polling으로 받은 SQS 메시지를 내부 Queue 계약으로 변환한다")
    void receivesMessage() {
        when(sqsClient.receiveMessage(org.mockito.ArgumentMatchers.any(
                ReceiveMessageRequest.class
        ))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder()
                        .body("job-id")
                        .receiptHandle("receipt")
                        .build())
                .build());

        EvaluationJobQueue.Message message = queue.receive().orElseThrow();

        assertThat(message.jobId()).isEqualTo("job-id");
        assertThat(message.receiptHandle()).isEqualTo("receipt");

        ArgumentCaptor<ReceiveMessageRequest> captor =
                ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());
        assertThat(captor.getValue().waitTimeSeconds())
                .isEqualTo(settings.getQueue().getReceiveWaitSeconds());
        assertThat(captor.getValue().visibilityTimeout())
                .isEqualTo(settings.getQueue().getVisibilityTimeoutSeconds());
        assertThat(captor.getValue().maxNumberOfMessages()).isEqualTo(1);
    }

    @Test
    @DisplayName("메시지 완료는 delete, 재시도는 visibility 변경으로 반영한다")
    void acknowledgesAndReleasesMessage() {
        EvaluationJobQueue.Message message =
                new EvaluationJobQueue.Message("job-id", "receipt");

        queue.acknowledge(message);
        queue.release(message, 17);

        ArgumentCaptor<DeleteMessageRequest> deleteCaptor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().receiptHandle()).isEqualTo("receipt");

        ArgumentCaptor<ChangeMessageVisibilityRequest> visibilityCaptor =
                ArgumentCaptor.forClass(ChangeMessageVisibilityRequest.class);
        verify(sqsClient).changeMessageVisibility(visibilityCaptor.capture());
        assertThat(visibilityCaptor.getValue().visibilityTimeout()).isEqualTo(17);
    }
}
