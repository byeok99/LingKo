package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 홈 화면이 실제 서버 데이터와 제대로 이어지는지 확인한다. UI 상태 자체는 위젯 테스트가 맡는다. */
class HomeScreenTest extends BaseTest {

    @Test
    @DisplayName("홈 화면은 서버의 추천 문장을 보여준다")
    void showsRecommendedSentencesFromServer() {
        // 앱이 이전 세션에서 고른 탭을 유지하므로 Daily가 기본 선택이라고 가정하지 않는다
        HomePage home = new HomePage(driver).waitUntilLoaded().selectCategory("Daily");

        assertTrue(home.showsSentence(SeedSentences.DAILY_FIRST),
                "Daily 탭에 시드 문장이 보여야 한다: " + SeedSentences.DAILY_FIRST);
    }

    @Test
    @DisplayName("모든 상황 탭에 서버 문장이 보인다 (추천 문장 limit=50 가정 보호)")
    void everyCategoryShowsServerSentences() {
        HomePage home = new HomePage(driver).waitUntilLoaded();

        // 탭마다 앱 세션을 새로 열면 느리므로 한 세션에서 돌고, 실패는 모아서 한 번에 보고한다
        assertAll(SeedSentences.FIRST_BY_CATEGORY.stream()
                .map(seed -> (Executable) () -> {
                    home.selectCategory(seed.tab());
                    assertFalse(home.showsEmptyCategory(seed.tab()),
                            seed.tab() + " 탭이 비어 있다");
                    assertTrue(home.showsSentence(seed.firstSentence()),
                            seed.tab() + " 탭에 시드 문장이 없다: " + seed.firstSentence());
                }));
    }
}