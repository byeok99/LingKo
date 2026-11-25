package com.lingko.e2e.tests;

import com.lingko.e2e.pages.BottomTabs;
import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import com.lingko.e2e.support.TestSupportApi;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 문장 저장 기능 E2E. 저장은 홈 카드와 프로필의 저장 목록 두 화면에 걸치므로 기능 단위로 묶는다.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.</p>
 */
class SavedSentenceTest extends BaseTest {

    @Nested
    @DisplayName("TC-018 저장한 문장은 다시 조회해도 저장 상태가 유지된다")
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
}
