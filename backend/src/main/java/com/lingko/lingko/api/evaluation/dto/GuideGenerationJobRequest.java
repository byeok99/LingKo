package com.lingko.lingko.api.evaluation.dto;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GuideGenerationJobRequest {
    @NotBlank
    @Size(max = 10)
    private String syllable;

    @NotNull
    private VideoType type;

    @NotEmpty
    @Size(max = 10)
    private List<@NotEmpty @Size(min = 1, max = 2) List<@NotBlank String>> urlPairs;

    public String trimmedSyllable() {
        return syllable.trim();
    }
}
