package com.tuempresa.nms.infrastructure.web;

import com.tuempresa.nms.core.services.DiscoveryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    // Scans activos para poder cancelarlos (futura mejora)
    private final Map<String, SseEmitter> activeScans = new ConcurrentHashMap<>();

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Endpoint SSE para descubrimiento en tiempo real.
     * Timeout 0L = infinito, evita cierre por inactividad del navegador.
     * Cada peticion inicia un scan nuevo en un virtual thread.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String subnet) {
        SseEmitter emitter = new SseEmitter(0L);
        String scanId = java.util.UUID.randomUUID().toString();
        activeScans.put(scanId, emitter);

        emitter.onCompletion(() -> activeScans.remove(scanId));
        emitter.onTimeout(() -> activeScans.remove(scanId));
        emitter.onError((e) -> activeScans.remove(scanId));

        Thread.startVirtualThread(() -> {
            try {
                discoveryService.discoverStream(subnet, emitter);
            } finally {
                activeScans.remove(scanId);
            }
        });

        return emitter;
    }

    /**
     * Endpoint clasico (bloqueante). Devuelve JSON al finalizar.
     */
    @GetMapping("/scan")
    public java.util.List<com.tuempresa.nms.core.domain.DiscoveryResult> scan(
            @RequestParam String subnet) {
        return discoveryService.discover(subnet);
    }
}
