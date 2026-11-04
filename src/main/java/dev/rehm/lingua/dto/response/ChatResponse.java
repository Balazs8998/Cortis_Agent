package dev.rehm.lingua.dto.response;

import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        String message
) {
}
