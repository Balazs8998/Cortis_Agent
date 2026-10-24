package dev.rehm.lingua.dto.response.tool;

public record ToolCallResponse(
        String type,
        ToolResponseFunction function
) {
}
