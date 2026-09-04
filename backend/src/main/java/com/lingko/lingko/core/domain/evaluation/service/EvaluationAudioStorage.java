package com.lingko.lingko.core.domain.evaluation.service;

import java.nio.file.Path;
import java.time.Instant;

/**
 * 평가 도메인이 S3 SDK 세부사항 없이 사용자 소유 음성의 업로드·다운로드·삭제를 요청하는 port다.
 */
public interface EvaluationAudioStorage {

    UploadTicket prepareUpload(Long userId, String fileName, String contentType, long contentLength);

    void validateUploaded(Long userId, String objectKey);

    Path download(String objectKey);

    void delete(String objectKey);

    /**
     * 회원 탈퇴 시 제출 여부와 무관하게 사용자 prefix의 원격 음성을 모두 삭제한다.
     *
     * @return 삭제 요청한 object 수
     */
    int deleteAllForUser(Long userId);

    void deleteLocal(Path path);

    record UploadTicket(String objectKey, String uploadUrl, Instant expiresAt) {
    }
}
