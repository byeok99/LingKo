package com.lingko.lingko.core.domain.legal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 약관·처리방침 HTML 변환이 지켜야 하는 계약을 고정한다.
 *
 * <p>보장 대상: 문서 4종이 모두 열릴 것, 표가 원문 파이프가 아닌 표로 변환될 것,
 * 저장소 작업자용 초안 안내가 이용자에게 노출되지 않을 것, 언어 전환 링크가 상대 언어를 가리킬 것.
 */
class LegalDocumentServiceTest {

    private final LegalDocumentService service = new LegalDocumentService();

    @Test
    void 문서와_언어_조합_네_가지를_모두_렌더링한다() {
        for (LegalDocument document : LegalDocument.values()) {
            for (LegalLanguage language : LegalLanguage.values()) {
                String page = service.renderPage(document, language);

                assertThat(page).startsWith("<!doctype html>");
                assertThat(page).contains("<html lang=\"" + language.code() + "\"");
                assertThat(page).contains("<h1>");
            }
        }
    }

    @Test
    void 표를_HTML_표로_변환하고_가로_스크롤로_감싼다() {
        String page = service.renderPage(LegalDocument.PRIVACY, LegalLanguage.KOREAN);

        // 확장 없이 파싱하면 표가 통째로 파이프 문자열 문단으로 남아 읽을 수 없게 된다.
        assertThat(page).contains("<div class=\"table-scroll\"><table>");
        assertThat(page).contains("</table></div>");
        assertThat(page).contains("<th>");
    }

    @Test
    void 저장소_작업자용_초안_안내는_이용자에게_노출하지_않는다() {
        String korean = service.renderPage(LegalDocument.TERMS, LegalLanguage.KOREAN);
        String english = service.renderPage(LegalDocument.TERMS, LegalLanguage.ENGLISH);

        // 원본 맨 앞의 인용문은 변호사 검토를 요구하는 내부 지침이라 문서 본문이 아니다.
        assertThat(korean).doesNotContain("변호사 검토를 받으세요");
        assertThat(english).doesNotContain("obtain legal review before release");
        // 본문은 그대로 남아야 한다.
        assertThat(korean).contains("제1조");
        assertThat(english).contains("Article 1");
    }

    @Test
    void 언어_전환_링크는_같은_문서의_다른_언어를_가리킨다() {
        String korean = service.renderPage(LegalDocument.TERMS, LegalLanguage.KOREAN);
        String english = service.renderPage(LegalDocument.PRIVACY, LegalLanguage.ENGLISH);

        assertThat(korean).contains("href=\"/legal/terms?lang=en\"");
        assertThat(english).contains("href=\"/legal/privacy?lang=ko\"");
    }

    @Test
    void 같은_요청을_반복해도_동일한_결과를_돌려준다() {
        String first = service.renderPage(LegalDocument.PRIVACY, LegalLanguage.ENGLISH);
        String second = service.renderPage(LegalDocument.PRIVACY, LegalLanguage.ENGLISH);

        // 캐시가 문서를 뒤섞지 않는지 확인한다.
        assertThat(second).isEqualTo(first);
        assertThat(service.renderPage(LegalDocument.TERMS, LegalLanguage.ENGLISH))
                .isNotEqualTo(first);
    }

    @Test
    void 지원하지_않는_언어는_오류_대신_한국어로_되돌린다() {
        // 약관은 어떤 상황에서도 읽을 수 있어야 한다. 언어 선택 실패가 열람을 막으면 안 된다.
        assertThat(LegalLanguage.from(null)).isEqualTo(LegalLanguage.KOREAN);
        assertThat(LegalLanguage.from("")).isEqualTo(LegalLanguage.KOREAN);
        assertThat(LegalLanguage.from("ja")).isEqualTo(LegalLanguage.KOREAN);
        assertThat(LegalLanguage.from("EN")).isEqualTo(LegalLanguage.ENGLISH);
    }

    @Test
    void 알_수_없는_문서_경로는_해석하지_않는다() {
        assertThat(LegalDocument.fromPath("terms")).isEqualTo(LegalDocument.TERMS);
        assertThat(LegalDocument.fromPath("PRIVACY")).isEqualTo(LegalDocument.PRIVACY);
        assertThat(LegalDocument.fromPath("cookie")).isNull();
        assertThat(LegalDocument.fromPath(null)).isNull();
    }
}
