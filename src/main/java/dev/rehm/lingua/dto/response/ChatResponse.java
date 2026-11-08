package dev.rehm.lingua.dto.response;

import dev.rehm.lingua.inspection.dto.AgentTerminationReason;
import dev.rehm.lingua.inspection.dto.ToolExecutionResult;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
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
