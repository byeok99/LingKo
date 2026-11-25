package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    // 앱 ProgressPanel의 Semantics label과 같다: "Practice energy 3 of 5, 59:41 until refill" 또는 "..., maximum"
    private static final String ENERGY_PREFIX = "Practice energy ";
    private static final Pattern ENERGY_LABEL =
            Pattern.compile("Practice energy (\\d+) of (\\d+), (?:(\\d+):(\\d{2}) until refill|maximum)");

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

    /**
     * 평가 기회 캡슐의 값을 읽는다. 값이 접근성 label 하나에 모여 있어 그 문장을 해석한다.
     *
     * <p>quota 응답이 늦으면 캡슐이 잠시 'Loading practice energy'이고, 실패하면 'Practice energy unavailable'이다.
     * 둘 다 형식에 맞지 않으므로 "N of M" 형식이 나타날 때까지 기다린다.</p>
     */
    public PracticeEnergy practiceEnergy() {
        return wait.until(d -> {
            for (WebElement capsule : d.findElements(Ui.textContaining(driver, ENERGY_PREFIX))) {
                Matcher matcher = ENERGY_LABEL.matcher(Ui.labelOf(driver, capsule));
                if (matcher.find()) {
                    return PracticeEnergy.from(matcher);
                }
            }
            return null;
        });
    }

    /**
     * 홈 캡슐에 보이는 평가 기회.
     *
     * <p>{@code untilRefill}이 null이면 최대치 보유('maximum')라 카운트다운이 없다는 뜻이다.
     * 화면은 초 단위 'mm:ss'로 보여 주므로 비교할 때 1초 안팎의 오차를 감안한다.</p>
     */
    public record PracticeEnergy(int remaining, int limit, Duration untilRefill) {

        static PracticeEnergy from(Matcher matcher) {
            Duration untilRefill = matcher.group(3) == null
                    ? null
                    : Duration.ofMinutes(Long.parseLong(matcher.group(3)))
                            .plusSeconds(Long.parseLong(matcher.group(4)));
            return new PracticeEnergy(
                    Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), untilRefill);
        }
    }
}