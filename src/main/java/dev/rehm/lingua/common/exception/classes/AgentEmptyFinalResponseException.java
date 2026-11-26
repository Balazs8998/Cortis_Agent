package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class AgentEmptyFinalResponseException extends BaseException {

    public AgentEmptyFinalResponseException(
            String functionName,
            String fieldName,
            String content
    ) {
        super(
                functionName,
                fieldName,
                content,
                "Agent finalization failed: Ollama returned an empty final response.",
                "AGENT_EMPTY_FINAL_RESPONSE"
        );
    }
}