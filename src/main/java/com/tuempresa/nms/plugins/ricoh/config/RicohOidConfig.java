package com.tuempresa.nms.plugins.ricoh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RicohOidConfig {

    @JsonProperty("device_identification")
    public DeviceIdentification deviceIdentification;

    @JsonProperty("status")
    public Status status;

    @JsonProperty("standard_mib_references")
    public StandardMibReferences standardMibReferences;

    public static class DeviceIdentification {
        @JsonProperty("model_name") public OidEntry modelName;
        @JsonProperty("display_name") public OidEntry displayName;
        @JsonProperty("serial_number") public OidEntry serialNumber;
    }

    public static class OidEntry {
        public String oid;
        public String description;
    }

    public static class Status {
        @JsonProperty("copier_status") public OidEntry copierStatus;
        @JsonProperty("scan_status") public OidEntry scanStatus;
    }

    public static class StandardMibReferences {
        @JsonProperty("paper_tray_level") public TableOid paperTrayLevel;
        @JsonProperty("physical_supplies") public TableOid physicalSupplies;
    }

    public static class TableOid {
        @JsonProperty("current_level_oid") public String currentLevelOid;
        @JsonProperty("max_capacity_oid") public String maxCapacityOid;
        @JsonProperty("description_oid") public String descriptionOid;
    }
}
