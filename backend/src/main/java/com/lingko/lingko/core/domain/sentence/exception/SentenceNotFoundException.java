package com.lingko.lingko.core.domain.sentence.exception;

public class SentenceNotFoundException extends RuntimeException {
    public SentenceNotFoundException(Long sentenceId) {
        super("Sentence not found: " + sentenceId);
    }
}
