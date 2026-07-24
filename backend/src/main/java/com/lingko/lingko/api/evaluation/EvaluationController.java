package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.StandardPronunciationRequest;
import com.lingko.lingko.api.evaluation.dto.StandardPronunciationResponse;
import com.lingko.lingko.api.evaluation.dto.PronunciationPrepareRequest;
import com.lingko.lingko.api.evaluation.dto.PronunciationPrepareResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * Evaluation 기능의 HTTP 진입점을 제공한다.
 *
 * 컨트롤러는 전송 형식 검증과 응답 변환만 담당하고 업무 결정은 도메인 서비스에 위임한다.
 */
@RestController
@RequestMapping("/api/pronunciation")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    private final EvaluationService service;

    /**
     * 표준발음 변환
     * POST /api/pronunciation/convert
     */
    @PostMapping("/convert")
    public ResponseEntity<StandardPronunciationResponse> convertToStandardPronunciation(
            @Valid @RequestBody StandardPronunciationRequest request
    ) {
        log.info("표준발음 변환 요청: {}", request.getText());

        String standardPronunciation = service.convertToStandardPronunciation(request.getText());

        StandardPronunciationResponse response = StandardPronunciationResponse.builder()
                .originalText(request.getText())
                .standardPronunciation(standardPronunciation)
                .build();

        log.info("표준발음 변환 완료: {} -> {}", request.getText(), standardPronunciation);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/prepare")
    public ResponseEntity<PronunciationPrepareResponse> preparePronunciation(
            @Valid @RequestBody PronunciationPrepareRequest request
    ) {
        if (!request.isCustom()) {
            throw new IllegalArgumentException("Only CUSTOM prepare requests are supported");
        }

        PronunciationPrepareResponse response = service.prepareCustomSentence(request.trimmedText());
        log.info("발음 연습 준비 완료: {}", request.trimmedText());

        return ResponseEntity.ok(response);
    }

//    @PostMapping("/videos")
//    public
}
