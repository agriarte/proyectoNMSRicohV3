package com.tuempresa.nms.core.domain;

public record CounterSnapshot(
    Long totalBlack,
    Long totalColor,
    Long totalScan
) {
    public CounterSnapshot {
        if (totalBlack == null) totalBlack = 0L;
    }
}
