package dev.rehm.lingua.inspection.dto;

import dev.rehm.lingua.dto.response.OllamaChatResponse;

import java.util.List;

public record AgentExecutionResult(

        OllamaChatResponse response,
        int remainingToolCalls,
        int remainingIterations,
        List<ToolExecutionResult> toolExecutionResults,
        int toolCalls,
        int successful,
        int failed,
        AgentTerminationReason agentTerminationReason

) {
}
