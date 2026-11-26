package com.lingko.e2e.tests;

import com.lingko.e2e.pages.AiConsentPage;
import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.Simulator;
import com.lingko.e2e.support.TestSupportApi;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 연습 화면 E2E.
 *
 * <p>Notion TC는 {@code @Nested} 클래스 하나에 대응하고, 그 안의 {@code @BeforeEach}가 TC 전용 사전조건 hook이다.</p>
 */
class PracticeScreenTest extends BaseTest {

    // 녹음 버튼은 평가 기회가 있어야 'Record'로 보이므로, 앞서 돈 테스트가 남긴 기회에 기대지 않도록 맞춘다
    private static void givenPracticesAvailable() {
        TestSupportApi.ensureE2eUser();
        TestSupportApi.setRemainingPractices(5);
    }

    @Nested
    @DisplayName("TC-017 자유 문장은 최대 100자까지 입력할 수 있다")
    class CustomSentenceUpTo100 {

        @BeforeEach
        void given() {
            givenPracticesAvailable();
        }

        @Test
        @DisplayName("100자 문장은 표준 발음이 준비되고 녹음할 수 있다")
        void prepares100CharacterSentence() {
            String hundred = koreanSentenceOfLength(100);

            // waitUntilPrepared가 표준 발음과 Record 버튼까지 기다리므로, 여기까지 오면 기대결과 1·2는 확인된 것이다
            PracticePage practice = new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeSentence(hundred)
                    .waitUntilPrepared();
            assertTrue(practice.showsSentence(hundred), "100자 문장이 입력칸에 그대로 있어야 한다");
        }
    }

    @Nested
    @DisplayName("TC-018 자유 문장이 100자를 넘으면 제한 안내가 표시되고 녹음할 수 없다")
    class CustomSentenceOver100 {

        @BeforeEach
        void given() {
            givenPracticesAvailable();
        }

        // 서버도 101자를 400으로 거부하는 최종 경계는 backend EvaluationJobTextLimitIntegrationTest가 맡는다
        @Test
        @DisplayName("101자 문장은 100자 제한 안내가 나오고 녹음할 수 없다")
        void explainsLimitFor101CharacterSentence() {
            PracticePage practice = new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeSentence(koreanSentenceOfLength(101));

            assertAll(
                    () -> assertTrue(practice.showsLengthLimitMessage(), "101자는 100자 제한 안내가 보여야 한다"),
                    () -> assertFalse(practice.showsStandardPronunciation(), "101자 문장은 표준 발음이 준비되지 않아야 한다"),
                    () -> assertFalse(practice.canRecord(), "101자 문장은 녹음할 수 없어야 한다"));
        }
    }

    @Nested
    @DisplayName("TC-019 문장부호만 입력하면 입력창에 남지 않고 발음 준비가 시작되지 않는다")
    class PunctuationOnly {

        // 앱은 입력과 붙여넣기 단계에서 문장부호·기호를 지운다(PracticeSentenceInputFormatter)
        private static final String PUNCTUATION = ".,?!";
        // 입력 동작 확인용. 앱이 지우지 않는 글자다
        private static final String HANGUL = "안녕";

        @Test
        @DisplayName("'.,?!'를 입력하면 입력창이 비고 표준 발음·오류 안내가 나오지 않는다")
        void punctuationIsDroppedWithoutPreparation() {
            PracticePage practice = new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeRaw(PUNCTUATION)
                    .waitPastPreparationDelay();
            String afterPunctuation = practice.inputText();
            boolean prepared = practice.showsStandardPronunciation();
            boolean errorShown = practice.showsAnyInputError();

            // 입력 자체가 막혀도 입력창은 비어 보이므로, 같은 입력칸에 한글은 들어가는지 확인해 위 결과가 의미 있게 한다
            String afterHangul = practice.typeRaw(HANGUL).inputText();

            assertAll(
                    () -> assertEquals("", afterPunctuation, "문장부호는 입력창에 남지 않아야 한다"),
                    () -> assertFalse(prepared, "문장부호만으로는 표준 발음이 준비되지 않아야 한다"),
                    () -> assertFalse(errorShown, "문장부호만 입력했을 때 오류 안내가 나오지 않아야 한다"),
                    () -> assertEquals(HANGUL, afterHangul, "같은 입력칸에 한글은 들어가야 한다(입력 동작 확인)"));
        }
    }

