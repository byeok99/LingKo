package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * AI 처리(Microsoft Azure 전송) 동의 화면. 동의하지 않은 사용자가 평가를 요청하면 전송 전에 뜬다.
 * 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다.
 */
public class AiConsentPage {

    // 앱 ai_processing_consent_screen.dart의 AppBar 제목
    private static final String SCREEN_TITLE = "AI assessment privacy";
    // 동의하지 않은 상태에서 보이는 거절 버튼. 이미 동의한 상태라면 같은 자리에 'Close'가 보인다
    private static final String DECLINE_BUTTON = "Not now — do not send";
    // 이미 동의한 상태에서만 보이는 철회 버튼
    private static final String WITHDRAW_BUTTON = "Withdraw permission";

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public AiConsentPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 앱은 서버에서 동의 상태를 조회한 뒤 화면을 띄우므로 제목이 보일 때까지 기다린다. */
    public AiConsentPage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(Ui.text(SCREEN_TITLE)));
        return this;
    }

    /**
     * 이미 동의한 상태에서 'Withdraw permission'으로 동의를 철회한다. 앱은 서버에 철회를 저장한 뒤 화면을 닫고
     * 이 화면을 연 곳(프로필)으로 돌아간다. 저장에 실패하면 화면에 머물며 오류 안내를 띄운다.
     */
    public void withdrawPermission() {
        Ui.scrollIntoView(driver, Ui.text(WITHDRAW_BUTTON), Ui.Scroll.TOWARD_BOTTOM).click();
        // 화면 제목은 프로필 메뉴 글자와 같아 닫힘 판단에 쓸 수 없다. 이 화면에만 있는 철회 버튼이 사라지면 닫힌 것이다
        wait.until(ExpectedConditions.invisibilityOfElementLocated(Ui.text(WITHDRAW_BUTTON)));
    }

    /** 거절 버튼은 화면 아래쪽에 있어 작은 기기에서는 스크롤해야 보인다. 누르면 앱은 연습 화면으로 돌아간다. */
    public void declineSending() {
        Ui.scrollIntoView(driver, Ui.text(DECLINE_BUTTON), Ui.Scroll.TOWARD_BOTTOM).click();
    }
}
