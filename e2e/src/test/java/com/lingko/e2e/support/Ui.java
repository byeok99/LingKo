package com.lingko.e2e.support;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

/** 화면 글자로 요소를 찾고 스크롤한다. OS별 차이는 이 클래스 안에서만 처리한다. */
public final class Ui {

    public enum Scroll { TOWARD_BOTTOM, TOWARD_TOP }

    private static final int MAX_SWIPES = 5;

    private Ui() {
    }

    /** 화면 글자와 정확히 같은 요소. iOS name/label, Android content-desc에 맞춰진다. */
    public static By text(String exact) {
        return AppiumBy.accessibilityId(exact);
    }

    /** 글자가 합쳐진 요소(문장 카드 등)를 일부 글자로 찾는다. */
    public static By textContaining(AppiumDriver driver, String part) {
        if (isIos(driver)) {
            return AppiumBy.iOSNsPredicateString("label CONTAINS " + predicateQuote(part));
        }
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(" + javaQuote(part) + ")");
    }

    /**
     * 합쳐진 글자가 part로 시작하는 요소. 문장 카드처럼 label이 "문장 + 로마자 + 번역"인 요소를 문장으로 고를 때 쓴다.
     * CONTAINS로 찾으면 짧은 문장이 다른 카드 문장 안에 들어 있을 때(예: "물"과 "물 한 잔 주세요") 엉뚱한 카드를 집는다.
     */
    public static By textStartingWith(AppiumDriver driver, String part) {
        if (isIos(driver)) {
            return AppiumBy.iOSNsPredicateString("label BEGINSWITH " + predicateQuote(part));
        }
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionStartsWith(" + javaQuote(part) + ")");
    }

    /** 화면의 글자 입력칸. 연습 화면처럼 입력칸이 하나뿐인 화면에서 쓴다. */
    public static By textInput(AppiumDriver driver) {
        if (isIos(driver)) {
            return AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'");
        }
        return AppiumBy.className("android.widget.EditText");
    }

