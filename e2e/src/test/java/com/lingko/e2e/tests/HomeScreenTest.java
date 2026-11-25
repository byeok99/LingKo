package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.HomePage.PracticeEnergy;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import com.lingko.e2e.support.TestSupportApi;
import com.lingko.e2e.support.TestSupportApi.ServerQuota;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @DisplayName("TC-013 홈에 남은 평가 기회와 다음 충전까지 남은 시간이 표시된다")
    class RemainingPractices {

        // 화면은 초 단위로 줄고 조회 시점도 달라, 정확히 같은 값 대신 이 범위 안에 들면 일치로 본다
        private static final Duration COUNTDOWN_TOLERANCE = Duration.ofSeconds(5);

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
        @DisplayName("남은 기회·최대 기회·카운트다운이 서버 값과 같다")
        void showsRemainingPracticesAndRefillCountdown() {
            PracticeEnergy energy = new HomePage(driver()).waitUntilLoaded().practiceEnergy();

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
                    });
        }
    }

    @Nested
    @DisplayName("TC-014 추천 문장을 선택하면 같은 문장으로 연습을 시작할 수 있다")
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
}
