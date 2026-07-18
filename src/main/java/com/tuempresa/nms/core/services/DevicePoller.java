package com.tuempresa.nms.core.services;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.domain.PollPlan;
import com.tuempresa.nms.core.ports.PrinterDriver;
import com.tuempresa.nms.infrastructure.persistence.DeviceEntity;
import com.tuempresa.nms.infrastructure.persistence.ReadingEntity;
import com.tuempresa.nms.infrastructure.persistence.ReadingMapper;
import com.tuempresa.nms.infrastructure.snmp.SnmpResponse;
import com.tuempresa.nms.core.ports.DeviceRepository;
import com.tuempresa.nms.core.ports.ReadingRepository;
import com.tuempresa.nms.core.ports.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class DevicePoller {

    private static final Logger log = LoggerFactory.getLogger(DevicePoller.class);

    private final DriverRegistry registry;
    private final SnmpClient snmp;
    private final DeviceRepository deviceRepository;
    private final ReadingRepository readingRepository;
    private final ReadingMapper readingMapper;

    private static final String OID_SYS_DESCR = ".1.3.6.1.2.1.1.1.0";
    private static final String OID_RICOH_MODEL = ".1.3.6.1.4.1.367.3.2.1.1.1.1.0";

    public DevicePoller(DriverRegistry registry,
                        SnmpClient snmp,
                        DeviceRepository deviceRepository,
                        ReadingRepository readingRepository,
                        ReadingMapper readingMapper) {
        this.registry = registry;
        this.snmp = snmp;
        this.deviceRepository = deviceRepository;
        this.readingRepository = readingRepository;
        this.readingMapper = readingMapper;
    }

    @Transactional
    public DeviceReading poll(String ip) {
        log.debug("Sondeando {}...", ip);

        String sysDescr = snmp.getString(ip, OID_SYS_DESCR);
        if (sysDescr == null || sysDescr.isBlank()) {
            log.warn("Sin respuesta SNMP en {}", ip);
            return null;
        }

        PrinterDriver driver = registry.resolve(sysDescr);
        log.debug("Driver: {} para {}", driver.getClass().getSimpleName(), ip);

        String modelName = resolveModelName(ip, driver);
        // TEST
        System.out.println("Modelo leído = [" + modelName + "]"); 
        PollPlan plan = driver.planFor(modelName);

        SnmpResponse raw = snmp.execute(plan, ip);
        DeviceReading reading = driver.parse(raw, ip);

        persist(reading, raw);

        log.info("OK: {} (familia {}) en {}", reading.modelName(), reading.familyCode(), ip);
        return reading;
    }

    private String resolveModelName(String ip, PrinterDriver driver) {
        String modelName = snmp.getString(ip, OID_RICOH_MODEL);
        if (modelName != null && !modelName.isBlank()) {
            return modelName;
        }
        return snmp.getString(ip, OID_SYS_DESCR);
    }

    private void persist(DeviceReading reading, SnmpResponse raw) {
        try {
            Optional<DeviceEntity> existing = deviceRepository.findByIp(reading.ip());
            DeviceEntity device;
            if (existing.isPresent()) {
                device = existing.get();
                device.setModelName(reading.modelName());
                device.setFamilyCode(reading.familyCode());
                device.setBrand(reading.brand());
                device.setSerialNumber(reading.serialNumber());
                device.setLastPolledAt(Instant.now());
            } else {
                device = new DeviceEntity();
                device.setIp(reading.ip());
                device.setModelName(reading.modelName());
                device.setFamilyCode(reading.familyCode());
                device.setBrand(reading.brand());
                device.setSerialNumber(reading.serialNumber());
                device.setDiscoveredAt(Instant.now());
                device.setLastPolledAt(Instant.now());
                device.setActive(true);
            }
            deviceRepository.save(device);

            ReadingEntity readingEntity = readingMapper.toEntity(reading, device);
            readingEntity.setRawOidsJson(rawOidsToJson(raw));
            readingRepository.save(readingEntity);

        } catch (Exception e) {
            log.error("Error persistiendo lectura para {}: {}", reading.ip(), e.getMessage());
        }
    }

    private String rawOidsToJson(SnmpResponse raw) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(raw.allScalars());
        } catch (Exception e) {
            return "{}";
        }
    }
}
