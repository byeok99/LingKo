package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.HomePage.PracticeEnergy;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.TestSupportApi;
import com.lingko.e2e.support.TestSupportApi.ServerQuota;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 홈의 평가 기회 표시가 서버의 평가 기회·다음 충전 시각과 일치하는지 확인한다(Notion TC-013). */
class PracticeQuotaTest extends BaseTest {

    // 화면은 초 단위로 줄고 조회 시점도 달라, 정확히 같은 값 대신 이 범위 안에 들면 일치로 본다
    private static final Duration COUNTDOWN_TOLERANCE = Duration.ofSeconds(5);

    private static ServerQuota serverQuota;
    private static long quotaSetAtNanos;

    // BaseTest의 @BeforeEach가 앱을 띄우는 순간 홈이 quota를 조회하므로, 그보다 먼저 도는 @BeforeAll에서 맞춘다
    @BeforeAll
    static void setRemainingPracticesToThree() {
        TestSupportApi.ensureE2eUser();
        serverQuota = TestSupportApi.setRemainingPractices(3);
        // 경과 시간은 호스트 시계 대신 단조 시계로 잰다. 시계가 서버와 어긋나도 비교가 흔들리지 않는다
        quotaSetAtNanos = System.nanoTime();
    }

    @Test
    @DisplayName("TC-013 홈에 남은 평가 기회와 다음 충전까지 남은 시간이 표시된다")
    void showsRemainingPracticesAndRefillCountdown() {
        PracticeEnergy energy = new HomePage(driver).waitUntilLoaded().practiceEnergy();

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
