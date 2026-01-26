package com.lingko.lingko.api.practice;

import com.lingko.lingko.api.practice.dto.StandardPronunciationRequest;
import com.lingko.lingko.api.practice.dto.StandardPronunciationResponse;
import com.lingko.lingko.core.domain.practice.service.PracticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pronunciation")
@RequiredArgsConstructor
@Slf4j
public class PracticeController {
    private final PracticeService service;

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

//    @PostMapping("/videos")
//    public
}
