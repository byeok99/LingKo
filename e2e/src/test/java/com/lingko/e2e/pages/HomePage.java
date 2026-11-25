package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 홈 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class HomePage {

    // 화면 문구가 바뀌면 여기만 고친다
    private static final String DAILY_TAB = "Daily";
    private static final String EMPTY_CATEGORY_TITLE = "No %s sentences yet";
    // 앱 _SentenceCategory.label 순서와 같다
    private static final List<String> CATEGORY_TABS =
            List.of("Daily", "Food", "Travel", "Study", "Work", "Health");

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public HomePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 추천 문장을 다 불러와야 탭이 그려지므로 탭이 보이면 도착으로 본다. */
    public HomePage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.text(DAILY_TAB)));
        return this;
    }

    /**
     * 탭 줄은 가로로 스크롤되어 화면 폭에 따라 끝 탭이 밖에 있다.
     * 세로로 탭 줄까지 올라간 뒤, 보이는 아무 탭을 기준으로 그 줄만 가로로 밀어 찾는다.
     */
    public HomePage selectCategory(String tabText) {
        WebElement visibleTab = Ui.scrollIntoView(driver, Ui.anyText(driver, CATEGORY_TABS), Scroll.TOWARD_TOP);
        Ui.swipeRowIntoView(driver, Ui.text(tabText), visibleTab).click();
        return this;
    }

    public boolean showsSentence(String sentence) {
        try {
            Ui.scrollIntoView(driver, Ui.textContaining(driver, sentence), Scroll.TOWARD_BOTTOM);
            return true;
        } catch (NoSuchElementException notFound) {
            return false;
        }
    }

    /**
     * 문장 카드를 눌러 연습 화면으로 간다. 화면을 옮기는 메서드는 다음 Page를 돌려줘 여정 테스트에서 그대로 이어 쓴다.
     * 카테고리 미리보기 개수 안의 문장만 찾는다. 'Show N more'로 숨겨진 문장은 아직 다루지 않는다.
     */
    public PracticePage openSentence(String sentence) {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, sentence), Scroll.TOWARD_BOTTOM).click();
        return new PracticePage(driver).waitUntilLoaded();
    }

    public boolean showsEmptyCategory(String tabText) {
        String title = String.format(EMPTY_CATEGORY_TITLE, tabText);
        return !driver.findElements(Ui.textContaining(driver, title)).isEmpty();
    }
}