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
