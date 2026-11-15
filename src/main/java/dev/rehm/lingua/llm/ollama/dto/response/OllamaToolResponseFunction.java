package dev.rehm.lingua.llm.ollama.dto.response;

import java.util.Map;


public record OllamaToolResponseFunction(
        Integer index,
        String name,
        Map<String, Object> arguments
) {
}
