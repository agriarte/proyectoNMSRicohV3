package com.tuempresa.nms.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "readings", indexes = {
    @Index(name = "idx_reading_device_time", columnList = "device_id, polled_at")
})
public class ReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(nullable = false)
    private Instant polledAt;

    private Long totalBlack;
    private Long totalColor;
    private Long totalScan;

    private Integer tonerBlackPercent;
    private Integer tonerCyanPercent;
    private Integer tonerMagentaPercent;
    private Integer tonerYellowPercent;

    @Column(columnDefinition = "jsonb")
    private String alertsJson;

    @Column(columnDefinition = "jsonb")
    private String rawOidsJson;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }

    public Instant getPolledAt() { return polledAt; }
    public void setPolledAt(Instant polledAt) { this.polledAt = polledAt; }

    public Long getTotalBlack() { return totalBlack; }
    public void setTotalBlack(Long totalBlack) { this.totalBlack = totalBlack; }

    public Long getTotalColor() { return totalColor; }
    public void setTotalColor(Long totalColor) { this.totalColor = totalColor; }

    public Long getTotalScan() { return totalScan; }
    public void setTotalScan(Long totalScan) { this.totalScan = totalScan; }

    public Integer getTonerBlackPercent() { return tonerBlackPercent; }
    public void setTonerBlackPercent(Integer tonerBlackPercent) { this.tonerBlackPercent = tonerBlackPercent; }

    public Integer getTonerCyanPercent() { return tonerCyanPercent; }
    public void setTonerCyanPercent(Integer tonerCyanPercent) { this.tonerCyanPercent = tonerCyanPercent; }

    public Integer getTonerMagentaPercent() { return tonerMagentaPercent; }
    public void setTonerMagentaPercent(Integer tonerMagentaPercent) { this.tonerMagentaPercent = tonerMagentaPercent; }

    public Integer getTonerYellowPercent() { return tonerYellowPercent; }
    public void setTonerYellowPercent(Integer tonerYellowPercent) { this.tonerYellowPercent = tonerYellowPercent; }

    public String getAlertsJson() { return alertsJson; }
    public void setAlertsJson(String alertsJson) { this.alertsJson = alertsJson; }

    public String getRawOidsJson() { return rawOidsJson; }
    public void setRawOidsJson(String rawOidsJson) { this.rawOidsJson = rawOidsJson; }
}
