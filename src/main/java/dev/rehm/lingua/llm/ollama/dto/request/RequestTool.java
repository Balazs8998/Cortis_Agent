package dev.rehm.lingua.llm.ollama.dto.request;

import dev.rehm.lingua.tool.schema.ToolRequestFunction;

public record RequestTool(
        String type,
        ToolRequestFunction function
) {
}
