package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * 약관·개인정보 처리방침 문서 화면. 앱은 backend의 {@code /legal/{terms|privacy}} 페이지를
 * iOS 인앱 Safari 화면(SFSafariViewController)으로 연다. 웹 글자도 iOS 접근성 트리에 보여 화면 글자로 찾는다.
 *
 * <p>닫기는 제공하지 않는다. Safari 화면은 앱 밖 프로세스가 그려 XCUITest의 탭(요소 클릭·좌표 탭)과
 * 왼쪽 가장자리 쓸기가 '완료' 버튼에 전달되지 않는 것을 확인했다(iOS 18.3 시뮬레이터). 문서를 닫아야 하면
 * 앱을 다시 실행한다.</p>
 */
public class LegalDocumentPage {

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public LegalDocumentPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 문서 제목(heading)이 보이는지. 페이지를 받아 그려야 보이므로 시간 안에 기다리고, 안 보이면 false다. */
    public boolean showsHeading(String heading) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(Ui.textContaining(driver, heading)));
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }
}