    @Nested
    @DisplayName("TC-021 표준 발음은 음운 규칙(연음·비음화 등)을 적용한 소리대로 표시된다")
    class PhonologicalRules {

        // TC Test Data. 기대값은 backend 음운 규칙 결과이므로 규칙이 바뀌면 TC와 함께 고친다
        private static final String LINKING = "음악을 들어요";
        private static final String LINKING_PRONUNCIATION = "으마글 드러요";
        private static final String NASALIZATION = "감사합니다";
        private static final String NASALIZATION_PRONUNCIATION = "감사함니다";

        @Test
        @DisplayName("'음악을 들어요'는 '으마글 드러요'로, '감사합니다'는 '감사함니다'로 표시된다")
        void showsPronunciationAfterRules() {
            // 발음 준비만 확인하고 녹음하지 않으므로 평가 기회 사전조건은 두지 않는다
            PracticePage practice = new HomePage(driver()).waitUntilLoaded().openCustomPractice();
            String linking = practice.typeSentence(LINKING).waitForStandardPronunciation(LINKING_PRONUNCIATION);
            String nasalization = practice.typeSentence(NASALIZATION)
                    .waitForStandardPronunciation(NASALIZATION_PRONUNCIATION);

            assertAll(
                    () -> assertEquals(LINKING_PRONUNCIATION, linking, "연음이 적용되어야 한다: " + LINKING),
                    () -> assertEquals(NASALIZATION_PRONUNCIATION, nasalization,
                            "비음화가 적용되어야 한다: " + NASALIZATION));
        }
    }

    @Nested
    @DisplayName("TC-022 로마자 발음은 표준 발음의 소리를 기준으로 음절과 단어 경계를 구분해 표시된다")
    class RomanizedPronunciation {

        // TC Test Data. 로마자는 원문이 아니라 표준 발음에서 파생되므로 표준 발음과 함께 둔다
        private static final String LINKING = "음악을 들어요";
        private static final String LINKING_PRONUNCIATION = "으마글 드러요";
        private static final String LINKING_ROMANIZED = "eu-ma-geul deu-reo-yo";
        private static final String NASALIZATION = "감사합니다";
        private static final String NASALIZATION_PRONUNCIATION = "감사함니다";
        private static final String NASALIZATION_ROMANIZED = "gam-sa-ham-ni-da";

        @Test
        @DisplayName("'음악을 들어요'는 'eu-ma-geul deu-reo-yo'로, '감사합니다'는 'gam-sa-ham-ni-da'로 표시된다")
        void showsRomanizationOfStandardPronunciation() {
            // 발음 준비만 확인하고 녹음하지 않으므로 평가 기회 사전조건은 두지 않는다
            PracticePage practice = new HomePage(driver()).waitUntilLoaded().openCustomPractice();
            // 표준 발음이 바뀐 것을 확인한 뒤 읽어야 앞 문장의 로마자와 섞이지 않는다
            String linkingPronunciation = practice.typeSentence(LINKING)
                    .waitForStandardPronunciation(LINKING_PRONUNCIATION);
            String linkingRomanized = practice.romanizedPronunciation();
            String nasalizationPronunciation = practice.typeSentence(NASALIZATION)
                    .waitForStandardPronunciation(NASALIZATION_PRONUNCIATION);
            String nasalizationRomanized = practice.romanizedPronunciation();

            assertAll(
                    () -> assertEquals(LINKING_PRONUNCIATION, linkingPronunciation, "표준 발음: " + LINKING),
                    () -> assertEquals(LINKING_ROMANIZED, linkingRomanized, "로마자 발음: " + LINKING),
                    () -> assertEquals(NASALIZATION_PRONUNCIATION, nasalizationPronunciation,
                            "표준 발음: " + NASALIZATION),
                    () -> assertEquals(NASALIZATION_ROMANIZED, nasalizationRomanized, "로마자 발음: " + NASALIZATION));
        }
    }

