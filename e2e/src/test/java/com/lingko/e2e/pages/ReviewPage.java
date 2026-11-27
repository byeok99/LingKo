package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 기록(Review) 탭 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class ReviewPage {

    // 앱 review_screen.dart에서 기록이 0건일 때 보이는 안내 제목
    private static final String NO_HISTORY_TITLE = "No practice history yet";

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public ReviewPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /**
     * 첫 연습 안내가 나타나는지. 앱은 기록을 서버에서 받은 뒤에 이 안내를 그리므로 시간 안에 기다리고,
     * 안 보이면 false다. 기록이 있거나 조회가 실패하면('History is unavailable') 다른 화면이 보인다.
     */
    public boolean showsNoHistoryGuide() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(Ui.textContaining(driver, NO_HISTORY_TITLE)));
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }
}
