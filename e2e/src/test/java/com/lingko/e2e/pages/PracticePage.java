package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 연습 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class PracticePage {

    // 평가 기회가 0이면 같은 버튼이 'No practices left'로 바뀌므로, 이 글자를 기다리는 테스트는 기회가 있어야 한다
    private static final String RECORD_BUTTON = "Record";

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

    /** 연습 문장은 화면 글자가 아니라 입력칸 값으로 보인다. 앱이 문장부호·기호를 빼고 넣는다는 점에 주의한다. */
    public boolean showsSentence(String sentence) {
        return !driver.findElements(Ui.inputContaining(driver, sentence)).isEmpty();
    }
}
