package dev.rehm.lingua.dto.request.tool;

public record ToolRequestFunction(
        String name,
        String description,
        ToolRequestParameters parameters
) {
}
