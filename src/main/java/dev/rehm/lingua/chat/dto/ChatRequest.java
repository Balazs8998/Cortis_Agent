package dev.rehm.lingua.chat.dto;

import java.util.UUID;

public record ChatRequest(
        UUID conversationId,
        String message,
        String model,
        Boolean think
) {
}
