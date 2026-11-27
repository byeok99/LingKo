package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.HomePage.PracticeEnergy;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.pages.SoundDetailPage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import com.lingko.e2e.support.TestSupportApi;
import com.lingko.e2e.support.TestSupportApi.SeedEvaluation;
import com.lingko.e2e.support.TestSupportApi.ServerQuota;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 홈 화면 E2E. 홈이 실제 서버 데이터와 제대로 이어지는지 확인한다. UI 상태 자체는 위젯 테스트가 맡는다.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.
 * 앱은 테스트 본문에서 {@code driver()}를 처음 부를 때 뜨므로, hook에서 바꾼 서버 상태를 앱이 읽은 채로 시작한다.</p>
 */
class HomeScreenTest extends BaseTest {

    @Test
    @DisplayName("홈 화면은 서버의 추천 문장을 보여준다")
    void showsRecommendedSentencesFromServer() {
        // 기본 선택 탭이 바뀌어도 깨지지 않도록 Daily를 직접 고른다
        HomePage home = new HomePage(driver()).waitUntilLoaded().selectCategory("Daily");

        assertTrue(home.showsSentence(SeedSentences.DAILY_FIRST),
                "Daily 탭에 시드 문장이 보여야 한다: " + SeedSentences.DAILY_FIRST);
    }

