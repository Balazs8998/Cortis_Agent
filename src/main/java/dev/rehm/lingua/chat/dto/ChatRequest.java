package dev.rehm.lingua.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatRequest(
        @NotNull(message = "Conversation ID must not be null.")
        UUID conversationId,

        @NotBlank(message = "Message must not be null, empty, or blank.")
        @Size(
                min = 1,
                max = 10000,
                message = "Message length must be between 1 and 10000 characters."
        )
        String message,

        String model,
        Boolean think
) {
}