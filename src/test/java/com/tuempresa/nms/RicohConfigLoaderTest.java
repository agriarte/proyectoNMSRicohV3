package com.tuempresa.nms;

import com.tuempresa.nms.plugins.ricoh.config.FamilyProfile;
import com.tuempresa.nms.plugins.ricoh.config.RicohConfigLoader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RicohConfigLoaderTest {

    private final RicohConfigLoader loader = new RicohConfigLoader();

    @Test
    void loadFamilies_shouldParseAllMeFamilies() throws IOException {
        var families = loader.loadFamilies(new ClassPathResource("ricoh_oid_5.yml"));

        assertFalse(families.isEmpty(), "Debe haber al menos una familia");
        assertTrue(families.containsKey("ME001"), "Debe existir ME001");
        assertTrue(families.containsKey("ME051"), "Debe existir ME051 (familia IM)");
        assertTrue(families.containsKey("ME078"), "Debe existir ME078 (ultima familia)");

        // Verificar ME051 (IM C2000, IM C2500, etc.)
        FamilyProfile me051 = families.get("ME051");
        assertNotNull(me051);
        assertTrue(me051.features.color, "ME051 debe ser color");
        assertTrue(me051.features.multifunction, "ME051 debe ser multifuncion");
        assertFalse(me051.features.scanner, "ME051 NO tiene scanner en este MIB");
        assertEquals("color", me051.features.toner);

        assertNotNull(me051.counters.totalBlack);
        assertNotNull(me051.counters.totalColor);
        assertNull(me051.counters.totalScan); // no tiene scanner

        assertNotNull(me051.consumables);
        assertNotNull(me051.consumables.tonerLevel);
        assertTrue(me051.consumables.tonerLevel.containsKey("black"));
        assertTrue(me051.consumables.tonerLevel.containsKey("cyan"));
        assertTrue(me051.consumables.tonerLevel.containsKey("magenta"));
        assertTrue(me051.consumables.tonerLevel.containsKey("yellow"));

        // Verificar ME064 (tiene scanner)
        FamilyProfile me064 = families.get("ME064");
        assertNotNull(me064);
        assertTrue(me064.features.scanner, "ME064 debe tener scanner");
        assertNotNull(me064.counters.totalScan, "ME064 debe tener OID de scan");
    }

    @Test
    void loadModelMappings_shouldMapImC5510toMe051() throws IOException {
        var mappings = loader.loadModelMappings(new ClassPathResource("models-parts5.yml"));

        assertEquals("ME068", mappings.get("IM C5510"), "IM C5510 → ME068");
        assertEquals("ME051", mappings.get("IM C2000"), "IM C2000 → ME051");
        assertEquals("ME068", mappings.get("IM C2010"), "IM C2010 → ME068");
        assertEquals("ME001", mappings.get("Aficio SP 8300DN"), "Aficio SP 8300DN → ME001");
        assertEquals("ME002", mappings.get("Aficio SP C830DN"), "Aficio SP C830DN → ME002");
        assertEquals("ME002", mappings.get("Aficio SP C831DN"), "Aficio SP C831DN → ME002 (misma familia)");
    }

    @Test
    void loadFamilies_me001_shouldBeMonoPrinter() throws IOException {
        var families = loader.loadFamilies(new ClassPathResource("ricoh_oid_5.yml"));
        FamilyProfile me001 = families.get("ME001");

        assertNotNull(me001);
        assertFalse(me001.features.color, "ME001 es monocromo");
        assertFalse(me001.features.multifunction, "ME001 es solo impresora");
        assertFalse(me001.features.scanner, "ME001 no tiene scanner");
        assertEquals("mono", me001.features.toner);

        assertNotNull(me001.counters.totalBlack);
        assertNull(me001.counters.totalColor);
        assertNull(me001.counters.totalScan);

        assertNotNull(me001.consumables.tonerLevel);
        assertTrue(me001.consumables.tonerLevel.containsKey("black"));
        assertFalse(me001.consumables.tonerLevel.containsKey("cyan"), "Monocromo no tiene cyan");
    }
}
