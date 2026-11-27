package com.lingko.e2e.tests;

import com.lingko.e2e.pages.AiConsentPage;
import com.lingko.e2e.pages.BottomTabs;
import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.LegalDocumentPage;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.pages.ProfilePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.Simulator;
import com.lingko.e2e.support.TestSupportApi;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 프로필 탭 E2E.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.</p>
 */
class ProfileScreenTest extends BaseTest {

    /**
     * 기대결과 2(계정 메뉴)와 계정 이름만 확인한다. E2E 세션의 고정 사용자는 이메일 없이 만들어지므로
     * (test-support {@code /test-support/sessions}) 이메일 표시는 확인할 수 없다.
     */
    @Nested
    @DisplayName("TC-050 프로필 탭에 계정 이름, 이메일과 계정 메뉴가 표시된다")
    class AccountInfo {

        // E2eSessionController가 고정 사용자를 만들 때 넣는 이름
        private static final String ACCOUNT_NAME = "E2E User";
        // TC 기대결과 2의 메뉴. 화면 위에서 아래 순서다
        private static final List<String> MENUS = List.of(
                "Saved sentences", "Terms of Service", "AI assessment privacy", "Privacy Policy",
                "Sign out", "Delete account");

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
        }

        @Test
        @DisplayName("계정 이름과 'Saved sentences'부터 'Delete account'까지 계정 메뉴가 보인다")
        void showsNameAndMenus() {
            new HomePage(driver()).waitUntilLoaded();
            ProfilePage profile = BottomTabs.openProfile(driver());
            // 이름은 화면 맨 위에 있어 메뉴를 찾으며 스크롤하기 전에 본다
            boolean nameShown = profile.showsAccountName(ACCOUNT_NAME);
            List<String> menus = profile.visibleMenusInOrder(MENUS);

            assertAll(
                    () -> assertTrue(nameShown, "계정 이름이 보여야 한다: " + ACCOUNT_NAME),
                    () -> assertEquals(MENUS, menus, "계정 메뉴가 모두 보여야 한다"));
        }
    }

    /**
     * 기대결과 1·2(각 문서가 열린다)만 자동화한다. 기대결과 3(문서를 닫으면 프로필로 돌아온다)은 수동 확인으로 남긴다.
     * 문서는 인앱 Safari 화면으로 열리는데, XCUITest로는 그 '완료' 버튼을 누를 수 없기 때문이다({@link LegalDocumentPage}).
     * 그래서 두 번째 문서를 열기 전에 앱을 다시 실행해 Safari 화면을 정리한다.
     */
    /**
     * 프로필 탭 배너만 확인한다. 기록 탭 배너는 연습 기록이 있을 때만 그려지는데(review_screen.dart), 로컬은 평가가
     * 성공하지 않아 기록이 0건이다. 배너가 콘텐츠를 가리지 않는지는 화면 배치 판단이라 수동 확인으로 남긴다.
     */
    @Nested
    @DisplayName("TC-052 기록 탭과 프로필 탭에 배너 광고가 표시된다")
    class BannerAd {

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
        }

        @Test
        @DisplayName("프로필 탭에 배너 광고가 보인다")
        void showsBannerOnProfile() {
            new HomePage(driver()).waitUntilLoaded();

            assertTrue(BottomTabs.openProfile(driver()).showsBannerAd(), "프로필 탭에 배너 광고가 보여야 한다");
        }
    }

    /**
     * 앱에는 UMP 디버그 지역을 고르는 설정이 없어, 시뮬레이터가 EEA 밖(UMP 미대상)이라는 실행 환경에 기댄다.
     * EEA로 설정된 기기에서 돌리면 메뉴가 보이는 것이 정상이므로 이 TC는 실패한다.
     */
    @Nested
    @DisplayName("TC-054 UMP 미대상 지역에서는 'Advertising privacy' 메뉴가 표시되지 않는다")
    class AdvertisingPrivacyHidden {

        // 앱이 프로필을 연 뒤 UMP 초기화·요건 조회를 마칠 때까지 두는 시간
        private static final Duration UMP_SETTLE = Duration.ofSeconds(5);

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
        }

        @Test
        @DisplayName("프로필 메뉴에 'Advertising privacy'가 없다")
        void hidesAdvertisingPrivacyMenu() {
            new HomePage(driver()).waitUntilLoaded();

            assertTrue(BottomTabs.openProfile(driver()).advertisingPrivacyHiddenAfter(UMP_SETTLE),
                    "UMP 미대상 지역에서는 'Advertising privacy' 메뉴가 없어야 한다");
        }
    }

    @Nested
    @DisplayName("TC-051 로그인 후 프로필의 문서 메뉴에서 이용약관과 개인정보 처리방침을 각각 열 수 있다")
    class LegalDocuments {

        // backend /legal/terms, /legal/privacy 영어 문서의 제목(h1)
        private static final String TERMS_HEADING = "LingKo Terms of Service";
        private static final String PRIVACY_HEADING = "LingKo Privacy Policy";

        @BeforeEach
        void given() {
            TestSupportApi.ensureE2eUser();
        }

        @Test
        @DisplayName("'Terms of Service'는 이용약관을, 'Privacy Policy'는 개인정보 처리방침을 연다")
        void opensEachDocument() {
            new HomePage(driver()).waitUntilLoaded();
            boolean termsShown = BottomTabs.openProfile(driver())
                    .openLegalDocument("Terms of Service")
                    .showsHeading(TERMS_HEADING);

            // Safari 화면을 닫을 수 없어 앱을 다시 실행한다. 다시 실행하면 홈에서 시작한다
            relaunchApp();
            new HomePage(driver()).waitUntilLoaded();
            boolean privacyShown = BottomTabs.openProfile(driver())
                    .openLegalDocument("Privacy Policy")
                    .showsHeading(PRIVACY_HEADING);
            // 다음 테스트는 forceAppLaunch로 새로 시작하므로 열린 문서를 따로 정리하지 않는다

            assertAll(
                    () -> assertTrue(termsShown, "이용약관 문서가 열려야 한다"),
                    () -> assertTrue(privacyShown, "개인정보 처리방침 문서가 열려야 한다"));
        }
    }

    /**
     * 프로필에서 AI 처리 동의를 철회한 뒤 새 평가를 시도한다. 철회는 화면 조작으로 하므로 TC-024처럼 API로
     * 철회하지 않는다. E2E 앱이 시작할 때 동의를 다시 기록하므로, 앱이 뜬 뒤 화면에서 철회하는 이 순서가 그대로 맞다.
     */
    @Nested
    @DisplayName("TC-048 AI 처리 허용을 철회하면 새 평가를 시작할 수 없다")
    class AiConsentWithdrawn {

        private static final int PRACTICES = 3;
        private static final String SENTENCE = "감사합니다";
        private static final Duration RECORD_FOR = Duration.ofSeconds(2);

        private String accessToken;
        private int historyBefore;

        @BeforeEach
        void given() {
            Simulator.setMicrophoneAllowed(true);
            accessToken = TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(PRACTICES);
            historyBefore = TestSupportApi.evaluationHistoryCount(accessToken);
        }

        @Test
        @DisplayName("프로필에서 'Withdraw permission' 후 녹음하면 평가 대신 AI 처리 안내가 뜨고 기회 3회·기록 수가 그대로다")
        void withdrawnConsentBlocksNewEvaluation() {
            new HomePage(driver()).waitUntilLoaded();
            BottomTabs.openProfile(driver()).openAiConsent().withdrawPermission();

            PracticePage practice = BottomTabs.openHome(driver())
                    .openCustomPractice()
                    .typeSentence(SENTENCE)
                    .recordAndRequestEvaluation(RECORD_FOR);
            // 동의 화면이 시간 안에 뜨지 않으면 여기서 실패한다(기대결과 1)
            new AiConsentPage(driver()).waitUntilLoaded().declineSending();
            boolean backWithRecording = practice.showsKeptRecording();

            assertAll(
                    () -> assertTrue(backWithRecording, "평가 없이 녹음을 보존한 연습 화면으로 돌아와야 한다"),
                    () -> assertEquals(PRACTICES, TestSupportApi.currentPractices(accessToken).remaining(),
                            "평가 기회가 차감되지 않아야 한다"),
                    () -> assertEquals(historyBefore, TestSupportApi.evaluationHistoryCount(accessToken),
                            "새 연습 기록이 생기지 않아야 한다"));
        }
    }
}
