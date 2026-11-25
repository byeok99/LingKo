package com.lingko.e2e.support;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * 테스트마다 시뮬레이터에 설치된 E2E 앱으로 Appium 세션을 열고 닫는다.
 *
 * <p>앱은 {@code run-local.sh}에 {@code LINGKO_FLUTTER_TARGET=lib/main_e2e.dart}로 미리 설치해 둔다.
 * 재설치하지 않고(noReset) 그 빌드를 그대로 띄우므로, 앱 코드를 바꾸면 다시 설치해야 반영된다.</p>
 */
public abstract class BaseTest {

    // 환경마다 다를 수 있는 값은 -D 시스템 프로퍼티로 덮어쓴다
    private static final String APPIUM_URL = System.getProperty("appium.url", "http://127.0.0.1:4723");
    private static final String DEVICE_NAME = System.getProperty("ios.device", "iPhone 15 LingKo");
    private static final String PLATFORM_VERSION = System.getProperty("ios.version", "18.3");
    private static final String BUNDLE_ID = "com.byeok.lingko";

    protected AppiumDriver driver;

    @BeforeEach
    void openSession() throws Exception {
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(DEVICE_NAME)
                .setPlatformVersion(PLATFORM_VERSION)
                .setBundleId(BUNDLE_ID)
                .setNoReset(true);
        driver = new IOSDriver(new URL(APPIUM_URL), options);
    }

    @AfterEach
    void closeSession() {
        if (driver != null) {
            driver.quit();
        }
    }
}
