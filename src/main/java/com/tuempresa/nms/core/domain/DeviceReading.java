package com.tuempresa.nms.core.domain;

import java.time.Instant;
import java.util.List;

public record DeviceReading(
    String ip,
    String modelName,
    String serialNumber,
    String familyCode,
    String brand,
    Instant polledAt,
    CounterSnapshot counters,
    ConsumableSnapshot consumables,
    List<PaperTray> paperTrays,
    List<StatusAlert> alerts
) {
    public DeviceReading {
        if (polledAt == null) polledAt = Instant.now();
        if (paperTrays == null) paperTrays = List.of();
        if (alerts == null) alerts = List.of();
    }
}