    // Notion TC 번호가 없는 회귀 테스트다. 6개 상황 모두에 문장이 내려오는지(추천 문장 limit=50 가정)를 지킨다
    @Test
    @DisplayName("모든 상황 탭에 서버 문장이 보인다 (추천 문장 limit=50 가정 보호)")
    void everyCategoryShowsServerSentences() {
        HomePage home = new HomePage(driver()).waitUntilLoaded();

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

    @Nested
    @DisplayName("TC-014 상황 칩을 선택하면 그 상황의 추천 문장이 표시된다")
    class CategoryFilter {

        // 사전조건은 기본 시드 상태라 따로 맞추지 않는다. 대상 문장은 각 상황의 첫 문장이라 미리보기(3개) 안에 항상 있다
        @Test
        @DisplayName("Food는 Food 문장만, Travel은 Travel 문장만 보인다")
        void showsOnlySelectedCategorySentences() {
            String food = SeedSentences.FOOD_FIRST;
            String travel = SeedSentences.TRAVEL_FIRST;
            HomePage home = new HomePage(driver()).waitUntilLoaded();

            // 기대결과 1과 2는 탭을 바꿔 가며 이어서 확인하므로, 앞 단계 결과를 먼저 모아 둔 뒤 함께 보고한다.
            // "보이지 않음"은 locator가 아무것도 못 찾아도 통과하므로, 같은 locator로 보이는 카드를 찾는 것도 함께 확인해
            // 없음 결과가 의미 있게 한다
            home.selectCategory("Food");
            boolean foodShowsFood = home.showsSentence(food) && home.hasSentenceCard(food);
            boolean foodHidesTravel = !home.hasSentenceCard(travel);

            home.selectCategory("Travel");
            boolean travelShowsTravel = home.showsSentence(travel) && home.hasSentenceCard(travel);
            boolean travelHidesFood = !home.hasSentenceCard(food);

            assertAll(
                    () -> assertTrue(foodShowsFood, "Food를 고르면 '" + food + "'가 보여야 한다"),
                    () -> assertTrue(foodHidesTravel, "Food를 고르면 Travel 문장 '" + travel + "'는 보이지 않아야 한다"),
                    () -> assertTrue(travelShowsTravel, "Travel을 고르면 '" + travel + "'가 보여야 한다"),
                    () -> assertTrue(travelHidesFood, "Travel을 고르면 Food 문장 '" + food + "'는 보이지 않아야 한다"));
        }
    }

    @Nested
    @DisplayName("TC-013 홈에 남은 평가 기회와 다음 충전까지 남은 시간이 표시된다")
    class RemainingPractices {

        // 화면은 초 단위로 줄고 조회 시점도 달라, 정확히 같은 값 대신 이 범위 안에 들면 일치로 본다
        private static final Duration COUNTDOWN_TOLERANCE = Duration.ofSeconds(5);
        // 자연 충전 간격. 카운트다운은 이 값을 넘을 수 없다(기대결과 2)
        private static final Duration REFILL_INTERVAL = Duration.ofHours(1);
        // TC는 10초 뒤 다시 보라고 하지만, 1초마다 줄어드는 값이라 "줄어들 때까지" 기다리면 같은 것을 더 빨리 확인한다
        private static final Duration COUNTDOWN_TICK_TIMEOUT = Duration.ofSeconds(15);

        private ServerQuota serverQuota;
        private long quotaSetAtNanos;

        @BeforeEach
        void givenThreePracticesLeft() {
            TestSupportApi.ensureE2eUser();
            serverQuota = TestSupportApi.setRemainingPractices(3);
            // 경과 시간은 호스트 시계 대신 단조 시계로 잰다. 시계가 서버와 어긋나도 비교가 흔들리지 않는다
            quotaSetAtNanos = System.nanoTime();
        }

        @Test
        @DisplayName("3/5와 서버와 같은 카운트다운이 보이고, 카운트다운이 줄어든다")
        void showsRemainingPracticesAndRefillCountdown() {
            HomePage home = new HomePage(driver()).waitUntilLoaded();
            PracticeEnergy energy = home.practiceEnergy();

            // 서버가 알려 준 남은 시간에서 그 뒤 흐른 시간을 빼면 지금 화면에 보여야 할 값이다
            Duration expectedUntilRefill = serverQuota.timeUntilRefill()
                    .minusNanos(System.nanoTime() - quotaSetAtNanos);

            assertAll(
                    () -> assertEquals(3, serverQuota.remaining(), "사전조건: 서버 평가 기회가 3회여야 한다"),
                    () -> assertEquals(serverQuota.remaining(), energy.remaining(), "남은 기회가 서버와 같아야 한다"),
                    () -> assertEquals(serverQuota.limit(), energy.limit(), "최대 기회가 서버와 같아야 한다"),
                    () -> {
                        assertNotNull(energy.untilRefill(), "5회 미만이면 충전 카운트다운이 보여야 한다");
                        Duration gap = energy.untilRefill().minus(expectedUntilRefill).abs();
                        assertTrue(gap.compareTo(COUNTDOWN_TOLERANCE) <= 0,
                                "다음 충전까지 남은 시간이 서버와 " + COUNTDOWN_TOLERANCE.toSeconds()
                                        + "초 안에서 같아야 한다. 화면=" + energy.untilRefill()
                                        + ", 기대=" + expectedUntilRefill);
                        assertTrue(energy.untilRefill().compareTo(REFILL_INTERVAL) <= 0,
                                "카운트다운은 60:00을 넘지 않아야 한다: " + energy.untilRefill());
                    },
                    // 기대결과 3: 처음 읽은 값보다 작아질 때까지 기다리고, 시간 안에 안 줄면 실패한다
                    () -> home.waitUntilCountdownBelow(energy.untilRefill(), COUNTDOWN_TICK_TIMEOUT));
        }
    }

    @Nested
    @DisplayName("TC-015 추천 문장을 선택하면 같은 문장으로 연습 화면이 열린다")
    class SentenceSelection {

        // 평가 기회가 0이면 연습 화면 버튼이 'No practices left'로 바뀌어 Record를 기다리던 진입 확인이 실패한다.
        // 앞서 돈 테스트가 남긴 기회에 기대지 않도록 이 TC가 직접 맞춘다
        @BeforeEach
        void givenPracticesAvailable() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(5);
        }

        @Test
        @DisplayName("Food 첫 문장을 고르면 연습 화면 입력칸에 같은 문장이 들어 있다")
        void selectedSentenceOpensPractice() {
            String sentence = SeedSentences.FOOD_FIRST;

            // openSentence가 Record 버튼까지 기다리므로, 여기까지 오면 기대결과 1(연습 화면 진입)은 확인된 것이다
            PracticePage practice = new HomePage(driver())
                    .waitUntilLoaded()
                    .selectCategory("Food")
                    .openSentence(sentence);

            assertTrue(practice.showsSentence(sentence),
                    "연습 화면 입력칸에 선택한 문장이 있어야 한다: " + sentence);
        }
    }

