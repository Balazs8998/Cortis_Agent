package dev.rehm.lingua.dto.response.tool;

import java.util.Map;


public record ToolResponseFunction(
        Integer index,
        String name,
        Map<String, Object> arguments
) {
}
