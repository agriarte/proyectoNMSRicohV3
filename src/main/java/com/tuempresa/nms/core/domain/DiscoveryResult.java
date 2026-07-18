package com.tuempresa.nms.core.domain;

public record DiscoveryResult(
    String ip,
    String status,
    String sysDescr,
    String modelName,
    String familyCode,
    String brand,
    String message
) {
    public static DiscoveryResult found(String ip, String sysDescr, String modelName, String familyCode, String brand) {
        return new DiscoveryResult(ip, "FOUND", sysDescr, modelName, familyCode, brand, "Dispositivo soportado");
    }

    public static DiscoveryResult unsupported(String ip, String sysDescr) {
        return new DiscoveryResult(ip, "UNSUPPORTED", sysDescr, null, null, null, "Marca no soportada");
    }

    public static DiscoveryResult timeout(String ip) {
        return new DiscoveryResult(ip, "TIMEOUT", null, null, null, null, "Sin respuesta SNMP");
    }
}
