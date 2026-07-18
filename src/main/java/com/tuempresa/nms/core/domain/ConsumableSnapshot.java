package com.tuempresa.nms.core.domain;

public record ConsumableSnapshot(
    Integer tonerBlackPercent,
    Integer tonerCyanPercent,
    Integer tonerMagentaPercent,
    Integer tonerYellowPercent
) {
}