    /** 입력칸 안의 글자는 label이 아니라 iOS는 value, Android는 text에 들어간다. */
    public static By inputContaining(AppiumDriver driver, String part) {
        if (isIos(driver)) {
            return AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView')"
                            + " AND value CONTAINS " + predicateQuote(part));
        }
        return AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\").textContains("
                        + javaQuote(part) + ")");
    }

    /** 화면에 보일 때까지 정해진 방향으로 스크롤한다. */
    public static WebElement scrollIntoView(AppiumDriver driver, By locator, Scroll direction) {
        for (int i = 0; i <= MAX_SWIPES; i++) {
            List<WebElement> found = driver.findElements(locator);
            if (!found.isEmpty() && found.get(0).isDisplayed()) {
                return found.get(0);
            }
            swipe(driver, direction);
        }
        throw new NoSuchElementException("스크롤해도 찾지 못함: " + locator);
    }

    /** 화면 글자가 여러 후보 중 하나와 정확히 같은 요소. 가로 목록의 기준점을 잡을 때 쓴다. */
    public static By anyText(AppiumDriver driver, List<String> exacts) {
        if (isIos(driver)) {
            String names = String.join(", ", exacts.stream().map(Ui::predicateQuote).toList());
            return AppiumBy.iOSNsPredicateString("name IN {" + names + "}");
        }
        String regex = "^(" + String.join("|", exacts.stream().map(Pattern::quote).toList()) + ")$";
        return AppiumBy.androidUIAutomator("new UiSelector().descriptionMatches(" + javaQuote(regex) + ")");
    }

    /**
     * 하단 탭. Flutter가 탭 label 뒤에 "Tab 4 of 4" 같은 위치 정보를 붙이므로("Profile\nTab 4 of 4"),
     * 탭 이름으로 시작하면서 탭 표시가 있는 요소로 찾는다. 같은 글자로 시작하는 화면 제목과 섞이지 않게 하기 위해서다.
     */
    public static By bottomTab(AppiumDriver driver, String tabName) {
        if (isIos(driver)) {
            return AppiumBy.iOSNsPredicateString(
                    "label BEGINSWITH " + predicateQuote(tabName) + " AND label CONTAINS 'Tab '");
        }
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionMatches(" + javaQuote("^" + Pattern.quote(tabName) + "\\s+Tab .*") + ")");
    }

    /**
     * 입력칸에 실제로 들어 있는 글자. 비어 있으면 빈 문자열이다.
     * iOS는 비어 있을 때 {@code getText()}가 placeholder와 label을 돌려줘 "비어 있음"을 판단할 수 없으므로
     * {@code value} 속성을 읽는다(비면 null). Android는 EditText의 text를 읽는다.
     */
    public static String inputValue(AppiumDriver driver, WebElement input) {
        String value = isIos(driver) ? input.getAttribute("value") : input.getText();
        return value == null ? "" : value;
    }

    /**
     * 화면의 모든 글자 요소. 점수 숫자처럼 label 하나만으로는 어느 항목인지 알 수 없어, 위치(좌표)로 짝을 지어야 할 때 쓴다.
     * Flutter가 Semantics로 묶은 영역은 여러 글자가 한 요소 label에 줄바꿈으로 합쳐져 나온다.
     */
    public static By staticTexts(AppiumDriver driver) {
        if (isIos(driver)) {
            return AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'");
        }
        return AppiumBy.className("android.widget.TextView");
    }

    /** 요소의 화면 글자(접근성 label). iOS는 label, Android는 content-desc 속성에 들어 있다. */
    public static String labelOf(AppiumDriver driver, WebElement element) {
        String label = element.getAttribute(isIos(driver) ? "label" : "content-desc");
        return label == null ? "" : label;
    }

    /**
     * 가로로 스크롤되는 줄(카테고리 탭 등)에서 target이 보일 때까지 그 줄만 민다.
     * 화면 전체를 미는 swipe는 세로 목록이 먼저 반응하므로 rowItem의 높이에서만 끌어 준다.
     * 끝 방향으로 먼저 밀고, 못 찾으면 처음 방향으로 되돌아오며 찾는다.
     */
    public static WebElement swipeRowIntoView(AppiumDriver driver, By target, WebElement rowItem) {
        Rectangle row = rowItem.getRect();
        int y = row.getY() + row.getHeight() / 2;
        int width = driver.manage().window().getSize().width;
        for (int i = 0; i <= MAX_SWIPES * 2; i++) {
            List<WebElement> found = driver.findElements(target);
            if (!found.isEmpty() && found.get(0).isDisplayed()) {
                return found.get(0);
            }
            boolean towardEnd = i < MAX_SWIPES;
            drag(driver, y, towardEnd ? width * 8 / 10 : width * 2 / 10, towardEnd ? width * 2 / 10 : width * 8 / 10);
        }
        throw new NoSuchElementException("가로로 밀어도 찾지 못함: " + target);
    }

    // W3C 터치 동작은 iOS·Android에 같은 코드로 동작한다
    private static void drag(AppiumDriver driver, int y, int fromX, int toX) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), fromX, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), toX, y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(drag));
    }

    private static void swipe(AppiumDriver driver, Scroll direction) {
        // 아래 내용을 보려면 손가락은 위로 민다
        String finger = direction == Scroll.TOWARD_BOTTOM ? "up" : "down";
        if (isIos(driver)) {
            driver.executeScript("mobile: swipe", Map.of("direction", finger));
            return;
        }
        Dimension size = driver.manage().window().getSize();
        driver.executeScript("mobile: swipeGesture", Map.of(
                "left", size.width / 10,
                "top", size.height / 3,
                "width", size.width * 8 / 10,
                "height", size.height / 3,
                "direction", finger,
                "percent", 0.75));
    }

    private static boolean isIos(AppiumDriver driver) {
        return driver instanceof IOSDriver;
    }

    private static String predicateQuote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String javaQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}