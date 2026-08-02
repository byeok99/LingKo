package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PronunciationPrepareResponse;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evaluation 서비스 Prepare Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class EvaluationServicePrepareTest {

    @Test
    @DisplayName("직접 입력 문장을 표준 발음과 guide item으로 준비한다")
    void prepareCustomSentenceBuildsGuideItems() {
        SyllableMappingUtil mappingUtil = mock(SyllableMappingUtil.class);
        when(mappingUtil.getImageUrl("ㅁ", VideoType.MOUTH)).thenReturn("https://guides/mouth/m.png");
        when(mappingUtil.getImageUrl("ㅁ", VideoType.TONGUE)).thenReturn("https://guides/tongue/m.png");
        when(mappingUtil.getImageUrl("ㅏ", VideoType.MOUTH)).thenReturn("https://guides/mouth/a.png");
        EvaluationService service = new EvaluationService(mappingUtil);

        PronunciationPrepareResponse response = service.prepareCustomSentence("  맛있겠다.!?  ");

        assertThat(response.getSentence().getSource()).isEqualTo("CUSTOM");
        assertThat(response.getSentence().getOriginalText()).isEqualTo("맛있겠다");
        assertThat(response.getSentence().getStandardPronunciation()).isEqualTo("마싣껟따");
        assertThat(response.getSentence().getCharacters()).isNotEmpty();

        GuideCharacterResponse first = response.getSentence().getCharacters().get(0);
        assertThat(first.getPosition()).isZero();
        assertThat(first.getText()).isEqualTo("마");
        assertThat(first.getPronunciationText()).isEqualTo("마");
        assertThat(first.getPhonemes()).containsExactly("ㅁ", "ㅏ");
        assertThat(first.getGuideType()).isEqualTo("TONGUE");
        assertThat(first.getGuideStatus()).isEqualTo("AVAILABLE");
        assertThat(first.getMouthGuideUrl()).isEqualTo("https://guides/mouth/m.png");
        assertThat(first.getTongueGuideUrl()).isEqualTo("https://guides/tongue/m.png");
    }
}
