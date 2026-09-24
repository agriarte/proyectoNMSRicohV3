package com.tuempresa.nms.infrastructure.snmp;

import java.util.*;

public class SnmpResponse {

    private final Map<String, String> scalarValues;
    private final Map<String, List<TableRow>> tableWalks;

    public SnmpResponse() {
        this.scalarValues = new HashMap<>();
        this.tableWalks = new HashMap<>();
    }

    // Normaliza el OID quitando un punto inicial, para que dé igual si viene
    // de una constante ".1.3.6...." o de SNMP4J OID.toString() ("1.3.6....")
    private static String normalize(String oid) {
        if (oid == null) return null;
        return oid.startsWith(".") ? oid.substring(1) : oid;
    }

    public void putScalar(String oid, String value) {
        scalarValues.put(normalize(oid), value);
    }

    public String getString(String oid) {
        return scalarValues.get(normalize(oid));
    }

    public Long getLong(String oid) {
        String v = scalarValues.get(normalize(oid));
        if (v == null || v.isBlank()) return null;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getInteger(String oid) {
        String v = scalarValues.get(normalize(oid));
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void putTableRows(String baseOid, List<TableRow> rows) {
        tableWalks.put(baseOid, rows);
    }

    public List<TableRow> getTableRows(String baseOid) {
        return tableWalks.getOrDefault(baseOid, Collections.emptyList());
    }

    public Map<String, String> allScalars() {
        return Collections.unmodifiableMap(scalarValues);
    }

    public record TableRow(Map<String, String> columns) {
    }
}