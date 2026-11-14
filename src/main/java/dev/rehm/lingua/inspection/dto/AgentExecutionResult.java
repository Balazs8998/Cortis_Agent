package dev.rehm.lingua.inspection.dto;

import dev.rehm.lingua.dto.response.OllamaChatResponse;

import java.util.List;
import java.util.Set;

public record AgentExecutionResult(

        OllamaChatResponse response,
        int remainingToolCalls,
        int remainingIterations,
        List<ToolExecutionResult> toolExecutionResults,
        int toolCalls,
        int successful,
        int failed,
        AgentTerminationReason agentTerminationReason,
        Set<AgentResponseIssue> agentResponseIssues

) {
}
