package com.lingko.lingko.core.domain.legal;

/**
 * 변환된 본문을 감싸 완성된 HTML 페이지로 만든다.
 *
 * <p>템플릿 엔진을 두지 않고 문자열로 조립한다. 페이지가 두 종류뿐이고 사용자 입력을
 * 끼워 넣지 않아서, 엔진과 뷰 리졸버를 추가하는 비용이 얻는 것보다 크다.
 *
 * <p>스타일을 외부 파일이 아닌 인라인으로 둔 것도 같은 이유다. 약관은 앱이 아직 로그인도
 * 못 한 상태나 느린 회선에서도 열려야 하므로 추가 요청 없이 한 응답으로 완결시킨다.
 */
final class LegalPageTemplate {

    private LegalPageTemplate() {
    }

    /**
     * @param title    문서 제목. 문서의 첫 H1에서 가져온 값이라 신뢰할 수 있는 내부 문자열이다
     * @param bodyHtml Markdown에서 변환된 본문 HTML
     */
    static String wrap(String title, String bodyHtml, LegalDocument document, LegalLanguage language) {
        LegalLanguage other = language.other();
        String switchHref = "/legal/%s?lang=%s".formatted(document.path(), other.code());
        return """
                <!doctype html>
                <html lang="%s">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                :root { color-scheme: light dark; }
                body {
                  margin: 0 auto; padding: 24px 18px 72px; max-width: 46rem;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
                    "Helvetica Neue", "Apple SD Gothic Neo", "Noto Sans KR", sans-serif;
                  font-size: 16px; line-height: 1.75; word-break: keep-all;
                  color: #14181f; background: #ffffff;
                }
                h1 { font-size: 1.55rem; line-height: 1.35; margin: 0 0 1.4rem; letter-spacing: -0.02em; }
                h2 { font-size: 1.15rem; margin: 2.4rem 0 0.8rem; letter-spacing: -0.01em; }
                h3 { font-size: 1rem; margin: 1.8rem 0 0.6rem; }
                p, li { font-size: 0.95rem; }
                a { color: #1d4ed8; }
                hr { border: 0; border-top: 1px solid #e3e7ee; margin: 2.2rem 0; }
                /* 표에 열이 많아 좁은 화면에서 넘친다. 페이지 전체가 가로로 밀리지 않도록
                   표 자체만 가로 스크롤되게 감싼다. */
                .table-scroll { overflow-x: auto; margin: 1rem 0; }
                table { border-collapse: collapse; width: 100%%; font-size: 0.85rem; }
                th, td { border: 1px solid #e3e7ee; padding: 8px 10px; text-align: left; vertical-align: top; }
                th { background: #f5f7fa; font-weight: 700; }
                code { background: #f5f7fa; padding: 0.1em 0.35em; border-radius: 4px; font-size: 0.85em; }
                blockquote { margin: 1rem 0; padding: 0.6rem 1rem; border-left: 3px solid #c9d2e0; color: #4a5464; }
                .lang-switch { display: inline-block; margin-bottom: 1.6rem; font-size: 0.85rem; }
                @media (prefers-color-scheme: dark) {
                  body { color: #e7ecf3; background: #12151b; }
                  a { color: #8ab4ff; }
                  hr, th, td { border-color: #2b323d; }
                  th, code { background: #1b2029; }
                  blockquote { border-left-color: #39424f; color: #aab3c0; }
                }
                </style>
                </head>
                <body>
                <a class="lang-switch" href="%s">%s</a>
                %s
                </body>
                </html>
                """.formatted(
                language.htmlLang(),
                title,
                switchHref,
                other.displayName(),
                wrapTables(bodyHtml)
        );
    }

    /**
     * 표를 가로 스크롤 컨테이너로 감싼다.
     *
     * <p>commonmark는 {@code <table>}만 내보내므로 좁은 화면에서 페이지 전체가 가로로 밀린다.
     * 렌더러를 확장하는 대신 결과 문자열을 치환한다. 대상이 고정된 태그 하나뿐이고
     * 사용자 입력이 끼어들지 않아 이 수준의 치환으로 충분하다.
     */
    private static String wrapTables(String bodyHtml) {
        return bodyHtml
                .replace("<table>", "<div class=\"table-scroll\"><table>")
                .replace("</table>", "</table></div>");
    }
}
