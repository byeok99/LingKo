package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.AzureSettings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Azure 발음 평가 대기가 무기한 이어지지 않는 계약을 고정한다.
 *
 * <p>평가 Worker는 작업을 하나씩 처리하므로 응답 없는 호출 하나가 전체 평가 큐를 멈추는 회귀를 막는다.
 * 실제 SDK recognizer는 native library가 필요해 대기 로직만 분리해 검증한다.</p>
 */
class AzureSpeechEvaluatorTimeoutTest {

    @Test
    @DisplayName("응답이 오지 않으면 설정 시간 뒤 timeout 예외를 던지고 future를 취소한다")
    void timesOutAndCancelsHangingRecognition() {
        CompletableFuture<String> neverCompletes = new CompletableFuture<>();

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> AzureSpeechEvaluator.awaitRecognition(neverCompletes, 1))
                .isInstanceOf(SpeechEvaluationTimeoutException.class)
                .hasMessageContaining("1초");
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(neverCompletes.isCancelled()).isTrue();
        assertThat(elapsedMillis).isBetween(900L, 5_000L);
    }

    @Test
    @DisplayName("시간 안에 끝난 인식 결과는 그대로 반환한다")
    void returnsCompletedRecognition() throws Exception {
        assertThat(AzureSpeechEvaluator.awaitRecognition(CompletableFuture.completedFuture("result"), 1))
                .isEqualTo("result");
    }

    @Test
    @DisplayName("SDK 호출 실패는 timeout과 구분해 ExecutionException으로 전달한다")
    void propagatesSdkFailureSeparatelyFromTimeout() {
        CompletableFuture<String> failed = CompletableFuture.failedFuture(new RuntimeException("auth failed"));

        assertThatThrownBy(() -> AzureSpeechEvaluator.awaitRecognition(failed, 1))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @DisplayName("timeout 기본값은 30초이며 1~120초 범위 밖 설정은 거부한다")
    void validatesTimeoutRange() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        AzureSettings settings = new AzureSettings();

        assertThat(settings.getRecognitionTimeoutSeconds()).isEqualTo(30);
        assertThat(validator.validate(settings)).isEmpty();

        settings.setRecognitionTimeoutSeconds(0);
        assertThat(validator.validate(settings)).isNotEmpty();
        settings.setRecognitionTimeoutSeconds(121);
        assertThat(validator.validate(settings)).isNotEmpty();
    }
}
