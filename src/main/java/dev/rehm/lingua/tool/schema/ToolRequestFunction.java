package dev.rehm.lingua.tool.schema;

public record ToolRequestFunction(
        String name,
        String description,
        ToolRequestParameters parameters
) {
}
