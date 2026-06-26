package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeHistoryResponse {
    private List<PracticeHistoryItemResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean hasNext;
    private Integer bestScore;
}
