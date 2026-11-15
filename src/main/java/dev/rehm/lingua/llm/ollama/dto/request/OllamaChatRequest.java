package dev.rehm.lingua.llm.ollama.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        @NotEmpty
        List<OllamaMessageDto>messages,
        List<RequestTool> tools,
        String format,
        RequestOption options,
        boolean stream,
        Object think,
        @JsonProperty("keep_alive")
        Integer keepAlive
) {
}
