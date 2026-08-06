package com.lingko.lingko.api.sentence;

import com.lingko.lingko.api.sentence.dto.SavedSentenceListResponse;
import com.lingko.lingko.api.sentence.dto.SavedSentenceResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.sentence.service.SavedSentenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 인증된 사용자가 다시 연습하려고 저장한 문장을 다룬다.
 */
@RestController
@RequestMapping("/api/sentences/saved")
@RequiredArgsConstructor
public class SavedSentenceController {

    private final SavedSentenceService savedSentenceService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    /** 저장한 문장을 최근 저장 순으로 반환한다. */
    @GetMapping
    public SavedSentenceListResponse getSavedSentences(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return savedSentenceService.findSaved(
                activeSessionAuthenticator.authenticateBearer(authorization)
        );
    }

    /**
     * 저장 상태를 뒤집는다.
     *
     * 화면이 원하는 상태를 보내지 않고 서버가 실제 상태를 뒤집는 이유는, 두 기기에서 동시에
     * 누를 때 뒤늦게 도착한 요청이 이전 상태를 되살리는 것을 막기 위해서다.
     */
    @PatchMapping("/{sentenceId}")
    public SavedSentenceResponse toggleSavedSentence(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long sentenceId
    ) {
        return savedSentenceService.toggle(
                activeSessionAuthenticator.authenticateBearer(authorization),
                sentenceId
        );
    }
}
