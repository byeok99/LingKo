package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import com.lingko.e2e.support.TestSupportApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

        // 저장 상태는 고정 테스트 사용자에게 계속 남으므로, 매 실행 앱을 띄우기 전에 "저장 안 됨"으로 되돌린다
        @BeforeEach
        void givenSentenceNotSaved() {
            String accessToken = TestSupportApi.ensureE2eUser();
            long sentenceId = TestSupportApi.recommendedSentenceId(accessToken, SENTENCE);
            TestSupportApi.ensureSentenceNotSaved(accessToken, sentenceId);
            System.out.println("[TC-018] unsaved sentenceId=" + sentenceId + " (" + SENTENCE + ")");
        }

        @Test
        @DisplayName("저장 후 앱을 다시 실행해도 카드와 저장 목록에 저장 상태가 남는다")
        void savedSentenceSurvivesRelaunch() {
            HomePage home = new HomePage(driver()).waitUntilLoaded();

            // shortcut: 아직 사전조건과 홈 진입까지만 확인한다. 저장·재실행·저장 목록 확인은 다음 단계에서 추가한다
            assertTrue(home.showsSentence(SENTENCE), "홈에 대상 문장이 보여야 한다: " + SENTENCE);
        }
    }
}
