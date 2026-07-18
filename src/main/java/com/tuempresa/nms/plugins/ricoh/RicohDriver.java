package com.tuempresa.nms.plugins.ricoh;

import com.tuempresa.nms.core.domain.*;
import com.tuempresa.nms.core.ports.PrinterDriver;
import com.tuempresa.nms.infrastructure.snmp.SnmpResponse;
import com.tuempresa.nms.plugins.ricoh.config.FamilyProfile;
import com.tuempresa.nms.plugins.ricoh.config.RicohConfigLoader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Component
@Order(1)
public class RicohDriver implements PrinterDriver {

	private static final Logger log = LoggerFactory.getLogger(RicohDriver.class);
	private static final String BRAND = "Ricoh";

	// OIDs fijos del MIB privado Ricoh
	private static final String OID_MODEL_NAME = ".1.3.6.1.4.1.367.3.2.1.1.1.1.0";
	private static final String OID_SERIAL = ".1.3.6.1.4.1.367.3.2.1.2.1.4.0";
	private static final String OID_COPIER_STATUS = ".1.3.6.1.4.1.367.3.2.1.2.2.22.0";
	private static final String OID_SCAN_STATUS = ".1.3.6.1.4.1.367.3.2.1.2.2.18.0";

	// Standard MIB para tablas
	private static final String OID_STD_PAPER_CURRENT = ".1.3.6.1.2.1.43.8.2.1.10";
	private static final String OID_STD_SUPPLY_CURRENT = ".1.3.6.1.2.1.43.11.1.1.9";

	private final RicohConfigLoader loader;
	private final Resource oidFile;
	private final Resource modelFile;

	private Map<String, FamilyProfile> families = new HashMap<>();
	private Map<String, String> modelToFamily = new HashMap<>();

	public RicohDriver(RicohConfigLoader loader, @Value("classpath:ricoh_oid_5.yml") Resource oidFile,
			@Value("classpath:models-parts5.yml") Resource modelFile) {
		this.loader = loader;
		this.oidFile = oidFile;
		this.modelFile = modelFile;
	}

	@PostConstruct
	public void init() {
		try {
			this.families = loader.loadFamilies(oidFile);
			this.modelToFamily = loader.loadModelMappings(modelFile);
			log.info("Ricoh driver cargado: {} familias, {} modelos mapeados", families.size(), modelToFamily.size());
		} catch (IOException e) {
			throw new RuntimeException("No se pudieron cargar los YAMLs de Ricoh", e);
		}
	}

	@Override
	public boolean supports(String sysDescrOrModelName) {
		if (sysDescrOrModelName == null)
			return false;
		String upper = sysDescrOrModelName.toUpperCase();
		return upper.contains("RICOH") || upper.contains("AFICIO");
	}

	@Override
	public PollPlan planFor(String modelName) {
		String family = resolveFamily(modelName);
		if (family == null || !families.containsKey(family)) {
			throw new IllegalArgumentException(
					"Modelo no mapeado a familia: " + modelName + " (limpio: " + cleanModelName(modelName) + ")");
		}

		FamilyProfile profile = families.get(family);
		Set<String> scalars = new LinkedHashSet<>();

		// Identificacion
		scalars.add(OID_MODEL_NAME);
		scalars.add(OID_SERIAL);

		// Contadores segun features
		scalars.add(profile.counters.totalBlack.oid);
		if (profile.features.color && profile.counters.totalColor != null) {
			scalars.add(profile.counters.totalColor.oid);
		}
		if (profile.features.scanner && profile.counters.totalScan != null) {
			scalars.add(profile.counters.totalScan.oid);
		}

		// Toner
		if (profile.consumables != null && profile.consumables.tonerLevel != null) {
			profile.consumables.tonerLevel.forEach((color, entry) -> {
				if (entry != null && entry.oid != null) {
					scalars.add(entry.oid);
				}
			});
		}

		// Status
		scalars.add(OID_COPIER_STATUS);
		if (profile.features.scanner) {
			scalars.add(OID_SCAN_STATUS);
		}

		// Tablas Standard MIB
		List<TableWalk> walks = Arrays.asList(new TableWalk(OID_STD_PAPER_CURRENT, "paper_trays"),
				new TableWalk(OID_STD_SUPPLY_CURRENT, "physical_supplies"));

		return new PollPlan(scalars, walks, profile.features.color, profile.features.scanner);
	}

	@Override
	public DeviceReading parse(SnmpResponse raw, String ip) {
	    String modelName = raw.getString(OID_MODEL_NAME);   // <-- sin limpiar
	    String family = resolveFamily(modelName);
	    FamilyProfile profile = families.get(family);

	    if (profile == null) {
	        log.warn("Familia no encontrada para modelo: {} (limpio: {}), usando fallback",
	                modelName, cleanModelName(modelName));
	        return fallbackReading(ip, cleanModelName(modelName), raw);
	    }

	    return new DeviceReading(
	            ip,
	            cleanModelName(modelName),      // o modelName, según cómo quieras mostrarlo
	            raw.getString(OID_SERIAL),
	            family,
	            BRAND,
	            Instant.now(),
	            parseCounters(raw, profile),
	            parseConsumables(raw, profile),
	            parsePaperTrays(raw),
	            parseAlerts(raw, profile));
	}

