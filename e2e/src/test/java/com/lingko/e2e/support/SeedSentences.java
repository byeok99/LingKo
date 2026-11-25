package com.lingko.e2e.support;

import java.util.List;

/**
 * 서버 Flyway 시드(V2·V22 {@code recommended_sentences})의 카테고리별 첫 문장.
 *
 * <p>서버는 {@code sort_order, sentence_id} 오름차순으로 내려주므로 각 카테고리에서 sort_order가 가장 작은 문장이다.
 * 시드 순서나 문구가 바뀌면 이 값만 고친다. {@code tab}은 앱 카테고리 탭에 보이는 글자다.
 * 값은 SQL 원문이 아니라 API 응답 {@code originalText} 기준이다. 서버가 끝의 마침표·물음표를 빼고 내려준다.</p>
 */
public final class SeedSentences {

    public record Seed(String tab, String firstSentence) {
    }

    public static final String DAILY_FIRST = "천천히 말씀해 주세요";
    // Notion TC-014 Test Data와 같은 값이다. 시드가 바뀌면 TC와 이 값을 함께 고친다
    public static final String FOOD_FIRST = "맛있겠다";

    public static final List<Seed> FIRST_BY_CATEGORY = List.of(
            new Seed("Daily", DAILY_FIRST),
            new Seed("Food", FOOD_FIRST),
            new Seed("Travel", "지하철역이 어디예요"),
            new Seed("Study", "한국어를 배우고 있어요"),
            new Seed("Work", "회사에 늦었어요"),
            new Seed("Health", "병원에 가야 해요"));

    private SeedSentences() {
    }
}
