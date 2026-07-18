package com.tuempresa.nms.plugins.ricoh.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class RicohConfigLoader {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @SuppressWarnings("unchecked")
    public Map<String, FamilyProfile> loadFamilies(Resource oidFile) throws IOException {
        Map<String, Object> raw = yamlMapper.readValue(oidFile.getInputStream(), Map.class);
        Map<String, FamilyProfile> families = new HashMap<>();

        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("device_") || key.equals("status") ||
                key.equals("standard_mib_references") || key.startsWith("#")) {
                continue;
            }
            if (key.matches("ME\\d+")) {
                families.put(key, yamlMapper.convertValue(entry.getValue(), FamilyProfile.class));
            }
        }
        return families;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> loadModelMappings(Resource modelFile) throws IOException {
        Map<String, Object> raw = yamlMapper.readValue(modelFile.getInputStream(), Map.class);
        Map<String, String> mappings = new HashMap<>();

        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey().trim();
            if (key.startsWith("#")) continue;
            Object val = entry.getValue();
            if (val != null) {
                mappings.put(key, val.toString().trim());
            }
        }
        return mappings;
    }
}