package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.TestSupportApi;
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
