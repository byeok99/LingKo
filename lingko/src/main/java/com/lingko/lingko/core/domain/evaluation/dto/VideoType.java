package com.lingko.lingko.core.domain.evaluation.dto;

public enum VideoType {
    TONGUE("tongue"),
    MOUTH("mouth");

    private final String prefix;

    VideoType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
