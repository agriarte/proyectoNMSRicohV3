package com.tuempresa.nms.core.domain;

public record StatusAlert(
    String source,
    String type,
    boolean active,
    String description
) {
}
