package com.tuempresa.nms.infrastructure.web;

import com.tuempresa.nms.core.ports.DeviceRepository;
import com.tuempresa.nms.infrastructure.persistence.DeviceEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;

    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping
    public List<DeviceEntity> listAll() {
        return deviceRepository.findAll();
    }

    @GetMapping("/brand/{brand}")
    public List<DeviceEntity> byBrand(@PathVariable String brand) {
        return deviceRepository.findByBrand(brand);
    }

    @GetMapping("/family/{family}")
    public List<DeviceEntity> byFamily(@PathVariable String family) {
        return deviceRepository.findByFamilyCode(family);
    }
}
