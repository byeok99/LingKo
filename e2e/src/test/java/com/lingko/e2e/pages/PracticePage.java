package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
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
     */
    public PracticePage typeSentence(String sentence) {
        // 앞 단계에서 녹음 버튼까지 내려갔으면 입력칸이 화면 위로 밀려나 입력이 닿지 않으므로 맨 위로 올린다
        Ui.scrollIntoView(driver, Ui.text(SCREEN_TITLE), Ui.Scroll.TOWARD_TOP);
        WebElement input = driver.findElement(Ui.textInput(driver));
        input.click();
        input.clear();
        input.sendKeys(sentence);
        // 입력이 조용히 무시되면 뒤의 검증이 엉뚱한 이유로 실패하므로, 여기서 입력 결과를 먼저 확인한다
        String typed = driver.findElement(Ui.textInput(driver)).getText();
        if (!sentence.equals(typed)) {
            throw new IllegalStateException("입력칸에 문장이 그대로 들어가지 않았다. 기대 " + sentence.length()
                    + "자, 실제 " + (typed == null ? 0 : typed.length()) + "자: " + typed);
        }
        return this;
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
        } catch (org.openqa.selenium.TimeoutException notShown) {
            return false;
        }
    }

    /** 표준 발음 영역이 지금 있는지. 준비되지 않은 문장은 표준 발음이 없어야 하므로 기다리지 않고 현재 상태만 본다. */
    public boolean showsStandardPronunciation() {
        return !driver.findElements(Ui.textContaining(driver, STANDARD_PRONUNCIATION)).isEmpty();
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
