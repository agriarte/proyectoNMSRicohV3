package com.tuempresa.nms.core.services;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.domain.DiscoveryResult;
import com.tuempresa.nms.core.ports.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final SnmpClient snmp;
    private final DriverRegistry registry;
    private final DevicePoller poller;

    private static final String OID_SYS_DESCR = ".1.3.6.1.2.1.1.1.0";

    public DiscoveryService(SnmpClient snmp, DriverRegistry registry, DevicePoller poller) {
        this.snmp = snmp;
        this.registry = registry;
        this.poller = poller;
    }

    /**
     * Discovery clasico (bloqueante). Devuelve lista al final.
     */
    public java.util.List<DiscoveryResult> discover(String subnet) {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

        var tasks = IntStream.rangeClosed(1, 254)
            .mapToObj(i -> subnet + "." + i)
            .map(ip -> CompletableFuture.supplyAsync(() -> probe(ip), pool))
            .toList();

        var result = tasks.stream()
            .map(CompletableFuture::join)
            .filter(java.util.Objects::nonNull)
            .toList();

        pool.shutdown();
        return result;
    }

    /**
     * Discovery via SSE con virtual threads.
     * Envia eventos en tiempo real: PROBING, FOUND, UNSUPPORTED, COMPLETE.
     */
    public void discoverStream(String subnet, SseEmitter emitter) {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

        try {
            sendEvent(emitter, Map.ofEntries(
                Map.entry("event", "START"),
                Map.entry("subnet", subnet),
                Map.entry("time", LocalTime.now())
            ));

            for (int i = 1; i <= 254; i++) {
                String ip = subnet + "." + i;

                CompletableFuture.runAsync(() -> {
                    try {
                        // 1. Probing
                        sendEvent(emitter, Map.ofEntries(
                            Map.entry("event", "PROBING"),
                            Map.entry("ip", ip),
                            Map.entry("time", LocalTime.now())
                        ));

                        // 2. SNMP sysDescr
                        String sysDescr = snmp.getString(ip, OID_SYS_DESCR);

                        if (sysDescr == null || sysDescr.isBlank()) {
                            return; // sin respuesta
                        }

                        // 3. Verificar si hay driver soportado
                        try {
                        	registry.resolve(sysDescr);  // Lanza excepción si no hay driver
                            // Soportado → poll completo
                            DeviceReading reading = poller.poll(ip);
                            if (reading != null) {
                                sendEvent(emitter, Map.ofEntries(
                                    Map.entry("event", "FOUND"),
                                    Map.entry("ip", ip),
                                    Map.entry("brand", reading.brand()),
                                    Map.entry("modelName", reading.modelName()),
                                    Map.entry("familyCode", reading.familyCode()),
                                    Map.entry("serialNumber", reading.serialNumber()),
                                    Map.entry("totalBlack", reading.counters().totalBlack()),
                                    Map.entry("totalColor", reading.counters().totalColor()),
                                    Map.entry("totalScan", reading.counters().totalScan()),
                                    Map.entry("tonerBlack", reading.consumables().tonerBlackPercent()),
                                    Map.entry("tonerCyan", reading.consumables().tonerCyanPercent()),
                                    Map.entry("tonerMagenta", reading.consumables().tonerMagentaPercent()),
                                    Map.entry("tonerYellow", reading.consumables().tonerYellowPercent())
                                ));
                            }
                        } catch (UnsupportedDeviceException e) {
                            // No soportado pero respondio SNMP
                            sendEvent(emitter, Map.ofEntries(
                                Map.entry("event", "UNSUPPORTED"),
                                Map.entry("ip", ip),
                                Map.entry("sysDescr", sysDescr.substring(0, Math.min(80, sysDescr.length()))),
                                Map.entry("reason", e.getMessage())
                            ));
                        }

                    } catch (Exception e) {
                        log.debug("Error en {}: {}", ip, e.getMessage());
                    }
                }, pool);
            }

            // Esperar a que terminen todas las tareas
            pool.shutdown();
            boolean finished = pool.awaitTermination(3, TimeUnit.MINUTES);
            if (!finished) {
                log.warn("Algunas tareas no terminaron a tiempo");
            }

            sendEvent(emitter, Map.ofEntries(
                Map.entry("event", "COMPLETE"),
                Map.entry("time", LocalTime.now())
            ));
            emitter.complete();

        } catch (Exception e) {
            log.error("Error en discoverStream: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    private DiscoveryResult probe(String ip) {
        try {
            String sys = snmp.getString(ip, OID_SYS_DESCR);
            if (sys == null || sys.isBlank()) {
                return null;
            }
            try {
                var driver = registry.resolve(sys);
                return new DiscoveryResult(ip, "FOUND", sys, null, null, null, "Dispositivo soportado");
            } catch (UnsupportedDeviceException e) {
                return new DiscoveryResult(ip, "UNSUPPORTED", sys, null, null, null, e.getMessage());
            }
        } catch (Exception e) {
            return null;
        }
    }

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
