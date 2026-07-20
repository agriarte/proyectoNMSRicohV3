package com.tuempresa.nms;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.ports.DeviceRepository;
import com.tuempresa.nms.core.ports.ReadingRepository;
import com.tuempresa.nms.core.ports.SnmpClient;
import com.tuempresa.nms.core.services.DevicePoller;
import com.tuempresa.nms.infrastructure.persistence.DeviceEntity;
import com.tuempresa.nms.infrastructure.persistence.ReadingEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integracion REAL: levanta un Postgres real (Testcontainers), arranca
 * el contexto completo de Spring y comprueba que DevicePoller.persist() escribe
 * de verdad en las tablas devices/readings via JPA/Hibernate.
 *
 * A diferencia de DevicePollerIntegrationTest (que usa repositorios en memoria),
 * este test SI valida el mapeo JPA, el DDL generado por Hibernate y la conexion
 * real a Postgres.
 */
@Testcontainers
@SpringBootTest
class DevicePollerPersistenceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nms_printers_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Deja que Hibernate cree el esquema en el Postgres real de test
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    /**
     * Sustituye el SnmpClient real (Snmp4jClient, que intentaria hablar por red)
     * por el mock ya usado en DevicePollerIntegrationTest. Todo lo demas
     * (JPA, repositorios, RicohDriver, DriverRegistry) es el bean real de Spring.
     */
    @TestConfiguration
    static class MockSnmpConfig {
        @Bean
        @Primary
        SnmpClient snmpClient() {
            return new Snmp4jClientMock();
        }
    }

    @Autowired
    private DevicePoller poller;

    @Autowired
    private SnmpClient snmpClient; // es el mock gracias a @Primary

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ReadingRepository readingRepository;

    @Test
    void poll_ricohImC5510_persistsDeviceAndReadingInRealPostgres() {
        String ip = "192.168.50.10";
        ((Snmp4jClientMock) snmpClient).setRicohImC5510(ip);

        DeviceReading reading = poller.poll(ip);
        assertNotNull(reading, "El poll deberia devolver una lectura valida");

        // --- Comprueba que devices tiene la fila, releyendo desde Postgres real ---
        Optional<DeviceEntity> savedDevice = deviceRepository.findByIp(ip);
        assertTrue(savedDevice.isPresent(), "El dispositivo deberia haberse persistido en devices");
        assertEquals("Ricoh", savedDevice.get().getBrand());
        assertEquals("SN123456789", savedDevice.get().getSerialNumber());
        assertNotNull(savedDevice.get().getId(), "Deberia tener un id autogenerado por Postgres");

        // --- Comprueba que readings tiene la fila asociada ---
        List<ReadingEntity> readings =
                readingRepository.findByDeviceIdOrderByPolledAtDesc(savedDevice.get().getId());
        assertFalse(readings.isEmpty(), "Deberia existir al menos una lectura en readings");

        ReadingEntity lastReading = readings.get(0);
        assertEquals(150000L, lastReading.getTotalBlack());
        assertEquals(45000L, lastReading.getTotalColor());
        assertEquals(85, lastReading.getTonerBlackPercent());
        assertNotNull(lastReading.getAlertsJson(), "El JSON de alertas deberia haberse serializado");
    }

    @Test
    void poll_samdeIpTwice_updatesExistingDeviceInsteadOfDuplicating() {
        String ip = "192.168.50.11";
        ((Snmp4jClientMock) snmpClient).setRicohAficioSpc8300(ip);

        poller.poll(ip);
        poller.poll(ip);

        // Debe haber UN solo device para esa IP (upsert), pero DOS lecturas historicas
        List<DeviceEntity> allWithThatIp = deviceRepository.findByIp(ip).stream().toList();
        assertEquals(1, allWithThatIp.size(), "No debe duplicar el device en cada poll");

        Long deviceId = allWithThatIp.get(0).getId();
        List<ReadingEntity> readings = readingRepository.findByDeviceIdOrderByPolledAtDesc(deviceId);
        assertEquals(2, readings.size(), "Cada poll debe anadir una nueva fila de reading, no sobrescribir");
    }
}
