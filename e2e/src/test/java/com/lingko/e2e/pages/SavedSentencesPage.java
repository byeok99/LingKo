package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
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

    /** 목록 위의 개수 문구('1 sentence', '2 sentences')가 count와 맞는지. 앱은 실제 목록 길이로 센다. */
    public boolean showsCount(int count) {
        String label = count == 1 ? "1 sentence" : count + " sentences";
        return !driver.findElements(Ui.text(label)).isEmpty();
    }

    /** 분류 필터 칩('All', 'Daily', 'Travel', 'My own')을 누른다. 필터는 화면 안에서만 거르고 서버를 다시 부르지 않는다. */
    public SavedSentencesPage selectFilter(String filter) {
        wait.until(ExpectedConditions.elementToBeClickable(Ui.text(filter))).click();
        return this;
    }

    /**
     * candidates 중 지금 화면에 보이는 문장 카드를 위에서 아래 순서로 돌려준다. 카드의 화면 y좌표로 순서를 정한다.
     * 스크롤하지 않으므로 목록이 한 화면에 들어오는 짧은 경우에만 쓴다.
     */
    public List<String> visibleSentencesInOrder(List<String> candidates) {
        Map<Integer, String> byTop = new TreeMap<>();
        for (String sentence : candidates) {
            List<WebElement> cards = driver.findElements(Ui.textStartingWith(driver, sentence));
            if (!cards.isEmpty()) {
                byTop.put(cards.get(0).getRect().getY(), sentence);
            }
        }
        return new ArrayList<>(byTop.values());
    }

    /** 고른 필터에 맞는 문장이 없을 때의 안내('No My own sentences' 등)가 보이는지. */
    public boolean showsEmptyFilter(String filter) {
        return !driver.findElements(Ui.textContaining(driver, "No " + filter + " sentences")).isEmpty();
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
