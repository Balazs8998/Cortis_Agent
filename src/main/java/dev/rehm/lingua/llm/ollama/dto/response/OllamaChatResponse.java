package dev.rehm.lingua.llm.ollama.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OllamaChatResponse(
        String model,
        OllamaMessageDto message,
        boolean done,
        @JsonProperty("done_reason")
        String doneReason,
        @JsonProperty("eval_count")
        Integer evalCount,
        @JsonProperty("prompt_eval_count")
         Long promptEvalCount,
        @JsonProperty("eval_duration")
         Long evalDuration


) {
}
