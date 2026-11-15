package dev.rehm.lingua.agent.inspection;

import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.tool.ToolExecutionResult;

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
