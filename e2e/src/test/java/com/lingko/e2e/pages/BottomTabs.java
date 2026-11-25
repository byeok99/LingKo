package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;

/** 모든 메인 화면 아래에 공통으로 있는 하단 탭. 화면을 옮기므로 도착한 화면의 Page를 돌려준다. */
public final class BottomTabs {

    private BottomTabs() {
    }

    public static ProfilePage openProfile(AppiumDriver driver) {
        driver.findElement(Ui.bottomTab(driver, "Profile")).click();
        return new ProfilePage(driver).waitUntilLoaded();
    }
}
