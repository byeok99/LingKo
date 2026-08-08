package com.lingko.lingko.core.domain.legal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 약관·처리방침 Markdown 원본을 읽어 열람 가능한 HTML 페이지로 만든다.
 *
 * <p>HTML 사본을 저장소에 따로 두지 않고 서빙 시점에 변환한다. 사본을 두면
 * {@code docs/legal/}의 원본과 어긋났을 때 사용자에게 보이는 쪽이 옛 내용이 되고,
 * 그 사실을 알아채기 어렵다.
 *
 * <p>이 서비스는 사용자 데이터를 다루지 않는다. 문서는 누구에게나 같은 내용이므로
 * 인증을 요구하지 않으며, 이 점이 곧 스토어 심사와 가입 화면이 요구하는 공개 URL 조건을 만족시킨다.
 */
@Service
public class LegalDocumentService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    /**
     * 변환 결과 캐시다.
     *
     * <p>문서는 배포 사이에 바뀌지 않는 정적 리소스라 요청마다 파싱할 이유가 없다.
     * 항목 수는 문서 2종 × 언어 2종으로 상한이 고정되어 있어 만료 정책을 두지 않는다.
     */
    private final Map<String, String> renderedCache = new ConcurrentHashMap<>();

    public LegalDocumentService() {
        // 문서에 수집 항목·보유기간 표가 많다. table 확장이 없으면 표가 통째로
        // 원문 파이프 문자열로 노출되어 읽을 수 없게 된다.
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    /**
     * 문서 한 편의 완성된 HTML 페이지를 돌려준다.
     *
     * @param document 열람 대상 문서
     * @param language 표시 언어. 지원하지 않는 값은 호출 전에 {@link LegalLanguage#from}이 걸러낸다
     * @return 브라우저가 그대로 렌더링할 수 있는 전체 HTML 문서
     * @throws IllegalStateException 리소스가 없거나 읽을 수 없을 때. 문서는 jar에 함께
     *         포함되므로 이 상황은 사용자 입력 문제가 아니라 배포 산출물이 깨진 것이다
     */
    public String renderPage(LegalDocument document, LegalLanguage language) {
        String cacheKey = document.name() + ':' + language.code();
        return renderedCache.computeIfAbsent(
                cacheKey,
                key -> buildPage(document, language)
        );
    }

    private String buildPage(LegalDocument document, LegalLanguage language) {
        String markdown = readMarkdown(document, language);
        Node parsed = parser.parse(stripDraftBanner(markdown));
        String body = renderer.render(parsed);
        String title = extractTitle(markdown, document, language);
        return LegalPageTemplate.wrap(title, body, document, language);
    }

    private String readMarkdown(LegalDocument document, LegalLanguage language) {
        ClassPathResource resource = new ClassPathResource(document.resourcePath(language));
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Legal document resource is missing: " + document.resourcePath(language),
                    error
            );
        }
    }

    /**
     * 문서 맨 앞의 초안 안내 인용문을 제거한다.
     *
     * <p>원본에는 자리표시자 교체와 변호사 검토를 요구하는 인용문이 붙어 있다.
     * 그 내용은 저장소를 읽는 작업자에게 필요한 것이지 이용자에게 보여줄 것이 아니다.
     * 원본에서 지우면 검토 전 배포를 막는 장치가 사라지므로, 원본은 그대로 두고
     * 서빙 시점에만 걷어낸다.
     */
    private String stripDraftBanner(String markdown) {
        String[] lines = markdown.split("\n", -1);
        StringBuilder kept = new StringBuilder();
        boolean insideBanner = false;
        for (String line : lines) {
            boolean isQuote = line.startsWith("> ") || line.equals(">");
            if (isQuote) {
                insideBanner = true;
                continue;
            }
            // 인용문 바로 다음의 빈 줄까지 함께 버려 문단 사이가 벌어지지 않게 한다.
            if (insideBanner && line.isBlank()) {
                insideBanner = false;
                continue;
            }
            insideBanner = false;
            kept.append(line).append('\n');
        }
        return kept.toString();
    }

    /**
     * 문서의 첫 번째 H1을 페이지 제목으로 쓴다.
     *
     * <p>제목을 별도 상수로 관리하면 문서를 고칠 때 한쪽만 바뀐다. 없으면 문서 종류와
     * 언어로 만든 이름으로 되돌린다. 제목 부재가 열람 자체를 막아서는 안 된다.
     */
    private String extractTitle(String markdown, LegalDocument document, LegalLanguage language) {
        for (String line : markdown.split("\n", -1)) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return "LingKo " + document.path() + " (" + language.code() + ")";
    }
}
