package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class AgentProcessFailedException extends BaseException {

    public AgentProcessFailedException(
    String functionName,
    String fieldName,
    boolean done
    ) {
        super(
                functionName,
                fieldName,
                done,
                "Agent execution failed: Ollama returned an incomplete response (done=" + done + ").",
                "AGENT_INCOMPLETE_RESPONSE"
        );
    }
}
