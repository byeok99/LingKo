package com.lingko.e2e.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 테스트 앱의 iOS 시뮬레이터 권한을 {@code xcrun simctl privacy}로 바꾼다.
 *
 * <p>권한 팝업을 화면에서 누르는 방식은 OS 버전마다 문구·위치가 달라 흔들리므로, 기기 설정에서 허용·거부한 상태를
 * 직접 만든다. Appium의 {@code mobile: setPermission}은 별도 도구(applesimutils)가 필요해 쓰지 않는다.</p>
 *
 * <p>주의: 권한을 바꾸면 iOS가 실행 중인 앱을 종료한다. 앱을 띄우기 전에 바꾸거나, 바꾼 뒤 앱을 다시 실행한다.
 * 실기기·Android에서는 동작하지 않는다.</p>
 */
public final class Simulator {

    // 같은 Mac에 여러 시뮬레이터가 켜져 있으면 -Dios.udid로 대상을 지정한다
    private static final String DEVICE = System.getProperty("ios.udid", "booted");

    private Simulator() {
    }

    /** 앱의 마이크 권한을 허용(true) 또는 거부(false) 상태로 맞춘다. 실패하면 사전조건이 틀린 것이므로 예외를 던진다. */
    public static void setMicrophoneAllowed(boolean allowed) {
        run("xcrun", "simctl", "privacy", DEVICE, allowed ? "grant" : "revoke", "microphone", BaseTest.BUNDLE_ID);
    }

    private static void run(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("시간 안에 끝나지 않았다: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException(String.join(" ", command) + " -> " + process.exitValue() + " " + output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("xcrun을 실행하지 못했다(macOS·Xcode 필요): " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("명령 실행이 중단됐다: " + String.join(" ", command), e);
        }
    }
}
