package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 프로필 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class ProfilePage {

    // 앱 ProfileScreen의 'Your content' 섹션 메뉴 글자
    private static final String SAVED_SENTENCES_MENU = "Saved sentences";

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public ProfilePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 저장 문장 메뉴가 보이면 도착으로 본다. TC가 이 메뉴를 쓰므로 가장 직접적인 신호다. */
    public ProfilePage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                Ui.textStartingWith(driver, SAVED_SENTENCES_MENU)));
        return this;
    }

    public SavedSentencesPage openSavedSentences() {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, SAVED_SENTENCES_MENU), Scroll.TOWARD_BOTTOM).click();
        return new SavedSentencesPage(driver).waitUntilLoaded();
    }
}
