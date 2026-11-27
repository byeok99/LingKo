package com.lingko.e2e.tests;

import com.lingko.e2e.pages.BottomTabs;
import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.SavedSentencesPage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import com.lingko.e2e.support.TestSupportApi;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 문장 저장 기능 E2E. 저장은 홈 카드와 프로필의 저장 목록 두 화면에 걸치므로 기능 단위로 묶는다.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.</p>
 */
class SavedSentenceTest extends BaseTest {

    @Nested
    @DisplayName("TC-016 저장한 문장은 다시 조회해도 저장 상태가 유지된다")
    class SavedAcrossRelaunch {

        // 앱을 새로 실행하면 홈은 Daily 탭에서 시작하므로 그 첫 문장을 대상으로 한다
        private static final String SENTENCE = SeedSentences.DAILY_FIRST;

        // 앱이 저장 요청을 보낸 뒤 서버 목록에 들어오기까지 기다리는 최대 시간
        private static final Duration SAVE_SYNC_TIMEOUT = Duration.ofSeconds(10);

        private String accessToken;
        private long sentenceId;

        // 저장 상태는 고정 테스트 사용자에게 계속 남으므로, 매 실행 앱을 띄우기 전에 "저장 안 됨"으로 되돌린다
        @BeforeEach
        void givenSentenceNotSaved() {
            accessToken = TestSupportApi.ensureE2eUser();
            sentenceId = TestSupportApi.recommendedSentenceId(accessToken, SENTENCE);
            TestSupportApi.ensureSentenceNotSaved(accessToken, sentenceId);
        }

        @Test
        @DisplayName("저장 후 앱을 다시 실행해도 카드와 저장 목록에 저장 상태가 남는다")
        void savedSentenceSurvivesRelaunch() {
            HomePage home = new HomePage(driver()).waitUntilLoaded();
            assertFalse(home.isSaved(SENTENCE), "사전조건: 대상 문장이 저장 안 됨 상태여야 한다");

            home.toggleSaved(SENTENCE);
            // 앱은 버튼부터 바꾸고 서버에 요청한다. 요청이 끝나기 전에 앱을 끄면 저장이 사라지므로 서버 반영을 먼저 확인한다
            TestSupportApi.waitUntilSentenceSaved(accessToken, sentenceId, SAVE_SYNC_TIMEOUT);

            relaunchApp();
            HomePage relaunched = new HomePage(driver()).waitUntilLoaded();

            // 기대결과 1과 2는 서로 다른 화면이라 순서대로 확인하되, 하나가 실패해도 나머지 결과를 함께 보고한다
            assertAll(
                    () -> assertTrue(relaunched.isSaved(SENTENCE), "재실행 후 홈 카드가 저장 상태여야 한다: " + SENTENCE),
                    () -> assertTrue(BottomTabs.openProfile(driver()).openSavedSentences().showsSentence(SENTENCE),
                            "프로필 'Saved sentences' 목록에 문장이 있어야 한다: " + SENTENCE));
        }
    }

    /**
     * Notion TC-046·047은 Daily 추천 → Travel 추천 → 자유 문장을 저장한 상태를 전제한다. 지금 제품은 자유 문장을
     * 저장할 수 없어(서버는 추천 문장 id만 저장, 연습 화면에 저장 버튼 없음) 추천 문장 2개로 사전조건을 만든다.
     * 그래서 기대 개수는 2이고, 'My own' 필터는 빈 안내가 보이는 것으로 확인한다.
     */
    private static String givenDailyThenTravelSaved() {
        String accessToken = TestSupportApi.ensureE2eUser();
        TestSupportApi.clearSavedSentences(accessToken);
        TestSupportApi.ensureSentenceSaved(accessToken,
                TestSupportApi.recommendedSentenceId(accessToken, SeedSentences.DAILY_FIRST));
        TestSupportApi.ensureSentenceSaved(accessToken,
                TestSupportApi.recommendedSentenceId(accessToken, SeedSentences.TRAVEL_FIRST));
        return accessToken;
    }

    @Nested
    @DisplayName("TC-046 저장한 문장 목록은 최근 저장순으로 표시된다")
    class SavedListOrder {

        @BeforeEach
        void given() {
            givenDailyThenTravelSaved();
        }

        @Test
        @DisplayName("나중에 저장한 Travel 문장이 위, 먼저 저장한 Daily 문장이 아래에 있고 '2 sentences'가 보인다")
        void showsMostRecentFirst() {
            new HomePage(driver()).waitUntilLoaded();
            SavedSentencesPage saved = BottomTabs.openProfile(driver()).openSavedSentences();

            assertAll(
                    () -> assertEquals(List.of(SeedSentences.TRAVEL_FIRST, SeedSentences.DAILY_FIRST),
                            saved.visibleSentencesInOrder(List.of(SeedSentences.DAILY_FIRST, SeedSentences.TRAVEL_FIRST)),
                            "최근에 저장한 문장이 위에 있어야 한다"),
                    () -> assertTrue(saved.showsCount(2), "목록 위에 '2 sentences'가 보여야 한다"));
        }
    }

    @Nested
    @DisplayName("TC-047 저장한 문장 목록에서 분류 필터를 선택하면 해당 분류의 문장만 표시된다")
    class SavedListFilter {

        private static final List<String> SAVED = List.of(SeedSentences.DAILY_FIRST, SeedSentences.TRAVEL_FIRST);

        @BeforeEach
        void given() {
            givenDailyThenTravelSaved();
        }

        @Test
        @DisplayName("'Daily'는 Daily 문장만, 'Travel'은 Travel 문장만 보이고, 'My own'은 빈 안내가 보인다")
        void filtersByCategory() {
            new HomePage(driver()).waitUntilLoaded();
            SavedSentencesPage saved = BottomTabs.openProfile(driver()).openSavedSentences();

            List<String> daily = saved.selectFilter("Daily").visibleSentencesInOrder(SAVED);
            List<String> travel = saved.selectFilter("Travel").visibleSentencesInOrder(SAVED);
            List<String> own = saved.selectFilter("My own").visibleSentencesInOrder(SAVED);
            boolean ownEmptyShown = saved.showsEmptyFilter("My own");

            assertAll(
                    () -> assertEquals(List.of(SeedSentences.DAILY_FIRST), daily, "'Daily'는 Daily 문장 1개만 보여야 한다"),
                    () -> assertEquals(List.of(SeedSentences.TRAVEL_FIRST), travel, "'Travel'은 Travel 문장 1개만 보여야 한다"),
                    () -> assertEquals(List.of(), own, "'My own'은 추천 문장을 보이지 않아야 한다"),
                    () -> assertTrue(ownEmptyShown, "'My own'에 저장 문장이 없으면 빈 안내가 보여야 한다"));
        }
    }
}
