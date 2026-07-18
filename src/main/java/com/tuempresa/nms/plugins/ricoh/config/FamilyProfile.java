package com.tuempresa.nms.plugins.ricoh.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FamilyProfile {

    @JsonProperty("features")
    public Features features;

    @JsonProperty("counters")
    public Counters counters;

    @JsonProperty("consumables")
    public Consumables consumables;

    public static class Features {
        public boolean color;
        public boolean multifunction;
        public boolean scanner;
        public String toner;
    }

    public static class Counters {
        @JsonProperty("total_black") public CounterEntry totalBlack;
        @JsonProperty("total_color") public CounterEntry totalColor;
        @JsonProperty("total_scan") public CounterEntry totalScan;
    }

    public static class CounterEntry {
        public String oid;
        public String description;
    }

    public static class Consumables {
        @JsonProperty("toner_level") public Map<String, TonerEntry> tonerLevel;
    }

    public static class TonerEntry {
        public String oid;
    }
}
