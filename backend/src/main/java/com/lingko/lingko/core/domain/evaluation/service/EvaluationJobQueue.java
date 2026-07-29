package com.lingko.lingko.core.domain.evaluation.service;

import java.util.Optional;

/**
 * 평가 작업 ID만 전달하는 Queue 경계다.
 *
 * 작업 상태와 payload는 DB에 유지해 Queue 중복 전달이나 재생성에도 동일한 상태 전이를 사용한다.
 */
public interface EvaluationJobQueue {

    void publish(String jobId);

    Optional<Message> receive();

    void acknowledge(Message message);

    void release(Message message, int delaySeconds);

    record Message(String jobId, String receiptHandle) {
        public Message {
            if (jobId == null || jobId.isBlank()) {
                throw new IllegalArgumentException("jobId must not be blank");
            }
            if (receiptHandle == null || receiptHandle.isBlank()) {
                throw new IllegalArgumentException("receiptHandle must not be blank");
            }
        }
    }
}
