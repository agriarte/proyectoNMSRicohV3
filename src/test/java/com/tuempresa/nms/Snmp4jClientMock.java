package com.tuempresa.nms;

import com.tuempresa.nms.core.domain.PollPlan;
import com.tuempresa.nms.core.ports.SnmpClient;
import com.tuempresa.nms.infrastructure.snmp.SnmpResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock de SnmpClient para tests sin impresora real.
 * Configura los valores OID que devolvera para cada IP.
 */
public class Snmp4jClientMock implements SnmpClient {

    private final Map<String, Map<String, String>> responses = new HashMap<>();

    public void setResponse(String ip, String oid, String value) {
        responses.computeIfAbsent(ip, k -> new HashMap<>()).put(oid, value);
    }

    public void setRicohImC5510(String ip) {
        // Identificacion
        setResponse(ip, ".1.3.6.1.2.1.1.1.0", "RICOH IM C5510 Network Printer");
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.1.1.1.0", "RICOH IM C5510.");
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.1.4.0", "SN123456789");

        // Contadores (ME068)
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.22", "150000");  // total_black
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.21", "45000");   // total_color

        // Toner (ME068)
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.1", "85");   // black
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.2", "72");   // cyan
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.3", "68");   // magenta
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.4", "91");   // yellow

        // Status
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.2.22.0", "0");  // todo OK
    }

    public void setRicohAficioSpc8300(String ip) {

        // sysDescr (usado por DriverRegistry)
        setResponse(ip, ".1.3.6.1.2.1.1.1.0", "RICOH Aficio SP C830DN");

        // Modelo (OID privado Ricoh)
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.1.1.1.0", "Aficio SP C830DN");

        // Número de serie
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.1.4.0", "XCN123456");

        // Estado de la copiadora (sin errores)
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.2.22.0", "0");

        // Contadores
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.14", "500000"); // total_black
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.13", "185000"); // total_color

        // Tóner (%)
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.1", "45"); // Black
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.2", "62"); // Cyan
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.3", "58"); // Magenta
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.4", "70"); // Yellow

        // Bitmask de alertas: toner_low
        setResponse(ip, ".1.3.6.1.4.1.367.3.2.1.2.2.18.0", "2");
    }
    

    public void setHpPrinter(String ip) {
        setResponse(ip, ".1.3.6.1.2.1.1.1.0", "HP LaserJet Pro M404dn");
        // No configuramos OIDs Ricoh → unsupported
    }

    public void setNoResponse(String ip) {
        // No entradas → null en getString
    }

    @Override
    public String getString(String ip, String oid) {
        Map<String, String> ipResponses = responses.get(ip);
        if (ipResponses == null) return null;
        return ipResponses.get(oid);
    }

    @Override
    public SnmpResponse execute(PollPlan plan, String ip) {
        SnmpResponse response = new SnmpResponse();
        Map<String, String> ipResponses = responses.getOrDefault(ip, Map.of());

        for (String oid : plan.scalarOids()) {
            response.putScalar(oid, ipResponses.get(oid));
        }
        return response;
    }
}
