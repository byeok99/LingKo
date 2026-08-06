package com.lingko.lingko.core.domain.sentence.service;

import com.lingko.lingko.api.sentence.dto.SavedSentenceListResponse;
import com.lingko.lingko.api.sentence.dto.SavedSentenceResponse;
import com.lingko.lingko.core.domain.sentence.entity.SavedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.sentence.repository.SavedSentenceRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 다시 연습하려는 문장의 저장·해제와 목록 조회를 담당한다.
 *
 * 저장은 같은 문장을 여러 번 눌러도 결과가 같아야 하므로 멱등하게 다룬다.
 */
@Service
@RequiredArgsConstructor
public class SavedSentenceService {

    private final SavedSentenceRepository savedSentenceRepository;
    private final RecommendedSentenceRepository recommendedSentenceRepository;
    private final UserRepository userRepository;
    private final SentenceService sentenceService;

    /**
     * 저장 상태를 뒤집고 뒤집힌 결과를 반환한다.
     *
     * 화면이 현재 상태를 보내고 서버가 그대로 따르면, 두 기기에서 동시에 누를 때 마지막 요청이
     * 이전 상태를 되돌린다. 서버가 실제 상태를 기준으로 뒤집어야 결과가 예측 가능하다.
     */
    @Transactional
    public SavedSentenceResponse toggle(Long userId, Long sentenceId) {
        requireExistingSentence(sentenceId);

        if (savedSentenceRepository.deleteByUserIdAndSentenceId(userId, sentenceId) > 0) {
            return new SavedSentenceResponse(sentenceId, false);
        }

        try {
            savedSentenceRepository.save(SavedSentence.builder()
                    .user(requireUser(userId))
                    .sentenceId(sentenceId)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            // 같은 사용자가 동시에 두 번 저장하면 유일 제약이 막는다. 결과는 '저장됨'으로 같다.
            return new SavedSentenceResponse(sentenceId, true);
        }
        return new SavedSentenceResponse(sentenceId, true);
    }

    @Transactional(readOnly = true)
    public SavedSentenceListResponse findSaved(Long userId) {
        List<Long> sentenceIds = savedSentenceRepository
                .findByUserUserIdxOrderByCreatedAtDescSavedSentenceIdxDesc(userId)
                .stream()
                .map(SavedSentence::getSentenceId)
                .toList();

        // 저장한 뒤 문장이 비활성화되면 목록에서 조용히 빠진다. 개수도 남은 항목 기준으로 센다.
        List<com.lingko.lingko.api.sentence.dto.PracticeSentenceResponse> items = sentenceIds.stream()
                .map(id -> recommendedSentenceRepository.findBySentenceIdAndActiveTrue(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(sentenceService::toPracticeSentenceResponse)
                .toList();

        return new SavedSentenceListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public List<Long> findSavedSentenceIds(Long userId) {
        return savedSentenceRepository.findSavedSentenceIds(userId);
    }

    private void requireExistingSentence(Long sentenceId) {
        if (recommendedSentenceRepository.findBySentenceIdAndActiveTrue(sentenceId).isEmpty()) {
            throw new SentenceNotFoundException(sentenceId);
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }
}
