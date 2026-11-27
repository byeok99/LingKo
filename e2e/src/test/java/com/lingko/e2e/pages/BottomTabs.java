package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 모든 메인 화면 아래에 공통으로 있는 하단 탭. 화면을 옮기므로 도착한 화면의 Page를 돌려준다. */
public final class BottomTabs {

    private BottomTabs() {
    }

    /**
     * 홈 탭. 다른 화면(동의 화면 등)을 닫은 직후에는 하단 탭이 접근성 트리에 다시 나타나기까지 잠깐 걸려,
     * 바로 찾지 않고 누를 수 있을 때까지 기다린다.
     */
    public static HomePage openHome(AppiumDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(Ui.bottomTab(driver, "Home")))
                .click();
        return new HomePage(driver).waitUntilLoaded();
    }

    /** 기록 탭. 도착 여부는 화면 상태(기록 있음·없음·오류)마다 달라 기다리지 않고, 확인은 ReviewPage 메서드가 기다려서 한다. */
    public static ReviewPage openReview(AppiumDriver driver) {
        driver.findElement(Ui.bottomTab(driver, "Review")).click();
        return new ReviewPage(driver);
    }

    public static ProfilePage openProfile(AppiumDriver driver) {
        driver.findElement(Ui.bottomTab(driver, "Profile")).click();
        return new ProfilePage(driver).waitUntilLoaded();
    }
}