	// métodos públicos para hacer Test
	public String resolveFamily(String modelName) {
		if (modelName == null)
			return null;

		// Primero: buscar nombre exacto (sin solo el punto final)
		String exact = modelName.replace(".", "").trim();

		// ===== DEBUG =====
		System.out.println("Buscando exacto: [" + exact + "]");
		System.out.println("Existe: " + modelToFamily.containsKey(exact));

		modelToFamily.keySet().stream().filter(k -> k.contains("830"))
				.forEach(k -> System.out.println("Clave mapa: [" + k + "]"));
		
		
		System.out.println("resolveFamily(" + modelName + ")");
		
		
		// =================

		if (modelToFamily.containsKey(exact)) {
			return modelToFamily.get(exact);
		}

		// Segundo: buscar versión limpia (sin RICOH/Aficio)
		String clean = cleanModelName(modelName);
		return modelToFamily.get(clean);
	}

	public String cleanModelName(String raw) {
		if (raw == null)
			return null;
		return raw.replace("RICOH ", "").replace("Aficio ", "").replace(".", "").trim();
	}

	// ========== METODOS PRIVADOS ==========
	private CounterSnapshot parseCounters(SnmpResponse raw, FamilyProfile profile) {
		Long totalBlack = parseLong(raw.getString(profile.counters.totalBlack.oid));
		Long totalColor = null;
		Long totalScan = null;

		if (profile.features.color && profile.counters.totalColor != null) {
			totalColor = parseLong(raw.getString(profile.counters.totalColor.oid));
		}
		if (profile.features.scanner && profile.counters.totalScan != null) {
			totalScan = parseLong(raw.getString(profile.counters.totalScan.oid));
		}

		return new CounterSnapshot(totalBlack, totalColor, totalScan);
	}

	private ConsumableSnapshot parseConsumables(SnmpResponse raw, FamilyProfile profile) {
		Integer black = null, cyan = null, magenta = null, yellow = null;

		if (profile.consumables != null && profile.consumables.tonerLevel != null) {
			Map<String, FamilyProfile.TonerEntry> toner = profile.consumables.tonerLevel;

			if (toner.containsKey("black")) {
				black = parseTonerPercent(raw.getString(toner.get("black").oid));
			}
			if (toner.containsKey("cyan")) {
				cyan = parseTonerPercent(raw.getString(toner.get("cyan").oid));
			}
			if (toner.containsKey("magenta")) {
				magenta = parseTonerPercent(raw.getString(toner.get("magenta").oid));
			}
			if (toner.containsKey("yellow")) {
				yellow = parseTonerPercent(raw.getString(toner.get("yellow").oid));
			}
		}

		return new ConsumableSnapshot(black, cyan, magenta, yellow);
	}

	private Integer parseTonerPercent(String raw) {
		if (raw == null || raw.isBlank())
			return null;
		try {
			int val = Integer.parseInt(raw.trim());
			if (val == -2)
				return null; // no medible
			if (val == -100)
				return 0; // casi vacio
			return Math.max(0, Math.min(100, val));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Long parseLong(String raw) {
		if (raw == null || raw.isBlank())
			return 0L;
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private List<PaperTray> parsePaperTrays(SnmpResponse raw) {
		// TODO: implementar parseo de tabla prtInput cuando se necesite
		return List.of();
	}

	private List<StatusAlert> parseAlerts(SnmpResponse raw, FamilyProfile profile) {
		List<StatusAlert> alerts = new ArrayList<>();

		String copierRaw = raw.getString(OID_COPIER_STATUS);
		if (copierRaw != null) {
			alerts.addAll(parseBitmask(copierRaw, "copier", Map.of(1, "toner_low", 7, "paper_end", 8,
					"paper_jam_scanner", 9, "paper_jam_plotter", 10, "toner_end", 11, "service_call")));
		}

		if (profile.features.scanner) {
			String scanRaw = raw.getString(OID_SCAN_STATUS);
			if (scanRaw != null) {
				alerts.addAll(
						parseBitmask(scanRaw, "scanner", Map.of(2, "adf_jam", 3, "adf_cover_open", 6, "service_call")));
			}
		}

		return alerts;
	}

	private List<StatusAlert> parseBitmask(String rawValue, String source, Map<Integer, String> bitToAlert) {
		List<StatusAlert> result = new ArrayList<>();
		try {
			long value = Long.parseLong(rawValue.trim());
			bitToAlert.forEach((bit, type) -> {
				boolean active = (value & (1L << bit)) != 0;
				result.add(new StatusAlert(source, type, active, source + ":" + type));
			});
		} catch (NumberFormatException e) {
			// Ignorar si no es parseable
		}
		return result;
	}

	private DeviceReading fallbackReading(String ip, String modelName, SnmpResponse raw) {
		return new DeviceReading(ip, modelName != null ? modelName : "UNKNOWN", raw.getString(OID_SERIAL), "UNKNOWN",
				BRAND, Instant.now(), new CounterSnapshot(0L, null, null),
				new ConsumableSnapshot(null, null, null, null), List.of(), List.of());
	}
}