    @Nested
    @DisplayName("TC-024 AI 처리 동의를 거부하면 평가가 시작되지 않고 평가 기회가 차감되지 않는다")
    class AiConsentDeclined {

        // TC Test Data: 평가 기회 3회. 5회가 아니어야 '차감 없음'과 '최대치 유지'가 구분된다
        private static final int PRACTICES = 3;
        private static final String SENTENCE = "감사합니다";
        private static final Duration RECORD_FOR = Duration.ofSeconds(2);

        private String accessToken;
        private int historyBefore;

        // 사전조건 hook: 앱을 띄우기 전에 마이크 허용·서버 기회 3회를 맞추고, 비교 기준이 될 기록 수를 남긴다
        @BeforeEach
        void given() {
            Simulator.setMicrophoneAllowed(true);
            accessToken = TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(PRACTICES);
            historyBefore = TestSupportApi.evaluationHistoryCount(accessToken);
        }

        @Test
        @DisplayName("동의 화면에서 'Not now'를 고르면 연습 화면으로 돌아오고 기회 3회·기록 수가 그대로다")
        void declineKeepsQuotaAndHistory() {
            HomePage home = new HomePage(driver()).waitUntilLoaded();
            // 동의 철회는 hook이 아니라 앱이 뜬 뒤에 한다. E2E 앱(main_e2e.dart)은 시작할 때 테스트 세션을 받으며
            // AI 동의를 다시 기록하므로, 앱보다 먼저 철회하면 되살아난다. 앱은 평가 요청 때마다 동의 상태를 새로 조회한다
            TestSupportApi.withdrawAiConsent(accessToken);
            PracticePage practice = home.openCustomPractice()
                    .typeSentence(SENTENCE)
                    .recordAndRequestEvaluation(RECORD_FOR);
            new AiConsentPage(driver()).waitUntilLoaded().declineSending();
            boolean backWithRecording = practice.showsKeptRecording();

            // 기대결과 2는 화면 대신 서버 값으로 본다. 거절 경로는 서버에 아무것도 보내지 않아야 하기 때문이다
            assertAll(
                    () -> assertTrue(backWithRecording, "평가 없이 녹음을 보존한 연습 화면으로 돌아와야 한다"),
                    () -> assertEquals(PRACTICES, TestSupportApi.currentPractices(accessToken).remaining(),
                            "평가 기회가 차감되지 않아야 한다"),
                    () -> assertEquals(historyBefore, TestSupportApi.evaluationHistoryCount(accessToken),
                            "새 연습 기록이 생기지 않아야 한다"));
        }
    }

    @Nested
    @DisplayName("TC-025 마이크 권한 거부 시 녹음을 시작하지 않고 권한 안내를 표시한다")
    class MicrophoneDenied {

        private static final String SENTENCE = "감사합니다";

        // 권한 변경은 실행 중인 앱을 종료시키므로 앱을 띄우기 전인 hook에서 거부 상태를 만든다
        @BeforeEach
        void given() {
            givenPracticesAvailable();
            Simulator.setMicrophoneAllowed(false);
        }

        // 녹음하는 다른 TC(TC-024 등)가 권한 거부 상태를 물려받지 않도록, 실패해도 허용으로 되돌린다
        @AfterEach
        void restoreMicrophone() {
            Simulator.setMicrophoneAllowed(true);
        }

