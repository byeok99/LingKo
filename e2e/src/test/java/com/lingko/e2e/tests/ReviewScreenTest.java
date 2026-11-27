package com.lingko.e2e.tests;

import com.lingko.e2e.pages.BottomTabs;
import com.lingko.e2e.pages.HistoryDetailPage;
import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.pages.ReviewPage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.TestSupportApi;
import com.lingko.e2e.support.TestSupportApi.SeedEvaluation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 기록(Review) 탭 E2E.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.
 * 로컬은 실제 평가가 끝까지 돌지 않으므로, 기록은 test-support 시드로 "평가가 끝난" 상태를 만든다. 그래서 이 클래스는
 * 기록을 <b>읽는</b> 화면만 검증하고 평가 자체는 검증하지 않는다. 단어별 결과의 단어는 실제 평가와 같이 표준 발음 어절이다.</p>
 */
class ReviewScreenTest extends BaseTest {

    @Nested
    @DisplayName("TC-038 연습 기록이 없으면 첫 연습 안내가 표시된다")
    class NoHistory {

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.clearEvaluations();
        }

        @Test
        @DisplayName("기록 탭을 열면 'No practice history yet' 안내가 보인다")
        void showsFirstPracticeGuide() {
            new HomePage(driver()).waitUntilLoaded();

            assertTrue(BottomTabs.openReview(driver()).showsNoHistoryGuide(),
                    "기록이 없으면 첫 연습 안내가 표시되어야 한다");
        }
    }

    @Nested
    @DisplayName("TC-037 연습 기록은 최신순으로 표시된다")
    class NewestFirst {

        // TC Test Data: A → B → C 순서로 연습
        private static final String A = "감사합니다";
        private static final String B = "음악을 들어요";
        private static final String C = "안녕하세요";

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.clearEvaluations();
            TestSupportApi.seedEvaluations(List.of(
                    SeedEvaluation.custom(A, 60, 60),
                    SeedEvaluation.custom(B, 70, 70, 70),
                    SeedEvaluation.custom(C, 80, 80)));
        }

        @Test
        @DisplayName("A, B, C 순서로 연습하면 목록이 위에서부터 C, B, A다")
        void listsNewestFirst() {
            new HomePage(driver()).waitUntilLoaded();
            List<String> sentences = BottomTabs.openReview(driver()).waitUntilHistoryLoaded().historySentencesInOrder();

            assertEquals(List.of(C, B, A), sentences, "가장 최근 기록이 맨 위에 있어야 한다");
        }
    }

    /**
     * TC 기대결과 3은 단어를 원문 어절('음악을' 65, '들어요' 80)로 적었지만, 실제 평가는 표준 발음을 기준 문장으로 써서
     * 단어도 표준 발음 어절('으마글', '드러요')로 저장·표시된다(EvaluationJobExecutor → job.getStandardPronunciation()).
     * 그래서 같은 점수를 표준 발음 어절로 확인한다.
     */
    @Nested
    @DisplayName("TC-039 연습 기록을 누르면 당시 문장, 세부 점수, 단어별 결과가 표시된다")
    class HistoryDetail {

        private static final String SENTENCE = "음악을 들어요";

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.clearEvaluations();
            TestSupportApi.seedEvaluations(List.of(
                    new SeedEvaluation(null, SENTENCE, null, 72, 70, 80, 75, List.of(65, 80))));
        }

        @Test
        @DisplayName("문장·종합 72·Accuracy 70·Fluency 80·Completeness 75·단어 65/80이 보인다")
        void showsSentenceScoresAndWords() {
            new HomePage(driver()).waitUntilLoaded();
            ReviewPage review = BottomTabs.openReview(driver()).waitUntilHistoryLoaded();
            // 기록 행을 연 것이 곧 기대결과 1의 문장 확인이다(문장이 다르면 행을 찾지 못해 실패한다)
            HistoryDetailPage detail = review.openHistory(SENTENCE);

            assertAll(
                    () -> assertEquals(72, detail.overallScore(), "종합 점수"),
                    () -> assertEquals(Map.of("Accuracy", 70, "Fluency", 80, "Completeness", 75), detail.scoreBreakdown(),
                            "세부 점수"),
                    () -> assertEquals(Map.of("으마글", "65", "드러요", "80"), detail.wordScores(), "단어별 점수"));
        }
    }

    @Nested
    @DisplayName("TC-040 'Practice again'을 누르면 같은 문장으로 연습이 열린다")
    class PracticeAgain {

        private static final String SENTENCE = "음악을 들어요";
        // 기록에 저장된 snapshot. 현재 규칙 결과와 일부러 다르게 넣는다(TC Test Data)
        private static final String STORED_PRONUNCIATION = "음악을 들어요";
        private static final String CURRENT_PRONUNCIATION = "으마글 드러요";

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            // 녹음 버튼이 'Record'로 보이려면 평가 기회가 있어야 한다(기대결과 3)
            TestSupportApi.setRemainingPractices(5);
            TestSupportApi.clearEvaluations();
            TestSupportApi.seedEvaluations(List.of(
                    new SeedEvaluation(null, SENTENCE, STORED_PRONUNCIATION, 72, 70, 80, 75, List.of(65, 80))));
        }

        @Test
        @DisplayName("저장된 snapshot이 아니라 현재 규칙의 표준 발음으로 연습이 열리고 바로 녹음할 수 있다")
        void reopensWithCurrentPronunciation() {
            new HomePage(driver()).waitUntilLoaded();
            HistoryDetailPage detail = BottomTabs.openReview(driver()).waitUntilHistoryLoaded().openHistory(SENTENCE);
            // 사전조건 확인: 상세에는 저장된 snapshot이 보여야 이 TC가 "다시 준비"를 검증한다
            String shownInHistory = detail.standardPronunciation();

            PracticePage practice = detail.practiceAgain();
            String preparedPronunciation = practice.waitForStandardPronunciation(CURRENT_PRONUNCIATION);
            practice.waitUntilPrepared();

            assertAll(
                    () -> assertEquals(STORED_PRONUNCIATION, shownInHistory, "사전조건: 기록에는 저장된 snapshot이 보여야 한다"),
                    () -> assertTrue(practice.showsSentence(SENTENCE), "같은 문장으로 연습 화면이 열려야 한다"),
                    () -> assertEquals(CURRENT_PRONUNCIATION, preparedPronunciation, "표준 발음은 현재 규칙 결과여야 한다"),
                    () -> assertTrue(practice.canRecord(), "바로 녹음을 시작할 수 있어야 한다"));
        }
    }

    /**
     * TC는 '저는' 85·'음악을' 70은 숫자로, '들어요'만 '—'로 보이길 기대하지만, 제품은 단어 점수가 하나라도 신뢰할 수 없으면
     * 그 기록의 단어 점수를 하나도 저장하지 않는다(EvaluationPersistenceService.hasReliableWordScores, 실제 평가도 all-or-none).
     * 그래서 기록 화면에서는 "일부만 '—'" 상태가 생길 수 없고, 이 TC는 "신뢰할 수 없는 점수는 0이 아니라 '—'(score unavailable)"
     * 부분만 검증한다. 기대결과 3(처음 선택 단어)은 점수 있는 단어가 없어 검증하지 않는다.
     */
    @Nested
    @DisplayName("TC-061 신뢰할 수 없는 단어별 평가 점수는 0점이 아닌 '—'로 표시된다")
    class UnreliableWordScore {

        private static final String SENTENCE = "저는 음악을 들어요";

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.clearEvaluations();
            TestSupportApi.seedEvaluations(List.of(
                    new SeedEvaluation(null, SENTENCE, null, 75, 75, 75, 75, java.util.Arrays.asList(85, 70, null))));
        }

        @Test
        @DisplayName("점수를 신뢰할 수 없는 기록의 단어는 0이 아니라 'score unavailable'로 보인다")
        void showsUnavailableInsteadOfZero() {
            new HomePage(driver()).waitUntilLoaded();
            Map<String, String> words = BottomTabs.openReview(driver()).waitUntilHistoryLoaded()
                    .openHistory(SENTENCE)
                    .wordScores();

            assertAll(
                    () -> assertEquals(List.of("저는", "으마글", "드러요"), List.copyOf(words.keySet()), "단어는 표준 발음 어절 순서다"),
                    () -> assertTrue(words.values().stream().allMatch("unavailable"::equals),
                            "신뢰할 수 없는 점수는 0이 아니라 'score unavailable'이어야 한다: " + words));
        }
    }

    @Nested
    @DisplayName("TC-064 연습 기록 점수 변화 그래프는 저장된 평가 기록과 일치한다")
    class ProgressChart {

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.clearEvaluations();
            // TC Test Data: 60 → 75 → 90(오래된 순)
            TestSupportApi.seedEvaluations(List.of(
                    SeedEvaluation.custom("감사합니다", 60, 60),
                    SeedEvaluation.custom("안녕하세요", 75, 75),
                    SeedEvaluation.custom("천천히 말해요", 90, 90, 90)));
        }

        @Test
        @DisplayName("'Your progress · Last 3 tries'·Latest score 90과 그래프 label이 기록과 같다")
        void chartMatchesHistory() {
            new HomePage(driver()).waitUntilLoaded();
            String summary = BottomTabs.openReview(driver()).waitUntilHistoryLoaded().progressSummary();
            List<String> lines = List.of(summary.split("\n"));

            assertAll(
                    // 화면 제목은 EyebrowLabel이 대문자로 그린다
                    () -> assertTrue(summary.toLowerCase().startsWith("your progress · last 3 tries"),
                            "진행 요약 제목이 보여야 한다: " + summary),
                    () -> assertEquals("90", lines.get(lines.indexOf("Latest score") + 1), "Latest score는 90이어야 한다"),
                    () -> assertTrue(summary.endsWith("Recent scores 60, 75, 90, recent average 75, latest score 90"),
                            "그래프 접근성 label이 기록과 같아야 한다: " + summary));
        }
    }
}
