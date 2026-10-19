package dev.rehm.lingua.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rehm.lingua.dto.OllamaMessageDto;

public record OllamaChatResponse(
        String model,
        OllamaMessageDto message,
        boolean done,
        @JsonProperty("done_reason")
        String doneReason,
        @JsonProperty("eval_count")
        Integer evalCount
) {
}
