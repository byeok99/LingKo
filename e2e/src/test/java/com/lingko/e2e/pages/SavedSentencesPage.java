package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 저장한 문장 목록 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class SavedSentencesPage {

    // 앱 SavedSentencesScreen의 상단 제목과 로딩 중 안내
    private static final String TITLE = "Saved";
    private static final String LOADING = "Loading saved sentences";

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public SavedSentencesPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 제목이 보이고 서버 목록 로딩 안내가 사라지면 도착으로 본다. 로딩 중에 목록을 읽으면 '없음'으로 잘못 판단한다. */
    public SavedSentencesPage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.text(TITLE)));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(Ui.textStartingWith(driver, LOADING)));
        return this;
    }

    /** 저장 목록 카드도 홈과 같은 SentenceCard라 label이 문장으로 시작한다. */
    public boolean showsSentence(String sentence) {
        try {
            Ui.scrollIntoView(driver, Ui.textStartingWith(driver, sentence), Scroll.TOWARD_BOTTOM);
            return true;
        } catch (NoSuchElementException notFound) {
            return false;
        }
    }
}
