package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.common.ErrorResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationApplicationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Evaluation Result 기능의 HTTP 진입점을 제공한다.
 *
 * 컨트롤러는 전송 형식 검증과 응답 변환만 담당하고 업무 결정은 도메인 서비스에 위임한다.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "evaluation.legacy-multipart-enabled",
        havingValue = "true"
)
public class EvaluationResultController {

    private final EvaluationService evaluationService;
    private final EvaluationApplicationService evaluationApplicationService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> evaluate(
            @RequestPart(required = false) MultipartFile audio,
            @RequestParam(required = false) Long sentenceId,
            @RequestParam(required = false) String text,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        // 비용이 발생하는 평가 흐름은 입력 검증보다 먼저 활성 로그인 세션을 확인해 익명 호출을 차단한다.
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);

        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("VALIDATION_FAILED", "audio is required"));
        }

        if (sentenceId == null && isBlank(text)) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("VALIDATION_FAILED", "sentenceId or text is required"));
        }

        if (audio.getSize() > EvaluationService.MAX_AUDIO_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ErrorResponse.of(
                            "AUDIO_TOO_LARGE",
                            "Audio must not exceed " + EvaluationService.MAX_AUDIO_BYTES + " bytes"
                    ));
        }

        EvaluationService.AudioValidationStatus validationStatus = evaluationService.validateAudio(audio);
        if (validationStatus == EvaluationService.AudioValidationStatus.UNSUPPORTED_TYPE) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "Only WAV audio is supported"));
        }
        if (validationStatus == EvaluationService.AudioValidationStatus.INVALID_WAV) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ErrorResponse.of("INVALID_WAV", "A valid 16-bit mono PCM WAV file is required"));
        }

        PracticeResultResponse response =
                evaluationApplicationService.evaluate(userId, audio, sentenceId, text);
        return ResponseEntity.ok(response);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
