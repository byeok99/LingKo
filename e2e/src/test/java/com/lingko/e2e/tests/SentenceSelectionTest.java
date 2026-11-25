package com.lingko.e2e.tests;

import com.lingko.e2e.pages.HomePage;
import com.lingko.e2e.pages.PracticePage;
import com.lingko.e2e.support.BaseTest;
import com.lingko.e2e.support.SeedSentences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 홈에서 고른 추천 문장이 그대로 연습 화면으로 이어지는지 확인한다(Notion TC-014). */
class SentenceSelectionTest extends BaseTest {

    @Test
    @DisplayName("TC-014 추천 문장을 선택하면 같은 문장으로 연습을 시작할 수 있다")
    void selectedSentenceOpensPractice() {
        String sentence = SeedSentences.FOOD_FIRST;

        // openSentence가 Record 버튼까지 기다리므로, 여기까지 오면 기대결과 1(연습 화면 진입)은 확인된 것이다
        PracticePage practice = new HomePage(driver)
                .waitUntilLoaded()
                .selectCategory("Food")
                .openSentence(sentence);

        assertTrue(practice.showsSentence(sentence),
                "연습 화면 입력칸에 선택한 문장이 있어야 한다: " + sentence);
    }
}
