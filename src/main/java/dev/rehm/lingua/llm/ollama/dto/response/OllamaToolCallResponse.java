package dev.rehm.lingua.llm.ollama.dto.response;

public record OllamaToolCallResponse(
        String type,
        OllamaToolResponseFunction function
) {
}
