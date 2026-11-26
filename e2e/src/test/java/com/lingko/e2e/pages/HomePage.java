package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
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
    private static final String CUSTOM_PRACTICE = "Practice my own sentence";
    // 앱 SentenceCard _BookmarkButton의 Semantics label과 같다
    private static final String SAVED_BOOKMARK = "Saved. Tap to remove";
    private static final List<String> BOOKMARK_LABELS = List.of("Save this sentence", SAVED_BOOKMARK);

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

    /** 'Practice my own sentence'로 빈 입력칸의 연습 화면을 연다. */
    public PracticePage openCustomPractice() {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, CUSTOM_PRACTICE), Scroll.TOWARD_BOTTOM).click();
        return new PracticePage(driver).waitUntilInputReady();
    }

    /** 문장 카드의 북마크가 저장 상태('Saved. Tap to remove')인지. */
    public boolean isSaved(String sentence) {
        return SAVED_BOOKMARK.equals(Ui.labelOf(driver, bookmarkOf(sentence)));
    }

    /**
     * 문장 카드의 북마크를 눌러 저장 상태를 뒤집고, 화면 표시가 바뀔 때까지 기다린다.
     * 앱은 화면부터 바꾸고 서버에 요청하므로, 이 메서드가 끝나도 서버 반영은 따로 확인해야 한다.
     */
    public HomePage toggleSaved(String sentence) {
        boolean before = isSaved(sentence);
        bookmarkOf(sentence).click();
        wait.until(d -> isSaved(sentence) != before);
        return this;
    }

    /**
     * 카드마다 같은 label의 북마크가 있어 글자로는 하나를 고를 수 없다.
     * 대상 카드와 세로 중심이 가장 가까운 북마크를 그 카드의 것으로 본다(북마크는 카드 행 오른쪽에 같은 높이로 있다).
     */
    private WebElement bookmarkOf(String sentence) {
        WebElement card = Ui.scrollIntoView(driver, Ui.textStartingWith(driver, sentence), Scroll.TOWARD_BOTTOM);
        int cardCenterY = centerY(card);
        return driver.findElements(Ui.anyText(driver, BOOKMARK_LABELS)).stream()
                .min(Comparator.comparingInt(bookmark -> Math.abs(centerY(bookmark) - cardCenterY)))
                .orElseThrow(() -> new NoSuchElementException("문장 카드의 북마크를 찾지 못함: " + sentence));
    }

    private static int centerY(WebElement element) {
        Rectangle rect = element.getRect();
        return rect.getY() + rect.getHeight() / 2;
    }

    /**
     * 문장 카드가 지금 목록에 있는지 스크롤 없이 확인한다. "다른 상황의 문장이 보이지 않는다"처럼 없음을 확인할 때 쓴다.
     * 홈은 선택한 상황의 카드만 그리므로, 트리에 없으면 화면 어디에도 없다. 있음 확인은 스크롤하는 {@link #showsSentence}를 쓴다.
     */
    public boolean hasSentenceCard(String sentence) {
        return !driver.findElements(Ui.textStartingWith(driver, sentence)).isEmpty();
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
     * 캡슐의 카운트다운이 {@code before}보다 작아질 때까지 기다린다. 시간 안에 줄지 않으면(멈춘 카운트다운) 실패한다.
     * 화면 값만 다시 읽으므로 서버를 다시 조회하지 않아도 앱의 1초 타이머가 동작하는지 확인할 수 있다.
     */
    public PracticeEnergy waitUntilCountdownBelow(Duration before, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(d -> {
            PracticeEnergy now = practiceEnergy();
            return now.untilRefill() != null && now.untilRefill().compareTo(before) < 0 ? now : null;
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