    /**
     * 기대결과 2·3(서버 충전 1회마다 4/5, 5/5, 5/5에서는 MAX·카운트다운 없음)을 자동화한다.
     * 기대결과 1(기기 시간만 바꿔서는 충전되지 않음)은 Appium으로 시뮬레이터 시계를 바꿀 수 없어 수동 확인으로 남긴다.
     * "1시간 경과"는 test-support로 서버의 다음 충전 시각을 지난 시각으로 옮겨 만든다. 충전 계산은 서버 운영 규칙 그대로다.
     */
    @Nested
    @DisplayName("TC-043 평가 기회가 5회 미만이면 서버 시간 기준 1시간마다 1회 충전된다")
    class HourlyRefill {

        // 다음 충전 시각을 지금보다 1초 전으로 옮겨 "1시간이 지난" 상태를 만든다
        private static final int REFILL_DUE_SECONDS = -1;

        @BeforeEach
        void givenThreePractices() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(3);
        }

        @Test
        @DisplayName("충전 시각이 지날 때마다 3/5 → 4/5 → 5/5가 되고, 5/5에서는 MAX만 보이고 카운트다운이 없다")
        void refillsOncePerElapsedHour() {
            PracticeEnergy start = new HomePage(driver()).waitUntilLoaded().practiceEnergy();

            // 앱은 시작할 때 quota를 읽으므로 서버 충전 뒤 다시 실행해 화면 값을 본다(TC 절차와 같다)
            TestSupportApi.moveNextRefill(REFILL_DUE_SECONDS);
            relaunchApp();
            PracticeEnergy afterFirst = new HomePage(driver()).waitUntilLoaded().practiceEnergy();

            TestSupportApi.moveNextRefill(REFILL_DUE_SECONDS);
            relaunchApp();
            PracticeEnergy afterSecond = new HomePage(driver()).waitUntilLoaded().practiceEnergy();

            assertAll(
                    () -> assertEquals(3, start.remaining(), "사전조건: 시작은 3/5여야 한다"),
                    () -> assertEquals(4, afterFirst.remaining(), "첫 충전 뒤 4/5여야 한다"),
                    () -> assertNotNull(afterFirst.untilRefill(), "4/5는 5회 미만이라 다음 충전 카운트다운이 보여야 한다"),
                    () -> assertEquals(5, afterSecond.remaining(), "두 번째 충전 뒤 5/5여야 한다"),
                    () -> assertNull(afterSecond.untilRefill(), "5/5에서는 카운트다운 대신 MAX가 보여야 한다"));
        }
    }

    @Nested
    @DisplayName("TC-045 평가 기회가 5회이면 광고를 시청할 수 없다")
    class MaximumPractices {

        @BeforeEach
        void givenFivePractices() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(5);
        }

        @Test
        @DisplayName("5/5와 MAX가 보이고 'Add one practice' 버튼이 없다")
        void hidesAdButtonAtMaximum() {
            HomePage home = new HomePage(driver()).waitUntilLoaded();
            // 캡슐이 그려진 뒤 버튼 유무를 봐야 "아직 안 그려짐"과 "숨김"이 구분된다
            PracticeEnergy energy = home.practiceEnergy();
            boolean adButtonShown = home.showsAddPracticeButton();

            assertAll(
                    () -> assertEquals(5, energy.remaining(), "남은 기회 5가 보여야 한다"),
                    () -> assertEquals(5, energy.limit(), "최대 기회 5가 보여야 한다"),
                    // 화면의 'MAX'는 접근성 label에서 'maximum'으로 읽히며, 이때 카운트다운이 없다(untilRefill == null)
                    () -> assertNull(energy.untilRefill(), "최대치면 카운트다운 대신 MAX가 보여야 한다"),
                    () -> assertFalse(adButtonShown, "최대치면 'Add one practice' 버튼이 없어야 한다"));
        }
    }

    /**
     * TC Test Data는 '밥' 40, '물' 60, '옷' 80이다. 취약 음절은 표준 발음 어절에서 모으는데(실제 평가도 표준 발음을 기준 문장으로 씀),
     * '옷'의 표준 발음은 '옫'이라 화면에도 '옫'으로 보인다. TC 데이터는 그대로 두고 화면 기대값을 제품 동작에 맞춘다.
     */
    @Nested
    @DisplayName("TC-041 취약 음절은 평균 점수가 낮은 순으로 표시된다")
    class WeakSoundsOrder {

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.clearEvaluations();
            // 취약 음절은 2회 이상 연습한 음절만 오르므로 단어마다 2회씩 넣는다(TC Test Data)
            TestSupportApi.seedEvaluations(List.of(
                    SeedEvaluation.custom("밥", 40, 40), SeedEvaluation.custom("밥", 40, 40),
                    SeedEvaluation.custom("물", 60, 60), SeedEvaluation.custom("물", 60, 60),
                    SeedEvaluation.custom("옷", 80, 80), SeedEvaluation.custom("옷", 80, 80)));
        }

        @Test
        @DisplayName("왼쪽부터 '밥', '물', '옫'(옷의 표준 발음) 순서다")
        void ordersWeakestFirst() {
            List<String> sounds = new HomePage(driver()).waitUntilLoaded().weakSoundsInOrder();

            assertEquals(List.of("밥", "물", "옫"), sounds, "평균 점수가 낮은 음절이 왼쪽에 있어야 한다");
        }
    }

    /**
     * TC는 '밥'을 쓰지만 로컬 시드 추천 문장 중 표준 발음에 '밥'이 들어간 문장이 없어 'Suggested'가 비므로
     * (사전조건 2 불충족), 추천 문장 '물 한 잔 주세요'가 있는 '물'로 같은 절차를 검증한다.
     */
    @Nested
    @DisplayName("TC-042 취약 음절을 누르면 평균 점수, 연습 횟수, 연습한 문장과 새 문장 탭이 표시된다")
    class WeakSoundDetail {

        private static final String SOUND = "물";

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(5);
            TestSupportApi.clearEvaluations();
            TestSupportApi.seedEvaluations(List.of(
                    SeedEvaluation.custom(SOUND, 40, 40), SeedEvaluation.custom(SOUND, 40, 40)));
        }

        @Test
        @DisplayName("'Average 40 across 2 tries'·Practiced 2·Suggested 문장에 '물'이 있고, 고르면 그 문장으로 연습이 열린다")
        void showsDetailAndOpensSuggestion() {
            SoundDetailPage detail = new HomePage(driver()).waitUntilLoaded().openWeakSound(SOUND);
            String average = detail.averageSummary();
            int practiced = detail.practicedCount();
            List<String> suggested = detail.suggestedSentences();

            assertAll(
                    () -> assertEquals("Average 40 across 2 tries", average, "평균 점수와 연습 횟수"),
                    () -> assertEquals(2, practiced, "'Practiced 2' 탭"),
                    () -> assertFalse(suggested.isEmpty(), "사전조건: '물'이 들어간 추천 문장이 있어야 한다"),
                    () -> assertTrue(suggested.stream().allMatch(sentence -> sentence.contains(SOUND)),
                            "Suggested 문장에는 모두 '물'이 들어 있어야 한다: " + suggested));

            String chosen = suggested.get(0);
            PracticePage practice = detail.selectSuggested(chosen).waitUntilInputReady();
            assertTrue(practice.showsSentence(chosen), "고른 문장으로 연습 화면이 열려야 한다: " + chosen);
        }
    }
}
