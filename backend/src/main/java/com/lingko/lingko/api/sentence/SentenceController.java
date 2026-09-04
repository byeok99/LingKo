package com.lingko.lingko.api.sentence;

import com.lingko.lingko.api.sentence.dto.PracticeSentenceResponse;
import com.lingko.lingko.api.sentence.dto.RecommendedSentencesResponse;
import com.lingko.lingko.core.domain.sentence.service.SentenceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentence 기능의 HTTP 진입점을 제공한다.
 *
 * 컨트롤러는 전송 형식 검증과 응답 변환만 담당하고 업무 결정은 도메인 서비스에 위임한다.
 */
@Validated
@RestController
@RequestMapping("/api/sentences")
@RequiredArgsConstructor
public class SentenceController {

    private final SentenceService sentenceService;

    @GetMapping("/recommended")
    public RecommendedSentencesResponse getRecommendedSentences(
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) String category
    ) {
        return sentenceService.findRecommendedSentences(limit, category);
    }

    @GetMapping("/{sentenceId}")
    public PracticeSentenceResponse getSentence(@PathVariable Long sentenceId) {
        return sentenceService.getSentence(sentenceId);
    }
}
