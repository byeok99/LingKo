package com.lingko.lingko.api.legal;

import com.lingko.lingko.core.domain.legal.LegalDocument;
import com.lingko.lingko.core.domain.legal.LegalDocumentService;
import com.lingko.lingko.core.domain.legal.LegalLanguage;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약관·처리방침을 브라우저에서 바로 읽을 수 있는 공개 URL로 제공한다.
 *
 * <p>이 저장소의 다른 endpoint는 {@code Authorization} 헤더로 사용자를 식별하지만
 * 여기는 인증을 요구하지 않는다. 가입 화면에서 아직 계정이 없는 사람과 스토어 심사자가
 * 같은 문서를 열 수 있어야 하기 때문이다. 문서는 모든 이용자에게 동일한 내용이며
 * 개인정보를 담지 않으므로 공개해도 노출되는 사용자 데이터가 없다.
 *
 * <p>JSON이 아닌 HTML을 반환하는 유일한 controller다. 응답 대상이 앱이 아니라 브라우저다.
 */
@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    /**
     * 문서 한 편을 HTML로 반환한다.
     *
     * @param document {@code terms} 또는 {@code privacy}. 그 밖의 값은 404다
     * @param lang     {@code ko} 또는 {@code en}. 생략하거나 지원하지 않는 값이면 한국어로
     *                 되돌린다. 잘못된 언어 때문에 약관을 못 읽는 상황을 만들지 않기 위해
     *                 400이 아니라 기본값으로 처리한다
     */
    @GetMapping("/{document}")
    public ResponseEntity<String> getDocument(
            @PathVariable String document,
            @RequestParam(required = false) String lang
    ) {
        LegalDocument target = LegalDocument.fromPath(document);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        LegalLanguage language = LegalLanguage.from(lang);
        String page = legalDocumentService.renderPage(target, language);

        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8))
                // 문서는 배포 사이에 바뀌지 않지만 개정 시 즉시 반영되어야 한다.
                // 짧은 캐시로 반복 조회 비용만 줄이고 개정 지연은 최소로 둔다.
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                // 문서 페이지는 어떤 외부 자원도 불러오지 않는다. 삽입 위험을 원천 차단한다.
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
                .header("X-Content-Type-Options", "nosniff")
                .body(page);
    }
}
