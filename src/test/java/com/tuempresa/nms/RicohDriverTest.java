package com.tuempresa.nms;

import com.tuempresa.nms.core.domain.PollPlan;
import com.tuempresa.nms.plugins.ricoh.RicohDriver;
import com.tuempresa.nms.plugins.ricoh.config.RicohConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RicohDriverTest {

    private RicohDriver driver;

    @BeforeEach
    void setUp() throws IOException {
        RicohConfigLoader loader = new RicohConfigLoader();
        driver = new RicohDriver(
            loader,
            new ClassPathResource("ricoh_oid_5.yml"),
            new ClassPathResource("models-parts5.yml")
        );
        driver.init();
    }

    @Test
    void supports_shouldMatchRicohDevices() {
        assertTrue(driver.supports("RICOH IM C5510"));
        assertTrue(driver.supports("Aficio SP 8300DN"));
        assertTrue(driver.supports("RICOH MP C4504ex"));
       
        assertFalse(driver.supports("HP LaserJet"));
        assertFalse(driver.supports("Canon imageRUNNER"));
        assertFalse(driver.supports(null));
    }

    
    
    
    @Test
    void planFor_imC5510_shouldIncludeColorAndTonerOids() {
        PollPlan plan = driver.planFor("RICOH IM C5510");

        assertTrue(plan.needsColor(), "IM C5510 es color");
        assertTrue(plan.needsScanner(), "IM C5510 tiene scanner segun MIB");
        assertFalse(plan.scalarOids().isEmpty(), "Debe tener OIDs escalares");

        // Debe incluir OIDs de toner CMYK
        long tonerOids = plan.scalarOids().stream()
            .filter(oid -> oid.contains(".2.24.1.1.5."))
            .count();
        assertEquals(4, tonerOids, "Debe tener 4 OIDs de toner (CMYK)");
    }

    @Test
    void planFor_aficioSp8300_shouldBeMonoOnly() {
        PollPlan plan = driver.planFor("Aficio SP 8300DN");

        assertFalse(plan.needsColor(), "SP 8300DN es monocromo");

        // Solo toner black
        long tonerOids = plan.scalarOids().stream()
            .filter(oid -> oid.contains(".2.24.1.1.5."))
            .count();
        assertEquals(1, tonerOids, "Monocromo solo tiene 1 toner OID");
    }

    @Test
    void planFor_unknownModel_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> {
            driver.planFor("RICOH UNKNOWN MODEL XYZ");
        });
    }

    @Test
    void cleanModelName_shouldStripPrefixes() {
        assertEquals("IM C5510", driver.cleanModelName("RICOH IM C5510."));
        assertEquals("SP 8300DN", driver.cleanModelName("Aficio SP 8300DN"));
        assertEquals("MP C4504ex", driver.cleanModelName("RICOH MP C4504ex"));
    }

    @Test
    void resolveFamily_shouldMapCorrectly() {
        assertEquals("ME051", driver.resolveFamily("IM C2000"));
        assertEquals("ME001", driver.resolveFamily("Aficio SP 8300DN"));
        assertEquals("ME068", driver.resolveFamily("IM C2010"));
        assertNull(driver.resolveFamily("UNKNOWN MODEL"));
    }
    
    @Test
    void planFor_imC5510_shouldIncludeColorAndTonerOids2() {
        System.out.println("Family for IM C5510: " + driver.resolveFamily("IM C5510"));
        System.out.println("Clean model: " + driver.cleanModelName("IM C5510"));
        
        PollPlan plan = driver.planFor("IM C5510");
        System.out.println("Plan: " + plan);
        
        assertTrue(plan.needsScanner(), "IM C5510 tiene scanner segun MIB");
        // ...
    }
}
