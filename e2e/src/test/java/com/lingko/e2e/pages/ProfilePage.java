package com.lingko.e2e.pages;

import com.lingko.e2e.support.Ui;
import com.lingko.e2e.support.Ui.Scroll;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 프로필 화면. 화면 글자와 요소 찾는 방법은 이 클래스 안에만 둔다. */
public class ProfilePage {

    // 앱 ProfileScreen의 'Your content' 섹션 메뉴 글자
    private static final String SAVED_SENTENCES_MENU = "Saved sentences";
    // 앱 ProfileScreen의 AI 처리 동의 관리 메뉴 글자
    private static final String AI_CONSENT_MENU = "AI assessment privacy";
    // UMP 광고 동의 대상 지역에서만 보이는 메뉴
    private static final String ADVERTISING_PRIVACY_MENU = "Advertising privacy";
    // 앱 InlineBannerAd의 Semantics label
    private static final String BANNER_AD = "Advertisement";

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    public ProfilePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /** 저장 문장 메뉴가 보이면 도착으로 본다. TC가 이 메뉴를 쓰므로 가장 직접적인 신호다. */
    public ProfilePage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                Ui.textStartingWith(driver, SAVED_SENTENCES_MENU)));
        return this;
    }

    public SavedSentencesPage openSavedSentences() {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, SAVED_SENTENCES_MENU), Scroll.TOWARD_BOTTOM).click();
        return new SavedSentencesPage(driver).waitUntilLoaded();
    }

    /** 프로필 머리의 계정 이름이 보이는지. 이름은 서버 사용자 정보에서 온다. */
    public boolean showsAccountName(String name) {
        return !driver.findElements(Ui.textStartingWith(driver, name)).isEmpty();
    }

    /**
     * menus를 화면 위에서 아래 순서로 넘겨받아, 아래로 스크롤하며 각각 보이는지 확인하고 보인 메뉴만 돌려준다.
     * Flutter는 화면 밖 요소를 접근성 트리에서 빼므로 한 번에 찾지 않고 순서대로 내려가며 찾는다.
     * 순서가 실제 배치와 다르면 이미 지나친 메뉴를 못 찾으므로 호출자는 화면 순서대로 넘긴다.
     */
    public List<String> visibleMenusInOrder(List<String> menus) {
        List<String> shown = new ArrayList<>();
        for (String menu : menus) {
            try {
                Ui.scrollIntoView(driver, Ui.textStartingWith(driver, menu), Scroll.TOWARD_BOTTOM);
                shown.add(menu);
            } catch (NoSuchElementException notFound) {
                // 없는 메뉴는 결과에서 빠지고, 판정은 호출자가 기대 목록과 비교해 한다
            }
        }
        return shown;
    }

    /**
     * 배너 광고가 보이는지. 앱 InlineBannerAd는 광고를 받아 그리면 'Advertisement' Semantics로 감싼다.
     * 테스트 광고를 네트워크로 받아야 하므로 시간 안에 기다리고, 안 보이면 false다.
     */
    public boolean showsBannerAd() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(Ui.text(BANNER_AD)));
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }

    /**
     * 'Advertising privacy' 메뉴가 settle만큼 기다린 뒤에도 없는지. 앱은 프로필을 연 뒤 UMP에 비동기로 물어 대상 지역일 때만
     * 메뉴를 추가하는데, 조회가 끝났다는 신호가 화면에 없어 고정 시간을 둔다. 메뉴는 'AI assessment privacy' 바로 아래라
     * 그 메뉴가 보이도록 스크롤한 뒤 확인한다.
     */
    public boolean advertisingPrivacyHiddenAfter(Duration settle) {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, AI_CONSENT_MENU), Scroll.TOWARD_BOTTOM);
        try {
            Thread.sleep(settle.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("UMP 조회 대기가 중단됐다", e);
        }
        return driver.findElements(Ui.textStartingWith(driver, ADVERTISING_PRIVACY_MENU)).isEmpty();
    }

    /** 'AI assessment privacy'로 AI 처리 동의 관리 화면을 연다. 앱은 서버에서 현재 상태를 조회한 뒤 화면을 띄운다. */
    public AiConsentPage openAiConsent() {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, AI_CONSENT_MENU), Scroll.TOWARD_BOTTOM).click();
        return new AiConsentPage(driver).waitUntilLoaded();
    }

    /** 문서 메뉴('Terms of Service', 'Privacy Policy')를 눌러 앱 안의 문서 화면을 연다. */
    public LegalDocumentPage openLegalDocument(String menu) {
        Ui.scrollIntoView(driver, Ui.textStartingWith(driver, menu), Scroll.TOWARD_BOTTOM).click();
        return new LegalDocumentPage(driver);
    }
}
