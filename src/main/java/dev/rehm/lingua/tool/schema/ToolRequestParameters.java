package dev.rehm.lingua.tool.schema;

import java.util.List;
import java.util.Map;

public record ToolRequestParameters(
        String type,
        Map<String, ToolRequestProperties> properties,
        List<String> required

) {
}
