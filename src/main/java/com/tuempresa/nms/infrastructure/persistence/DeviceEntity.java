package com.tuempresa.nms.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 45)
    private String ip;

    @Column(length = 100)
    private String modelName;

    @Column(length = 50)
    private String familyCode;

    @Column(length = 50)
    private String brand;

    @Column(length = 100)
    private String serialNumber;

    @Column(nullable = false)
    private Instant discoveredAt;

    private Instant lastPolledAt;

    private boolean active = true;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getFamilyCode() { return familyCode; }
    public void setFamilyCode(String familyCode) { this.familyCode = familyCode; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Instant getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(Instant discoveredAt) { this.discoveredAt = discoveredAt; }

    public Instant getLastPolledAt() { return lastPolledAt; }
    public void setLastPolledAt(Instant lastPolledAt) { this.lastPolledAt = lastPolledAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
