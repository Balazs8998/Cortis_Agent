package dev.rehm.lingua.dto.request;

import dev.rehm.lingua.dto.OllamaMessageDto;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OllamaChatRequest(
        String model,
        @NotEmpty
        List<OllamaMessageDto>messages,
        boolean stream
) {
}
