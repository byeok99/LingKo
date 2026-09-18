package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;

import java.util.Locale;

/**
 * 비용이 발생하는 가이드 사전 생성 범위를 명시한다.
 *
 * 인자 없이 실행하면 항상 dry-run이다. 실제 외부 호출은 {@code --prewarm-dry-run=false}를
 * 명시해야만 가능하도록 fail-safe 기본값을 사용한다.
 */
public record GuideMediaPrewarmOptions(
        boolean dryRun,
        int offset,
        int limit,
        VideoType type,
        boolean continueOnError
) {
    public GuideMediaPrewarmOptions {
        if (offset < 0) {
            throw new IllegalArgumentException("prewarm offset must be zero or positive");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("prewarm limit must be positive");
        }
    }

    /** CLI 인자를 비용 안전 기본값을 가진 실행 옵션으로 변환한다. */
    public static GuideMediaPrewarmOptions fromArgs(String[] args) {
        boolean dryRun = true;
        int offset = 0;
        int limit = Integer.MAX_VALUE;
        VideoType type = null;
        boolean continueOnError = true;

        for (String argument : args) {
            if (argument.startsWith("--prewarm-dry-run=")) {
                dryRun = parseBoolean(argument);
            } else if (argument.startsWith("--prewarm-offset=")) {
                offset = Integer.parseInt(value(argument));
            } else if (argument.startsWith("--prewarm-limit=")) {
                limit = Integer.parseInt(value(argument));
            } else if (argument.startsWith("--prewarm-type=")) {
                type = VideoType.valueOf(value(argument).toUpperCase(Locale.ROOT));
            } else if (argument.startsWith("--prewarm-continue-on-error=")) {
                continueOnError = parseBoolean(argument);
            }
        }
        return new GuideMediaPrewarmOptions(dryRun, offset, limit, type, continueOnError);
    }

    private static String value(String argument) {
        return argument.substring(argument.indexOf('=') + 1).trim();
    }

    private static boolean parseBoolean(String argument) {
        String parsedValue = value(argument).toLowerCase(Locale.ROOT);
        if (!parsedValue.equals("true") && !parsedValue.equals("false")) {
            // dry-run 오타가 false로 해석되면 의도치 않은 유료 호출이 시작되므로 fail-closed 한다.
            throw new IllegalArgumentException(argument.substring(0, argument.indexOf('='))
                    + " must be true or false");
        }
        return Boolean.parseBoolean(parsedValue);
    }
}
