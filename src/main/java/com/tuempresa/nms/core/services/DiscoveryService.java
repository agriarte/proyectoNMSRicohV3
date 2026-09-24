package com.tuempresa.nms.core.services;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.domain.DiscoveryResult;
import com.tuempresa.nms.core.ports.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class DiscoveryService {

	private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

	private final SnmpClient snmp;
	private final DriverRegistry registry;
	private final DevicePoller poller;
	private final int maxConcurrentIps;

	private static final String OID_SYS_DESCR = ".1.3.6.1.2.1.1.1.0";

	public DiscoveryService(SnmpClient snmp, DriverRegistry registry, DevicePoller poller,
			@Value("${nms.discovery.concurrent-ips:50}") int maxConcurrentIps) {

		this.snmp = snmp;
		this.registry = registry;
		this.poller = poller;
		this.maxConcurrentIps = maxConcurrentIps;
	}

	/**
	 * Discovery clásico (bloqueante).
	 * Devuelve la lista al final.
	 *
	 * Limita la concurrencia real de sondeos SNMP simultáneos
	 * mediante un Semaphore para no saturar el socket UDP
	 * compartido cuando se lanzan 254 hilos virtuales.
	 */
	public List<DiscoveryResult> discover(String subnet) {

		ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

		Semaphore throttle = new Semaphore(maxConcurrentIps);

		try {

			var tasks = IntStream.rangeClosed(1, 254).mapToObj(i -> subnet + "." + i)
					.map(ip -> CompletableFuture.supplyAsync(() -> probeThrottled(ip, throttle), pool)).toList();

			return tasks.stream().map(CompletableFuture::join).filter(java.util.Objects::nonNull).toList();

		} finally {
			pool.shutdown();
		}
	}

	/**
	 * Discovery mediante SSE con virtual threads.
	 *
	 * Mantiene el mismo límite de concurrencia real que
	 * el discovery clásico.
	 *
	 * Envía eventos en tiempo real:
	 *
	 * START
	 * PROBING
	 * FOUND
	 * UNSUPPORTED
	 * COMPLETE
	 */
	public void discoverStream(String subnet, SseEmitter emitter) {

		ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

		Semaphore throttle = new Semaphore(maxConcurrentIps);

		try {

			sendEvent(emitter, Map.ofEntries(Map.entry("event", "START"), Map.entry("subnet", subnet),
					Map.entry("time", LocalTime.now())));

			List<CompletableFuture<Void>> tasks = new ArrayList<>();

			for (int i = 1; i <= 254; i++) {

				String ip = subnet + "." + i;

				CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {

					try {

						throttle.acquire();

					} catch (InterruptedException ie) {

						Thread.currentThread().interrupt();
						return;
					}

					try {

						probeAndEmit(ip, emitter);

					} catch (Exception e) {

						log.warn("Error inesperado sondeando {}: {}", ip, e.getMessage(), e);

					} finally {

						throttle.release();
					}

				}, pool);

				tasks.add(task);
			}

			boolean finished;

			try {

				CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get(3, TimeUnit.MINUTES);

				finished = true;

			} catch (java.util.concurrent.TimeoutException te) {

				finished = false;

				log.warn("El escaneo de {} no terminó en 3 minutos", subnet);
			}

			if (!finished) {

				log.warn("Algunas tareas no terminaron a tiempo");
			}

			sendEvent(emitter, Map.ofEntries(Map.entry("event", "COMPLETE"), Map.entry("time", LocalTime.now())));

			emitter.complete();

		} catch (Exception e) {

			log.error("Error en discoverStream: {}", e.getMessage(), e);

			emitter.completeWithError(e);

		} finally {

			pool.shutdown();
		}
	}

	/**
	 * Sondea una IP y envía el resultado mediante SSE.
	 */
	private void probeAndEmit(String ip, SseEmitter emitter) {

		// ---------------------------------------------------------
		// 1. Avisamos a la interfaz de que comenzamos el sondeo
		// ---------------------------------------------------------

		sendEvent(emitter,
				Map.ofEntries(Map.entry("event", "PROBING"), Map.entry("ip", ip), Map.entry("time", LocalTime.now())));

		// ---------------------------------------------------------
		// 2. Obtenemos sysDescr mediante SNMP
		// ---------------------------------------------------------

		String sysDescr = snmp.getString(ip, OID_SYS_DESCR);

		// No ha respondido al SNMP
		if (sysDescr == null || sysDescr.isBlank()) {
			return;
		}

		try {

			// -----------------------------------------------------
			// 3. Comprobamos si existe un driver compatible
			// -----------------------------------------------------

			registry.resolve(sysDescr);

			// -----------------------------------------------------
			// 4. Hacemos el polling completo del dispositivo
			// -----------------------------------------------------

			DeviceReading reading = poller.poll(ip);

			if (reading != null) {

				// -------------------------------------------------
				// IMPORTANTE:
				//
				// No utilizamos Map.ofEntries() aquí porque
				// Map.entry() NO admite valores null.
				//
				// Algunos dispositivos pueden tener, por ejemplo:
				//
				// totalColor = null
				// totalScan  = null
				//
				// HashMap sí permite esos valores.
				// -------------------------------------------------

				Map<String, Object> data = new HashMap<>();

				data.put("event", "FOUND");
				data.put("ip", ip);
				data.put("brand", reading.brand());
				data.put("modelName", reading.modelName());
				data.put("familyCode", reading.familyCode());
				data.put("serialNumber", reading.serialNumber());

				// Contadores
				data.put("totalBlack", reading.counters().totalBlack());

				data.put("totalColor", reading.counters().totalColor());

				data.put("totalScan", reading.counters().totalScan());

				// Consumibles
				data.put("tonerBlack", reading.consumables().tonerBlackPercent());

				data.put("tonerCyan", reading.consumables().tonerCyanPercent());

				data.put("tonerMagenta", reading.consumables().tonerMagentaPercent());

				data.put("tonerYellow", reading.consumables().tonerYellowPercent());

				// -------------------------------------------------
				// Enviamos el dispositivo encontrado al navegador
				// -------------------------------------------------

				sendEvent(emitter, data);
			}

		} catch (UnsupportedDeviceException e) {

			// -----------------------------------------------------
			// El dispositivo responde a SNMP pero no tenemos
			// ningún driver compatible.
			// -----------------------------------------------------

			sendEvent(emitter,
					Map.ofEntries(Map.entry("event", "UNSUPPORTED"), Map.entry("ip", ip),
							Map.entry("sysDescr", sysDescr.substring(0, Math.min(80, sysDescr.length()))),
							Map.entry("reason", e.getMessage())));
		}
	}

	/**
	 * Ejecuta un probe respetando el límite de concurrencia.
	 */
	private DiscoveryResult probeThrottled(String ip, Semaphore throttle) {

		try {

			throttle.acquire();

		} catch (InterruptedException ie) {

			Thread.currentThread().interrupt();
			return null;
		}

		try {

			return probe(ip);

		} finally {

			throttle.release();
		}
	}

	/**
	 * Discovery clásico de una única IP.
	 */
	private DiscoveryResult probe(String ip) {

		try {

			String sys = snmp.getString(ip, OID_SYS_DESCR);

			if (sys == null || sys.isBlank()) {
				return null;
			}

			try {

				registry.resolve(sys);

				return new DiscoveryResult(ip, "FOUND", sys, null, null, null, "Dispositivo soportado");

			} catch (UnsupportedDeviceException e) {

				return new DiscoveryResult(ip, "UNSUPPORTED", sys, null, null, null, e.getMessage());
			}

		} catch (Exception e) {

			log.warn("Error inesperado sondeando {}: {}", ip, e.getMessage(), e);

			return null;
		}
	}

	/**
	 * Envía un evento SSE al navegador.
	 *
	 * Se sincroniza sobre el emitter porque varios virtual
	 * threads pueden intentar enviar eventos simultáneamente.
	 */
	private void sendEvent(SseEmitter emitter, Map<String, Object> data) {

		try {

			synchronized (emitter) {

				emitter.send(data);
			}

		} catch (Exception e) {

			log.debug("Error enviando evento SSE: {}", e.getMessage());
		}
	}

}