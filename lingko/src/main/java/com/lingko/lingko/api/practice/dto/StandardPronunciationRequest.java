package com.lingko.lingko.api.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StandardPronunciationRequest {
    @NotBlank(message = "문장을 입력해주세요.")
    @Size(min=1, max=30, message = "문장은 30자 이내로 입력해주세요.")
    private String text;
}
