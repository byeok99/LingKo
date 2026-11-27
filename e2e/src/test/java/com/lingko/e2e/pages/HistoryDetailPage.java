package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 연습 기록 상세 화면(기록 탭에서 기록 하나를 연 화면). 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class HistoryDetailPage {

    // 단어별 결과 영역 제목. 상세를 다 그렸다는 신호로 쓴다
    private static final String WORDS_TITLE = "Pronunciation by word";
    private static final String STANDARD_PRONUNCIATION = "Standard pronunciation";
    private static final String PRACTICE_AGAIN = "Practice again";
    // 세부 점수 이름. 화면에서 숫자 줄 아래 같은 순서로 놓인다
    private static final List<String> BREAKDOWN_LABELS = List.of("Accuracy", "Fluency", "Completeness");
    // 단어 버튼 label(word_syllable_explorer.dart): "단어, score 65. Show syllables." 또는 "단어, score unavailable. ..."
    private static final Pattern WORD_BUTTON = Pattern.compile("^(.+), score (\\d+|unavailable)\\. Show syllables\\.$");
    private static final Pattern NUMBER = Pattern.compile("^\\d+$");

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public HistoryDetailPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public HistoryDetailPage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.text(WORDS_TITLE)));
        return this;
    }

    /** 표준 발음 제목 바로 아래 줄의 표준 발음. 기록에 저장된 snapshot이 그대로 보인다. */
    public String standardPronunciation() {
        int titleTop = driver.findElement(Ui.text(STANDARD_PRONUNCIATION)).getRect().getY();
        return numbersAndTexts().stream()
                .filter(text -> text.getRect().getY() > titleTop)
                .min(Comparator.comparingInt(text -> text.getRect().getY()))
                .map(text -> Ui.labelOf(driver, text))
                .orElse("");
    }

    /** 화면 맨 위 문장 아래에 크게 보이는 종합 점수. 숫자만 있는 글자 중 가장 위에 있는 것이다. */
    public int overallScore() {
        return numbers().stream()
                .min(Comparator.comparingInt(text -> text.getRect().getY()))
                .map(text -> Integer.parseInt(Ui.labelOf(driver, text)))
                .orElseThrow(() -> new IllegalStateException("종합 점수를 찾지 못했다"));
    }

    /**
     * 세부 점수 이름(Accuracy·Fluency·Completeness)과 그 위에 놓인 숫자를 짝지어 돌려준다.
     * 숫자와 이름이 서로 다른 요소라, 이름 바로 위에서 가로 위치가 겹치는 숫자를 그 이름의 점수로 본다.
     */
    public Map<String, Integer> scoreBreakdown() {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        List<WebElement> numbers = numbers();
        for (String label : BREAKDOWN_LABELS) {
            Rectangle name = driver.findElement(Ui.text(label)).getRect();
            int nameCenter = name.getX() + name.getWidth() / 2;
            numbers.stream()
                    .filter(number -> number.getRect().getY() < name.getY())
                    .filter(number -> Math.abs(center(number) - nameCenter) < name.getWidth())
                    .max(Comparator.comparingInt(number -> number.getRect().getY()))
                    .ifPresent(number -> breakdown.put(label, Integer.parseInt(Ui.labelOf(driver, number))));
        }
        return breakdown;
    }

    /**
     * 'Pronunciation by word'의 단어별 결과를 화면 순서대로 돌려준다. 값은 점수 숫자이거나, 신뢰할 수 없는 점수면
     * "unavailable"이다(화면에는 '—'로 보이고 접근성 label은 'score unavailable'). 단어는 표준 발음 어절이다.
     */
    public Map<String, String> wordScores() {
        Map<String, String> words = new LinkedHashMap<>();
        List<WebElement> buttons = new ArrayList<>(driver.findElements(Ui.textContaining(driver, ". Show syllables.")));
        buttons.sort(Comparator.comparingInt((WebElement b) -> b.getRect().getY()).thenComparingInt(b -> b.getRect().getX()));
        for (WebElement button : buttons) {
            Matcher word = WORD_BUTTON.matcher(Ui.labelOf(driver, button));
            if (word.find()) {
                words.put(word.group(1), word.group(2));
            }
        }
        return words;
    }

    /** 'Practice again'으로 같은 문장의 연습 화면을 연다. 앱은 저장된 snapshot이 아니라 현재 규칙으로 다시 준비한다. */
    public PracticePage practiceAgain() {
        Ui.scrollIntoView(driver, Ui.text(PRACTICE_AGAIN), Ui.Scroll.TOWARD_BOTTOM).click();
        return new PracticePage(driver);
    }

    private List<WebElement> numbers() {
        return numbersAndTexts().stream()
                .filter(text -> NUMBER.matcher(Ui.labelOf(driver, text)).matches())
                .toList();
    }

    private List<WebElement> numbersAndTexts() {
        return driver.findElements(Ui.staticTexts(driver));
    }

    private static int center(WebElement element) {
        Rectangle rect = element.getRect();
        return rect.getX() + rect.getWidth() / 2;
    }
}
