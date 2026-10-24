package dev.rehm.lingua.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequestOption(
        @JsonProperty("num_ctx")
        Integer numCtx,
        @JsonProperty("num_predict")
        Integer numPredict,
        Double temperature,
        @JsonProperty("top_p")
        Double topP,
        @JsonProperty("min_p")
        Double minP,
        @JsonProperty("top_k")
        Integer topK,
        @JsonProperty("repeat_penalty")
        Double repeatPenalty
) {
}
