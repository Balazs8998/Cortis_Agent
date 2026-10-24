package dev.rehm.lingua.dto.request;

import dev.rehm.lingua.dto.request.tool.ToolRequestFunction;

public record RequestTool(
        String type,
        ToolRequestFunction function
) {
}
