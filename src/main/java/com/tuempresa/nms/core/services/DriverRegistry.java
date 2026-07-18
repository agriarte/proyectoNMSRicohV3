package com.tuempresa.nms.core.services;

import com.tuempresa.nms.core.ports.PrinterDriver;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DriverRegistry {

    private final List<PrinterDriver> drivers;

    public DriverRegistry(List<PrinterDriver> drivers) {
        this.drivers = drivers.stream()
            .sorted(Comparator.comparingInt(d -> {
                var order = d.getClass().getAnnotation(org.springframework.core.annotation.Order.class);
                return order != null ? order.value() : Integer.MAX_VALUE;
            }))
            .toList();
    }

    public PrinterDriver resolve(String sysDescr) {
        return drivers.stream()
            .filter(d -> d.supports(sysDescr))
            .findFirst()
            .orElseThrow(() -> new UnsupportedDeviceException("No hay driver para: " + sysDescr));
    }

    public List<PrinterDriver> allDrivers() {
        return drivers;
    }
}
