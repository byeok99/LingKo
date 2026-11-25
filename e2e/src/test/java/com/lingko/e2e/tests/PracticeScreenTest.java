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

    @Nested
    @DisplayName("TC-019 자유 문장은 100자까지 평가 요청할 수 있고 101자 입력은 거부된다")
    class CustomSentenceLengthLimit {

        // 녹음 버튼은 평가 기회가 있어야 'Record'로 보이므로, 앞서 돈 테스트가 남긴 기회에 기대지 않도록 맞춘다
        @BeforeEach
        void givenPracticesAvailable() {
            TestSupportApi.ensureE2eUser();
            TestSupportApi.setRemainingPractices(5);
        }

        // API로 101자를 직접 보내는 기대결과 3·4는 backend EvaluationJobTextLimitIntegrationTest가 맡는다
        @Test
        @DisplayName("100자는 준비되어 녹음할 수 있고, 101자는 100자 제한 안내가 나오고 녹음할 수 없다")
        void acceptsUpTo100AndExplainsLimitAt101() {
            String hundred = koreanSentenceOfLength(100);
            String hundredOne = koreanSentenceOfLength(101);

            PracticePage practice = new HomePage(driver()).waitUntilLoaded()
                    .openCustomPractice()
                    .typeSentence(hundred)
                    .waitUntilPrepared();
            assertTrue(practice.showsSentence(hundred), "100자 문장이 입력칸에 그대로 있어야 한다");

            practice.typeSentence(hundredOne);
            assertAll(
                    () -> assertTrue(practice.showsLengthLimitMessage(), "101자는 100자 제한 안내가 보여야 한다"),
                    () -> assertFalse(practice.canRecord(), "101자 문장은 녹음할 수 없어야 한다"));
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
