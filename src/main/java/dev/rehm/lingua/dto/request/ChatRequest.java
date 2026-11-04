package dev.rehm.lingua.dto.request;

import java.util.UUID;

public record ChatRequest(
        UUID conversationId,
        String message
) {
}
