package com.lingko.lingko.api.sentence.dto;

import java.util.List;

public record RecommendedSentencesResponse(List<PracticeSentenceResponse> items) {
    public RecommendedSentencesResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
