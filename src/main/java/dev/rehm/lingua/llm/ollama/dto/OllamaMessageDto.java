package dev.rehm.lingua.llm.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolCallResponse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaMessageDto(
        String role,
        @JsonProperty("tool_name")
        String toolName,
        String content,
        @JsonProperty("tool_calls")
        List<OllamaToolCallResponse> toolCalls
) {
}
