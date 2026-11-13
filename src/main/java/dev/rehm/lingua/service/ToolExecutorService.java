package dev.rehm.lingua.service;

import dev.rehm.lingua.dto.response.tool.ToolCallResponse;
import dev.rehm.lingua.inspection.dto.ToolExecutionResult;
import dev.rehm.lingua.inspection.dto.ToolExecutionStatus;
import dev.rehm.lingua.registry.ToolRegistry;
import dev.rehm.lingua.tool.AgentTool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Service
public class ToolExecutorService {

    private final ToolRegistry toolRegistry;

    public ToolExecutorService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }


    private Object choiceTool(ToolCallResponse toolCall) {
        String toolName = toolCall.function().name();
        Map<String, Object> arguments =
                toolCall.function().arguments();

        AgentTool tool = toolRegistry.getTool(toolName);

        if (tool == null) {
            throw new IllegalArgumentException(
                    "Unknown tool: " + toolName
            );
        }

        return tool.execute(arguments);
    }

    public ToolExecutionResult executeTool(ToolCallResponse toolCall, int index, Instant executedAt) {
        long start = System.nanoTime();

        String toolName = toolCall.function().name();

        System.out.printf(
                "TOOL START: %s | thread=%s | virtual=%s%n",
                toolName,
                Thread.currentThread().threadId(),
                Thread.currentThread().isVirtual()
        );

        Thread currentThread = Thread.currentThread();

        Map<String, Object> arguments =
                toolCall.function().arguments() == null
                        ? Map.of()
                        : new HashMap<>(toolCall.function().arguments());

        try {
            Object result = choiceTool(toolCall);

            return new ToolExecutionResult(
                    toolName,
                    arguments,
                    result,
                    ToolExecutionStatus.SUCCES,
                    executedAt,
                    (System.nanoTime() - start) / 1_000_000.0,
                    currentThread.threadId(),
                    currentThread.isVirtual(),
                    index

            );

        } catch (Exception e) {

            return new ToolExecutionResult(
                    toolName,
                    arguments,
                    e.getMessage(),
                    ToolExecutionStatus.FAILED,
                    executedAt,
                    (System.nanoTime() - start) / 1_000_000.0,
                    currentThread.threadId(),
                    currentThread.isVirtual(),
                    index
            );
        }
    }

}
