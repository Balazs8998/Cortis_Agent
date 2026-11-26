package dev.rehm.lingua.chat.dto;

import dev.rehm.lingua.agent.inspection.AgentResponseIssue;
import dev.rehm.lingua.agent.inspection.AgentTerminationReason;
import dev.rehm.lingua.tool.ToolExecutionResult;

import java.util.List;
import java.util.Set;
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
        AgentTerminationReason agentTerminationReason,
        Set<AgentResponseIssue> agentResponseIssues,
        String doneReason,
        Integer evalCount,
        Long promptEvalCount,
        Long evalDuration
) {
}
