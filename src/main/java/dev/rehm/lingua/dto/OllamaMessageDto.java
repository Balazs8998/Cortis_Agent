package dev.rehm.lingua.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rehm.lingua.dto.response.tool.ToolCallResponse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaMessageDto(
        String role,
        @JsonProperty("tool_name")
        String toolName,
        String content,
        @JsonProperty("tool_calls")
        List<ToolCallResponse> toolCalls
) {
}
