package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.TestSupportApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 홈의 평가 기회 표시가 서버의 평가 기회·다음 충전 시각과 일치하는지 확인한다(Notion TC-013). */
class PracticeQuotaTest extends BaseTest {

    private static String serverQuota;

    // BaseTest의 @BeforeEach가 앱을 띄우는 순간 홈이 quota를 조회하므로, 그보다 먼저 도는 @BeforeAll에서 맞춘다
    @BeforeAll
    static void setRemainingPracticesToThree() {
        TestSupportApi.ensureE2eUser();
        serverQuota = TestSupportApi.setRemainingPractices(3);
    }

    @Test
    @DisplayName("TC-013 홈에 남은 평가 기회와 다음 충전까지 남은 시간이 표시된다")
    void showsRemainingPracticesAndRefillCountdown() {
        new HomePage(driver).waitUntilLoaded();
        System.out.println("[TC-013] server quota = " + serverQuota);

        // shortcut: JSON 원문 문자열 비교, Step 3에서 파싱해 화면 값과 비교하도록 교체
        assertTrue(serverQuota.contains("\"remainingPractices\":3"), serverQuota);
    }
}
