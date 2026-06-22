package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.common.ErrorResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationResultController {

    private final EvaluationService evaluationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> evaluate(
            @RequestPart(required = false) MultipartFile audio,
            @RequestParam(required = false) Long sentenceId,
            @RequestParam(required = false) String text
    ) {
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

        PracticeResultResponse response = evaluationService.evaluatePronunciation(audio, sentenceId, text);
        return ResponseEntity.ok(response);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
