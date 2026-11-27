package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 취약 음절 상세 화면(홈 'Your weakest sounds'에서 음절 하나를 연 화면). 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class SoundDetailPage {

    // 요약 줄: "Average 40 across 2 tries". 음절·로마자와 한 label로 합쳐져 보인다
    private static final Pattern AVERAGE = Pattern.compile("Average \\d+ across \\d+ tr(?:y|ies)");
    // 탭 버튼 label(sound_detail_screen.dart): "Practiced 2", "Suggested 1"
    private static final String PRACTICED_TAB = "Practiced ";
    private static final String SUGGESTED_TAB = "Suggested ";

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public SoundDetailPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 서버에서 음절 상세를 받아 탭이 그려지면 도착으로 본다. */
    public SoundDetailPage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.textStartingWith(driver, PRACTICED_TAB)));
        return this;
    }

    /** 요약 줄('Average 40 across 2 tries'). 없으면 빈 문자열이다. */
    public String averageSummary() {
        for (WebElement text : driver.findElements(Ui.textContaining(driver, "Average "))) {
            Matcher matcher = AVERAGE.matcher(Ui.labelOf(driver, text));
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return "";
    }

    /** 'Practiced N' 탭의 N. 이 음절이 들어간 연습 기록 수다. */
    public int practicedCount() {
        return tabCount(PRACTICED_TAB);
    }

    /** 'Suggested N' 탭의 N. 이 음절이 들어간, 아직 연습하지 않은 추천 문장 수다. */
    public int suggestedCount() {
        return tabCount(SUGGESTED_TAB);
    }

    /**
     * 'Suggested' 탭으로 바꾸고 보이는 추천 문장을 위에서 아래 순서로 돌려준다. 행 label은 "문장\n로마자\n번역"이라
     * 첫 줄을 문장으로 읽는다. 스크롤하지 않으므로 한 화면에 들어오는 문장만 돌려준다.
     */
    public List<String> suggestedSentences() {
        driver.findElement(Ui.textStartingWith(driver, SUGGESTED_TAB)).click();
        int tabBottom = driver.findElement(Ui.textStartingWith(driver, SUGGESTED_TAB)).getRect().getY();
        List<WebElement> rows = new ArrayList<>(driver.findElements(Ui.staticTexts(driver)).stream()
                .filter(text -> text.getRect().getY() > tabBottom)
                .filter(text -> Ui.labelOf(driver, text).split("\n").length >= 3)
                .toList());
        rows.sort(Comparator.comparingInt(row -> row.getRect().getY()));
        return rows.stream().map(row -> Ui.labelOf(driver, row).split("\n")[0]).toList();
    }

    /** 'Suggested' 목록에서 문장을 눌러 그 문장의 연습 화면을 연다. {@link #suggestedSentences()} 뒤에 부른다. */
    public PracticePage selectSuggested(String sentence) {
        driver.findElement(Ui.textStartingWith(driver, sentence)).click();
        return new PracticePage(driver);
    }

    private int tabCount(String prefix) {
        String label = Ui.labelOf(driver, driver.findElement(Ui.textStartingWith(driver, prefix)));
        return Integer.parseInt(label.substring(prefix.length()).trim());
    }
}
