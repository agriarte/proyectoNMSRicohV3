package com.tuempresa.nms;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.services.DevicePoller;
import com.tuempresa.nms.core.services.DriverRegistry;
import com.tuempresa.nms.infrastructure.persistence.DeviceEntity;
import com.tuempresa.nms.infrastructure.persistence.ReadingEntity;
import com.tuempresa.nms.infrastructure.persistence.ReadingMapper;
import com.tuempresa.nms.plugins.ricoh.RicohDriver;
import com.tuempresa.nms.plugins.ricoh.config.RicohConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import com.tuempresa.nms.core.services.UnsupportedDeviceException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integracion del flujo completo: mock SNMP → parse → persistencia.
 * No requiere impresora real ni base de datos.
 */
class DevicePollerIntegrationTest {

    private Snmp4jClientMock snmpMock;
    private DevicePoller poller;

    @BeforeEach
    void setUp() throws IOException {
        // 1. Cargar driver Ricoh
        RicohConfigLoader loader = new RicohConfigLoader();
        RicohDriver ricohDriver = new RicohDriver(
            loader,
            new ClassPathResource("ricoh_oid_5.yml"),
            new ClassPathResource("models-parts5.yml")
        );
        ricohDriver.init();

        // 2. Registry con solo Ricoh (simula Spring)
        DriverRegistry registry = new DriverRegistry(List.of(ricohDriver));

        // 3. Mock SNMP
        snmpMock = new Snmp4jClientMock();

        // 4. Repositorios en memoria (simulados)
        InMemoryDeviceRepository deviceRepo = new InMemoryDeviceRepository();
        InMemoryReadingRepository readingRepo = new InMemoryReadingRepository();
        ReadingMapper mapper = new ReadingMapper(new com.fasterxml.jackson.databind.ObjectMapper());

        // 5. Poller con todo mock
        poller = new DevicePoller(registry, snmpMock, deviceRepo, readingRepo, mapper);
    }

    @Test
    void poll_ricohImC5510_shouldReturnColorCountersAndPersist() {
        String ip = "192.168.1.100";
        snmpMock.setRicohImC5510(ip);

        DeviceReading reading = poller.poll(ip);

        assertNotNull(reading);
        assertEquals("IM C5510", reading.modelName());
        assertEquals("ME068", reading.familyCode());
        assertEquals("Ricoh", reading.brand());
        assertEquals("SN123456789", reading.serialNumber());

        // Contadores
        assertEquals(150000L, reading.counters().totalBlack());
        assertEquals(45000L, reading.counters().totalColor());
        assertNotNull(reading.counters().totalScan()); // ME068 tiene scanner

        // Consumibles
        assertEquals(85, reading.consumables().tonerBlackPercent());
        assertEquals(72, reading.consumables().tonerCyanPercent());
        assertEquals(68, reading.consumables().tonerMagentaPercent());
        assertEquals(91, reading.consumables().tonerYellowPercent());

        // Alertas
        assertTrue(reading.alerts().stream()
            .anyMatch(a -> a.type().equals("toner_low") && !a.active()),
            "Toner no debe estar bajo");
    }

    @Test
    void poll_ricohAficioSpc8300_shouldReturnColor() {
        String ip = "192.168.1.101";
        snmpMock.setRicohAficioSpc8300(ip);

        DeviceReading reading = poller.poll(ip);

        assertNotNull(reading);
        assertEquals(500000L, reading.counters().totalBlack());
        assertEquals(185000L, reading.counters().totalColor());
        assertNull(reading.counters().totalScan());

        assertEquals(45, reading.consumables().tonerBlackPercent());
        assertEquals(62, reading.consumables().tonerCyanPercent());
        assertEquals(58, reading.consumables().tonerMagentaPercent());
        assertEquals(70, reading.consumables().tonerYellowPercent());
    }

    @Test
    void poll_hpPrinter_shouldReturnNull() {
        String ip = "192.168.1.200";
        snmpMock.setHpPrinter(ip);

        assertThrows(
        	    UnsupportedDeviceException.class,
        	    () -> poller.poll(ip)
        	);
    }

    @Test
    void poll_noResponse_shouldReturnNull() {
        String ip = "192.168.1.99";
        snmpMock.setNoResponse(ip);

        DeviceReading reading = poller.poll(ip);
        assertNull(reading);
    }
}
