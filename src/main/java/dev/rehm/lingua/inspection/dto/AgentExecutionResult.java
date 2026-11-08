package dev.rehm.lingua.inspection.dto;

import java.util.List;

public record AgentExecutionResult(

        String message,
        int remainingToolCalls,
        int remainingIterations,
        List<ToolExecutionResult> toolExecutionResults,
        int toolCalls,
        int successful,
        int failed,
        AgentTerminationReason agentTerminationReason

) {
}
