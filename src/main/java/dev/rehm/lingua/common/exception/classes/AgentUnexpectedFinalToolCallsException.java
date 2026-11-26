package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class AgentUnexpectedFinalToolCallsException extends BaseException {

    public AgentUnexpectedFinalToolCallsException(
            String functionName,
            String fieldName,
            int toolCallCount
    ) {
        super(
                functionName,
                fieldName,
                toolCallCount,
                "Agent finalization failed: Unexpected tool calls in final response. Count: " + toolCallCount,
                "AGENT_UNEXPECTED_FINAL_TOOL_CALLS"
        );
    }
}