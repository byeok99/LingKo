package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 비용이 발생하는 사전 생성 명령이 안전한 기본값과 명시적 범위를 사용하는지 검증한다. */
class GuideMediaPrewarmOptionsTest {

    @Test
    void defaultsToDryRunAndAllTypes() {
        var options = GuideMediaPrewarmOptions.fromArgs(new String[0]);

        assertThat(options.dryRun()).isTrue();
        assertThat(options.type()).isNull();
        assertThat(options.offset()).isZero();
        assertThat(options.limit()).isEqualTo(Integer.MAX_VALUE);
        assertThat(options.continueOnError()).isTrue();
    }

    @Test
    void parsesExplicitPaidExecutionBounds() {
        var options = GuideMediaPrewarmOptions.fromArgs(new String[]{
                "--prewarm-dry-run=false",
                "--prewarm-type=tongue",
                "--prewarm-offset=10",
                "--prewarm-limit=20",
                "--prewarm-continue-on-error=false"
        });

        assertThat(options.dryRun()).isFalse();
        assertThat(options.type()).isEqualTo(VideoType.TONGUE);
        assertThat(options.offset()).isEqualTo(10);
        assertThat(options.limit()).isEqualTo(20);
        assertThat(options.continueOnError()).isFalse();
    }

    @Test
    void rejectsNonPositiveLimit() {
        assertThatThrownBy(() -> GuideMediaPrewarmOptions.fromArgs(
                new String[]{"--prewarm-limit=0"}
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidBooleanInsteadOfAccidentallyEnablingPaidExecution() {
        assertThatThrownBy(() -> GuideMediaPrewarmOptions.fromArgs(
                new String[]{"--prewarm-dry-run=flase"}
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("true or false");
    }
}
