package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 기록(Review) 탭 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class ReviewPage {

    // 앱 review_screen.dart에서 기록이 0건일 때 보이는 안내 제목
    private static final String NO_HISTORY_TITLE = "No practice history yet";
    // 기록 목록 위 제목. 기록을 받아 목록을 그렸다는 신호로 쓴다
    private static final String HISTORY_TITLE = "Recent history";
    // 점수 변화 그래프의 접근성 label 앞부분(review_screen.dart). 진행 요약 카드 label 끝에 합쳐져 보인다
    private static final String PROGRESS_CHART = "Recent scores ";
    // 기록 행 label: "점수\n문장\n로마자\n날짜". 행마다 하나의 요소로 합쳐져 보인다
    private static final Pattern HISTORY_ROW = Pattern.compile("^(\\d+)\\n([^\\n]+)\\n");

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

    /** 기록 목록이 그려질 때까지 기다린다. 기록이 없거나 조회가 실패하면 시간 안에 안 보여 실패한다. */
    public ReviewPage waitUntilHistoryLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.text(HISTORY_TITLE)));
        return this;
    }

    /**
     * 지금 화면에 보이는 기록 행의 문장을 위에서 아래 순서로 돌려준다. 행의 화면 y좌표로 순서를 정한다.
     * 스크롤하지 않으므로 한 화면에 들어오는 몇 건만 비교하는 TC에 쓴다.
     */
    public List<String> historySentencesInOrder() {
        Map<Integer, String> byTop = new TreeMap<>();
        for (WebElement text : driver.findElements(Ui.staticTexts(driver))) {
            Matcher row = HISTORY_ROW.matcher(Ui.labelOf(driver, text));
            if (row.find()) {
                byTop.put(text.getRect().getY(), row.group(2));
            }
        }
        return new ArrayList<>(byTop.values());
    }

    /**
     * 진행 요약 카드의 전체 글자. 화면 글자("YOUR PROGRESS · LAST 3 TRIES", "Latest score", 점수)와
     * 그래프 접근성 label("Recent scores ..., recent average ..., latest score ...")이 줄바꿈으로 합쳐져 있다.
     * 카드가 없으면(기록 0건) 빈 문자열이다.
     */
    public String progressSummary() {
        List<WebElement> cards = driver.findElements(Ui.textContaining(driver, PROGRESS_CHART));
        return cards.isEmpty() ? "" : Ui.labelOf(driver, cards.get(0));
    }

    /** 문장이 sentence인 기록 행을 눌러 기록 상세를 연다. */
    public HistoryDetailPage openHistory(String sentence) {
        // 행 label이 줄바꿈으로 합쳐져 있어 predicate 대신 행 형식을 해석해 문장이 같은 행을 고른다
        wait.until(d -> {
            for (WebElement text : d.findElements(Ui.staticTexts(driver))) {
                Matcher row = HISTORY_ROW.matcher(Ui.labelOf(driver, text));
                if (row.find() && row.group(2).equals(sentence)) {
                    text.click();
                    return true;
                }
            }
            return false;
        });
        return new HistoryDetailPage(driver).waitUntilLoaded();
    }
}
