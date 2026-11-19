package dev.rehm.lingua.common.util;


import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class JsonMapperUtil {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .build();

    private JsonMapperUtil() {
    }

    public static String toJson(Object object) {
        return MAPPER.writeValueAsString(object);
    }
}