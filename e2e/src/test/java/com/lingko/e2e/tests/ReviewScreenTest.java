package com.lingko.e2e.tests;

import com.lingko.e2e.pages.BottomTabs;
import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.TestSupportApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 기록(Review) 탭 E2E.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.</p>
 */
class ReviewScreenTest extends BaseTest {

    @Nested
    @DisplayName("TC-038 연습 기록이 없으면 첫 연습 안내가 표시된다")
    class NoHistory {

        private int historyCount;

        // 서버에 기록을 지우는 test-support API가 없어, 고정 사용자의 기록이 0건일 때만 이 TC가 성립한다.
        // 로컬 환경은 평가 업로드가 실패해 기록이 쌓이지 않으므로 지금은 항상 0건이다
        @BeforeEach
        void given() {
            String accessToken = TestSupportApi.ensureE2eUser();
            historyCount = TestSupportApi.evaluationHistoryCount(accessToken);
        }

        @Test
        @DisplayName("기록 탭을 열면 'No practice history yet' 안내가 보인다")
        void showsFirstPracticeGuide() {
            // 사전조건이 깨진 것을 앱 결함으로 오인하지 않도록 실패 대신 건너뛴다
            assumeTrue(historyCount == 0, "사전조건: 고정 테스트 사용자의 연습 기록이 0건이어야 한다. 현재 " + historyCount + "건");
            new HomePage(driver()).waitUntilLoaded();

            assertTrue(BottomTabs.openReview(driver()).showsNoHistoryGuide(),
                    "기록이 없으면 첫 연습 안내가 표시되어야 한다");
        }
    }
}