        @Test
        @DisplayName("권한이 없으면 녹음 대신 권한 안내가 나오고, 허용한 뒤에는 녹음이 시작된다")
        void showsGuideThenRecordsAfterAllowing() {
            PracticePage denied = new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeSentence(SENTENCE)
                    .tapRecord();
            boolean guideShown = denied.showsMicrophoneDeniedMessage();
            boolean recordingWhileDenied = denied.isRecording();

            // 기기 설정에서 허용하는 단계. iOS가 권한을 바꾸며 앱을 종료하므로 다시 실행해 연습 화면으로 돌아간다
            Simulator.setMicrophoneAllowed(true);
            relaunchApp();
            new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeSentence(SENTENCE)
                    .tapRecord()
                    .waitUntilRecording();

            // 녹음 시작은 바로 위 waitUntilRecording이 시간 안에 확인하지 못하면 실패로 처리한다(기대결과 2)
            assertAll(
                    () -> assertTrue(guideShown, "마이크 권한 안내가 표시되어야 한다"),
                    () -> assertFalse(recordingWhileDenied, "권한이 없으면 녹음이 시작되지 않아야 한다"));
        }
    }

    @Nested
    @DisplayName("TC-026 녹음 중 취소하면 녹음이 폐기되고 평가 요청이 생성되지 않는다")
    class RecordingCancelled {

        // TC Test Data: 시작 평가 기회 3회. 5회가 아니어야 '차감 없음'과 '최대치 유지'가 구분된다
        private static final int PRACTICES = 3;
        private static final String SENTENCE = "감사합니다";
        // 테스트 발화 1건에 해당하는 녹음 시간
        private static final Duration RECORD_FOR = Duration.ofSeconds(2);

        private String accessToken;
        private int historyBefore;

        // AI 처리 동의는 E2E 앱이 시작할 때 다시 기록하므로 따로 맞추지 않는다(TC-024 참고)
        @BeforeEach
        void given() {
            Simulator.setMicrophoneAllowed(true);
            accessToken = TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(PRACTICES);
            historyBefore = TestSupportApi.evaluationHistoryCount(accessToken);
        }

        @Test
        @DisplayName("녹음 중 'Cancel'을 고르면 녹음 전 화면으로 돌아오고 기회 3회·기록 수가 그대로다")
        void cancelDiscardsRecording() throws InterruptedException {
            PracticePage practice = new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeSentence(SENTENCE)
                    .tapRecord();
            practice.waitUntilRecording();
            Thread.sleep(RECORD_FOR.toMillis());
            boolean backBeforeRecording = practice.cancelRecording().isBackBeforeRecording();

            // 기대결과 2·3은 화면 대신 서버 값으로 본다. 취소 경로는 서버에 아무것도 보내지 않아야 하기 때문이다
            assertAll(
                    () -> assertTrue(backBeforeRecording, "녹음이 남지 않은 녹음 전 화면으로 돌아와야 한다"),
                    () -> assertEquals(PRACTICES, TestSupportApi.currentPractices(accessToken).remaining(),
                            "평가 기회가 차감되지 않아야 한다"),
                    () -> assertEquals(historyBefore, TestSupportApi.evaluationHistoryCount(accessToken),
                            "새 연습 기록이 생기지 않아야 한다"));
        }
    }

    /**
     * TC Test Data 조건에 맞는 정확한 길이의 한글 문장. 공백은 포함하되 연속·앞뒤 공백과 문장부호·이모지는 없다.
     * 앱과 서버는 정리(문장부호 제거·공백 정규화) 후 길이를 세므로, 정리해도 길이가 그대로인 문장을 만든다.
     */
    private static String koreanSentenceOfLength(int length) {
        String words = "안녕하세요 오늘도 발음을 연습해요 ";
        StringBuilder sentence = new StringBuilder();
        while (sentence.length() < length) {
            sentence.append(words.charAt(sentence.length() % words.length()));
        }
        if (sentence.charAt(length - 1) == ' ') {
            sentence.setCharAt(length - 1, '요');
        }
        String result = sentence.toString();
        assertEquals(length, result.length(), "테스트 데이터 길이");
        return result;
    }
}
