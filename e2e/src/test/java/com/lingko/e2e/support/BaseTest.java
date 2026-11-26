package com.lingko.e2e.support;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;

/**
 * 테스트마다 시뮬레이터에 설치된 E2E 앱으로 Appium 세션을 열고 닫는다.
 *
 * <p>앱은 {@code run-local.sh}에 {@code LINGKO_FLUTTER_TARGET=lib/main_e2e.dart}로 미리 설치해 둔다.
 * 재설치하지 않고(noReset) 그 빌드를 그대로 띄우므로, 앱 코드를 바꾸면 다시 설치해야 반영된다.</p>
 *
 * <p>세션은 {@link #driver()}를 처음 부를 때 연다. JUnit은 부모의 {@code @BeforeEach}를 자식보다 먼저 돌리므로,
 * 여기서 세션을 열면 TC별 사전조건 hook(화면 테스트의 {@code @Nested} 클래스 {@code @BeforeEach})보다 앱이 먼저 떠
 * 바꾼 서버 상태를 읽지 못한다. 지연 실행으로 "사전조건 hook → 앱 실행 → 검증" 순서를 보장한다.</p>
 */
public abstract class BaseTest {

    // 환경마다 다를 수 있는 값은 -D 시스템 프로퍼티로 덮어쓴다
    private static final String APPIUM_URL = System.getProperty("appium.url", "http://127.0.0.1:4723");
    private static final String DEVICE_NAME = System.getProperty("ios.device", "iPhone 15 LingKo");
    private static final String PLATFORM_VERSION = System.getProperty("ios.version", "18.3");
    // Simulator가 같은 앱의 권한을 바꿀 때도 쓴다
    static final String BUNDLE_ID = "com.byeok.lingko";

    // null이면 이 테스트에서 아직 앱을 띄우지 않았다는 뜻이다
    private AppiumDriver driver;

    /** 이 테스트의 앱 세션. 처음 부를 때 앱을 새로 실행하므로 사전조건을 모두 맞춘 뒤에 부른다. */
    protected AppiumDriver driver() {
        if (driver == null) {
            driver = openSession();
        }
        return driver;
    }

    /**
     * 같은 세션에서 앱을 완전히 종료한 뒤 다시 실행한다. "재실행해도 상태가 유지된다" 같은 TC에서 쓴다.
     * 재실행한 앱은 서버에서 데이터를 다시 받으므로, 화면 상태가 아니라 서버·기기 저장 상태를 확인하게 된다.
     */
    protected void relaunchApp() {
        InteractsWithApps apps = (InteractsWithApps) driver();
        apps.terminateApp(BUNDLE_ID);
        apps.activateApp(BUNDLE_ID);
    }

    @AfterEach
    void closeSession() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    private static AppiumDriver openSession() {
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(DEVICE_NAME)
                .setPlatformVersion(PLATFORM_VERSION)
                .setBundleId(BUNDLE_ID)
                .setNoReset(true)
                // noReset이면 기본값이 false라 앞 테스트가 끝낸 화면을 그대로 이어받는다. 테스트가 서버 상태를 바꾼 뒤
                // 화면이 그 값을 다시 읽고 항상 홈에서 시작하도록 세션마다 앱을 새로 실행한다(앱 데이터는 noReset으로 유지)
                .setForceAppLaunch(true);
        try {
            return new IOSDriver(new URL(APPIUM_URL), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("appium.url 형식이 잘못됐다: " + APPIUM_URL, e);
        }
    }
}
