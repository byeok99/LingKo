package com.lingko.lingko.core.domain.evaluation.service;

/**
 * 외부 가이드 source URL을 job 등록 전에 검증하는 application 경계다.
 */
@FunctionalInterface
public interface GuideSourceUrlPolicy {

    void validate(String sourceUrl);
}
