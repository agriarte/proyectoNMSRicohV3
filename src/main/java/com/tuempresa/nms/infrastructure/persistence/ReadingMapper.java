package com.tuempresa.nms.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuempresa.nms.core.domain.DeviceReading;
import org.springframework.stereotype.Component;

@Component
public class ReadingMapper {

    private final ObjectMapper objectMapper;

    public ReadingMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReadingEntity toEntity(DeviceReading reading, DeviceEntity device) {
        ReadingEntity e = new ReadingEntity();
        e.setDevice(device);
        e.setPolledAt(reading.polledAt());
        e.setTotalBlack(reading.counters().totalBlack());
        e.setTotalColor(reading.counters().totalColor());
        e.setTotalScan(reading.counters().totalScan());

        var cons = reading.consumables();
        e.setTonerBlackPercent(cons.tonerBlackPercent());
        e.setTonerCyanPercent(cons.tonerCyanPercent());
        e.setTonerMagentaPercent(cons.tonerMagentaPercent());
        e.setTonerYellowPercent(cons.tonerYellowPercent());

        try {
            e.setAlertsJson(objectMapper.writeValueAsString(reading.alerts()));
        } catch (Exception ex) {
            e.setAlertsJson("[]");
        }

        return e;
    }
}
