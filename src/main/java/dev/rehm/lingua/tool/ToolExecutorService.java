package dev.rehm.lingua.tool;

import dev.rehm.lingua.agent.context.AgentExecutionContext;
import dev.rehm.lingua.agent.message.MessageFactory;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolCallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dev.rehm.lingua.tool.ToolExecutionTiming;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ToolExecutorService {

    private final ToolRegistry toolRegistry;
    private static final Logger log =
            LoggerFactory.getLogger(ToolExecutorService.class);

    public ToolExecutorService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }


    public List<ToolExecutionResult> toolCallingProcess(
            AgentExecutionContext context
    ) {
        List<OllamaToolCallResponse> calls =
                context.getResponse().message().toolCalls();

        int approved = context.getAgentExecutionBudget().reserveBudget(calls.size());
        List<ToolExecutionResult> results = new ArrayList<>();

        ExecutorService executor =
                Executors.newVirtualThreadPerTaskExecutor();

        try {
            ToolExecutionBatch toolExecutionBatch = new ToolExecutionBatch(approved);
            executeMultiTool(executor, toolExecutionBatch, calls, approved);

            for (int i = 0; i < calls.size(); i++) {

                ToolExecutionResult result;

                if (i < approved) {
                    try {

                        result = executeResult(toolExecutionBatch, i);

                    } catch (TimeoutException e) {
                        toolExecutionBatch.getFuture(i).cancel(true);

                        result = createErrorResult(
                                calls, toolExecutionBatch, i,
                                ToolExecutionStatus.TIMEOUT,
                                "Tool execution timed out"
                        );

                    } catch (InterruptedException e) {
                        toolExecutionBatch.getFutures().forEach(f -> f.cancel(true));
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Tool execution interrupted", e
                        );

                    } catch (ExecutionException e) {
                        result = createErrorResult(
                                calls, toolExecutionBatch, i,
                                ToolExecutionStatus.FAILED,
                                "Tool execution failed"
                        );
                    }
                } else {
                    result = createRejectedResult(calls, i);
                }
                results.add(result);
                saveToolExecutionMessage(context, result);
            }
        } finally {
            executor.shutdownNow();
        }

        return results;
    }


    public Object choiceTool(OllamaToolCallResponse toolCall) {
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


    private ToolExecutionResult executeTool(OllamaToolCallResponse toolCall, int index, Instant executedAt) {
        long start = System.nanoTime();

        String toolName = "UNKNOWN";
        Map<String, Object> arguments = null;

        Thread currentThread = Thread.currentThread();

        try {

            toolName = toolCall.function().name();

            arguments =
                    toolCall.function().arguments() == null
                            ? Map.of()
                            : new HashMap<>(toolCall.function().arguments());


            log.info(
                    "TOOL START: {} | thread={} | virtual={}",
                    toolName,
                    Thread.currentThread().threadId(),
                    Thread.currentThread().isVirtual()
            );

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

            log.debug(e.getMessage(), e);

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


    private void executeMultiTool(ExecutorService executor, ToolExecutionBatch toolExecutionBatch,
                                  List<OllamaToolCallResponse> calls, int approved) {

        for (int i = 0; i < approved; i++) {
            OllamaToolCallResponse call = calls.get(i);
            int index = i;

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            toolExecutionBatch.setDeadlinesNanos(i, deadline);

            toolExecutionBatch.addFuture(
                    executor.submit(() -> {
                        toolExecutionBatch.setToolThread(index, Thread.currentThread());
                        Instant startedAt = Instant.now();
                        toolExecutionBatch.setToolStartedAt(index, startedAt);


                        try {
                            return executeTool(call, index, startedAt);
                        } finally {
                            toolExecutionBatch.setFinishedAtNanos(index, System.nanoTime());
                        }
                    })
            );
        }
    }

    private ToolExecutionResult executeResult(ToolExecutionBatch toolExecutionBatch,
                                              int index) throws TimeoutException, ExecutionException, InterruptedException {

        long remainingNanos =
                toolExecutionBatch.getDeadlinesNanos(index) - System.nanoTime();


        ToolExecutionResult result = toolExecutionBatch.getFuture(index).get(
                Math.max(0, remainingNanos),
                TimeUnit.NANOSECONDS
        );

        if (toolExecutionBatch.getFinishedAtNanos(index) - toolExecutionBatch.getDeadlinesNanos(index) > 0) {
            throw new TimeoutException(
                    "Tool completed after deadline"
            );
        }

        return result;
    }


    private void saveToolExecutionMessage(AgentExecutionContext context, ToolExecutionResult result) {
        OllamaMessageDto toolMessage = MessageFactory.createToolMessageDto(result.toolName(), result);

        context.addPendingMessage(toolMessage);

    }


    private ToolExecutionResult createSkippedResult(List<OllamaToolCallResponse> calls, int index) {
        OllamaToolCallResponse call = calls.get(index);

        return new ToolExecutionResult(
                call.function().name(),
                call.function().arguments(),
                "Tool limit reached",
                ToolExecutionStatus.SKIPPED,
                Instant.now(),
                0,
                null,
                false,
                index
        );
    }

    private ToolExecutionResult createRejectedResult(
            List<OllamaToolCallResponse> calls,
            int index
    ) {
        OllamaToolCallResponse call = calls.get(index);

        if (call == null || call.function() == null
                || call.function().name() == null
                || call.function().name().isBlank()) {

            return createFailedResult(
                    calls,
                    index
            );
        }

        return createSkippedResult(calls, index);
    }

    private ToolExecutionResult createFailedResult(List<OllamaToolCallResponse> calls, int index) {
        OllamaToolCallResponse call = calls.get(index);

        return new ToolExecutionResult(
                "UNKNOW",
                null,
                "Tool functions not found",
                ToolExecutionStatus.FAILED,
                Instant.now(),
                0,
                null,
                false,
                index
        );
    }


    private ToolExecutionResult createErrorResult(
            List<OllamaToolCallResponse> calls,
            ToolExecutionBatch batch,
            int index,
            ToolExecutionStatus status,
            String errorMessage
    ) {
        ToolExecutionTiming timing =
                new ToolExecutionTiming(batch, index);

        Thread thread = batch.getToolThreads(index);

        return new ToolExecutionResult(
                calls.get(index).function().name(),
                calls.get(index).function().arguments(),
                errorMessage,
                status,
                timing.getExecutedAt(),
                timing.getDurationMs(),
                thread == null ? null : thread.threadId(),
                thread != null && thread.isVirtual(),
                index
        );
    }


}
