package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 전체 현대 한글을 중복 없는 가이드 영상 시퀀스로 축약하는 사전 생성 계약을 검증한다. */
class GuideMediaPrewarmPlannerTest {

    private GuideMediaPrewarmPlanner planner;

    @BeforeEach
    void setUp() {
        SyllableMappingUtil mappingUtil = new SyllableMappingUtil(null);
        mappingUtil.loadMapping();
        planner = new GuideMediaPrewarmPlanner(mappingUtil);
    }

    @Test
    void plansEveryUniqueDynamicSequenceAcrossModernHangul() {
        var plan = planner.planAll();

        assertThat(plan).hasSize(693);
        assertThat(plan.stream().filter(item -> item.type() == VideoType.MOUTH)).hasSize(61);
        assertThat(plan.stream().filter(item -> item.type() == VideoType.TONGUE)).hasSize(632);
        assertThat(GuideMediaPrewarmService.countUniqueSegments(plan)).isEqualTo(94);
        assertThat(GuideMediaPrewarmService.countUniqueSegments(
                plan.stream().filter(item -> item.type() == VideoType.MOUTH).toList()
        )).isEqualTo(26);
        assertThat(GuideMediaPrewarmService.countUniqueSegments(
                plan.stream().filter(item -> item.type() == VideoType.TONGUE).toList()
        )).isEqualTo(68);
        assertThat(plan)
                .extracting(GuideMediaPrewarmItem::signature)
                .doesNotHaveDuplicates();
        assertThat(plan)
                .allSatisfy(item -> assertThat(item.urlPairs())
                        .isNotEmpty()
                        .allSatisfy(pair -> assertThat(pair).hasSize(2)));
    }
}
