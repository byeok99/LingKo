package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 연습 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class PracticePage {

    // 화면 맨 위 제목. 입력칸을 화면 안으로 가져올 때 스크롤 기준으로 쓴다
    private static final String SCREEN_TITLE = "Practice";
    // 평가 기회가 0이면 같은 버튼이 'No practices left'로 바뀌므로, 이 글자를 기다리는 테스트는 기회가 있어야 한다
    private static final String RECORD_BUTTON = "Record";
    // 준비가 끝나면 입력칸 아래에 표준 발음 영역이 생긴다. 접근성 트리에서는 입력칸 label에 합쳐져 보인다
    private static final String STANDARD_PRONUNCIATION = "STANDARD PRONUNCIATION";
    // 앱 practice_screen.dart의 자유 문장 길이 초과 안내 앞부분
    private static final String LENGTH_LIMIT_MESSAGE = "Sentences can be up to 100 characters";
    // 앱 practice_screen.dart의 서버 준비 실패 안내 앞부분
    private static final String PREPARE_FAILURE_MESSAGE = "We could not prepare this sentence";
    // 앱 준비 지연 0.7초에 요청·화면 갱신 여유를 더한 값
    private static final Duration PREPARATION_DELAY_MARGIN = Duration.ofMillis(2000);

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public PracticePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 문장의 발음 준비가 끝나야 녹음 버튼이 그려지므로 버튼이 보이면 도착으로 본다. */
    public PracticePage waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(Ui.text(RECORD_BUTTON)));
        return this;
    }

    /** 자유 문장으로 들어오면 아직 준비된 문장이 없어 녹음 버튼이 없다. 입력칸이 보이면 도착으로 본다. */
    public PracticePage waitUntilInputReady() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(Ui.textInput(driver)));
        return this;
    }

    /**
     * 입력칸을 비우고 문장을 입력한다. 앱은 입력이 0.7초 멈추면 서버에 발음 준비를 요청하므로,
     * 결과 확인은 {@link #waitUntilPrepared()}나 {@link #showsLengthLimitMessage()}로 기다려서 한다.
     * 앱이 지우지 않는 문장(한글·공백)만 넣는다. 입력이 조용히 무시되면 뒤 검증이 엉뚱한 이유로 실패하므로 결과를 바로 확인한다.
     */
    public PracticePage typeSentence(String sentence) {
        String typed = typeRaw(sentence).inputText();
        if (!sentence.equals(typed)) {
            throw new IllegalStateException("입력칸에 문장이 그대로 들어가지 않았다. 기대 " + sentence.length()
                    + "자, 실제 " + typed.length() + "자: " + typed);
        }
        return this;
    }

    /**
     * 입력칸을 비우고 글자를 넣되 결과는 확인하지 않는다. 문장부호처럼 앱이 입력 즉시 지우는 글자를 넣을 때 쓰고,
     * 남은 내용은 {@link #inputText()}로 따로 확인한다.
     */
    public PracticePage typeRaw(String text) {
        // 앞 단계에서 녹음 버튼까지 내려갔으면 입력칸이 화면 위로 밀려나 입력이 닿지 않으므로 맨 위로 올린다
        Ui.scrollIntoView(driver, Ui.text(SCREEN_TITLE), Ui.Scroll.TOWARD_TOP);
        WebElement input = driver.findElement(Ui.textInput(driver));
        input.click();
        input.clear();
        input.sendKeys(text);
        return this;
    }

    /** 입력칸에 지금 남아 있는 글자. 비어 있으면 빈 문자열이다(placeholder는 포함하지 않는다). */
    public String inputText() {
        return Ui.inputValue(driver, driver.findElement(Ui.textInput(driver)));
    }

    /**
     * 앱이 발음 준비를 요청하는 지연(0.7초)이 확실히 지날 때까지 기다린다.
     * "준비가 시작되지 않는다"처럼 무언가가 나타나지 않음을 확인하려면 조건으로 기다릴 수 없어 고정 시간을 둔다.
     */
    public PracticePage waitPastPreparationDelay() {
        try {
            Thread.sleep(PREPARATION_DELAY_MARGIN.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("준비 지연 대기가 중단됐다", e);
        }
        return this;
    }

    /** 입력 관련 오류 안내(준비 실패, 길이 초과)가 지금 보이는지. 기다리지 않고 현재 상태만 본다. */
    public boolean showsAnyInputError() {
        return !driver.findElements(Ui.textContaining(driver, PREPARE_FAILURE_MESSAGE)).isEmpty()
                || !driver.findElements(Ui.textContaining(driver, LENGTH_LIMIT_MESSAGE)).isEmpty();
    }

    /**
     * 서버가 표준 발음을 준비해 녹음할 수 있는 상태가 될 때까지 기다린다. 시간 안에 안 되면 실패한다.
     * 긴 문장은 입력칸·표준 발음 영역이 커지고 키보드도 떠 있어 녹음 버튼이 화면 밖에 있으므로 버튼까지 스크롤한다.
     * Flutter는 화면 밖 요소를 접근성 트리에서 빼므로, 스크롤하기 전에 버튼 존재를 기다리면 찾지 못한다.
     * Appium hideKeyboard는 쓰지 않는다. 키보드를 내리면 Flutter 입력칸 포커스 연결이 끊겨 다시 눌러도
     * 입력·지우기가 무시되는 것을 확인했다(자유 문장 100자 TC 작업 중). 키보드를 둔 채 스크롤해도 버튼까지 닿는다.
     */
    public PracticePage waitUntilPrepared() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.textContaining(driver, STANDARD_PRONUNCIATION)));
        Ui.scrollIntoView(driver, Ui.text(RECORD_BUTTON), Ui.Scroll.TOWARD_BOTTOM);
        return this;
    }

    /** 길이 초과 안내가 나타나는지. 앱은 이 경우 서버에 보내지 않으므로 바로 표시된다. */
    public boolean showsLengthLimitMessage() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(Ui.textContaining(driver, LENGTH_LIMIT_MESSAGE)));
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }

    /** 표준 발음 영역이 지금 있는지. 준비되지 않은 문장은 표준 발음이 없어야 하므로 기다리지 않고 현재 상태만 본다. */
    public boolean showsStandardPronunciation() {
        return !driver.findElements(Ui.textContaining(driver, STANDARD_PRONUNCIATION)).isEmpty();
    }

    /** 표준 발음 영역에 보이는 발음 글자. 영역이 없으면 빈 문자열이다. */
    public String standardPronunciation() {
        return pronunciationCardLine(1);
    }

    /**
     * 표준 발음 영역에서 발음 바로 아래 줄에 보이는 로마자 발음. 영역이 없으면 빈 문자열이다.
     * 표준 발음과 같은 준비 결과로 함께 그려지므로, {@link #waitForStandardPronunciation(String)} 뒤에 읽으면 같은 문장의 값이다.
     * 서버가 로마자를 주지 않으면 앱이 이 줄을 그리지 않아 다른 글자가 올 수 있으니, 호출자는 기대값과 정확히 비교한다.
     */
    public String romanizedPronunciation() {
        return pronunciationCardLine(2);
    }

    /**
     * 표준 발음 영역 제목에서 offset만큼 아래 줄. 줄이 없으면 빈 문자열이다.
     * 접근성 트리에서 이 영역은 다른 글자와 한 label로 합쳐지고 줄바꿈으로 구분되므로, 제목 줄을 기준으로 위치를 센다.
     */
    private String pronunciationCardLine(int offset) {
        for (WebElement element : driver.findElements(Ui.textContaining(driver, STANDARD_PRONUNCIATION))) {
            List<String> lines = List.of(Ui.labelOf(driver, element).split("\n"));
            int title = lines.indexOf(STANDARD_PRONUNCIATION);
            if (title >= 0 && title + offset < lines.size()) {
                return lines.get(title + offset).trim();
            }
        }
        return "";
    }

    /**
     * 표준 발음이 expected가 될 때까지 기다린 뒤 화면 값을 돌려준다. 시간 안에 안 바뀌면 마지막으로 본 값을 돌려주므로
     * 호출자는 반환값을 expected와 비교해 판정하고, 실패 메시지에 실제 값을 남긴다.
     * 같은 화면에서 문장을 바꾸면 새 결과가 올 때까지 앞 문장 발음이 남아 있어, 영역이 보이는 것만으로는 새 결과인지 알 수 없다.
     */
    public String waitForStandardPronunciation(String expected) {
        try {
            wait.until(d -> {
                try {
                    return expected.equals(standardPronunciation());
                } catch (StaleElementReferenceException redrawn) {
                    // 결과가 바뀌며 영역을 다시 그리는 중이다. 다음 확인에서 새 요소를 찾는다
                    return false;
                }
            });
        } catch (TimeoutException notChanged) {
            // 판정은 호출자의 assert가 맡는다
        }
        return standardPronunciation();
    }

    /** 녹음 버튼이 지금 있는지. 준비되지 않은 문장은 녹음할 수 없어야 하므로 기다리지 않고 현재 상태만 본다. */
    public boolean canRecord() {
        return !driver.findElements(Ui.text(RECORD_BUTTON)).isEmpty();
    }

    /** 연습 문장은 화면 글자가 아니라 입력칸 값으로 보인다. 앱이 문장부호·기호를 빼고 넣는다는 점에 주의한다. */
    public boolean showsSentence(String sentence) {
        return !driver.findElements(Ui.inputContaining(driver, sentence)).isEmpty();
    }
}
