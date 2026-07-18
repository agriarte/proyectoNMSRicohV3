package com.tuempresa.nms.core.domain;

public record PaperTray(
    String name,
    Integer currentLevel,
    Integer maxCapacity,
    String mediaSize
) {
}
