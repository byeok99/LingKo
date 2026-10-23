package com.lingko.lingko.core.domain.evaluation.exception;

/**
 * 업로드된 음성이 허용 형식(RIFF/WAVE, 16-bit mono PCM, 48kHz 이하)이 아님을 나타낸다.
 *
 * <p>같은 파일을 다시 읽어도 결과가 바뀌지 않는 영구 실패다. Worker는 이 예외를 재시도하지 않고 즉시
 * {@code INVALID_AUDIO}로 종료하며, 앱은 일시 장애가 아니라 다시 녹음이 필요하다고 안내할 수 있다.
 * 외부 연동 실패를 뜻하는 {@link VideoGenerationException}과 구분하기 위해 별도 타입으로 둔다.</p>
 */
public class InvalidEvaluationAudioException extends RuntimeException {

    public InvalidEvaluationAudioException(String message) {
        super(message);
    }
}
