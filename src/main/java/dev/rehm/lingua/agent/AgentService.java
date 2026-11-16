package dev.rehm.lingua.agent;

import java.util.concurrent.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import dev.rehm.lingua.agent.context.AgentExecutionContext;
import dev.rehm.lingua.agent.inspection.AgentExecutionResult;
import dev.rehm.lingua.agent.inspection.AgentResponseIssue;
import dev.rehm.lingua.agent.inspection.AgentTerminationReason;
import dev.rehm.lingua.agent.iteration.AgentExecutionBudget;
import dev.rehm.lingua.conversation.ConversationService;
import dev.rehm.lingua.conversation.MessageService;
import dev.rehm.lingua.llm.LlmProfileService;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.LlmProfiles;
import dev.rehm.lingua.chat.dto.ChatRequest;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolCallResponse;
import dev.rehm.lingua.common.exception.classes.UnexpectedNullValueException;
import dev.rehm.lingua.conversation.MessageFactory;
import dev.rehm.lingua.llm.ollama.RequestFactory;
import dev.rehm.lingua.llm.ollama.OllamaService;
import dev.rehm.lingua.tool.ToolExecutionBatch;
import dev.rehm.lingua.tool.ToolExecutionResult;
import dev.rehm.lingua.tool.ToolExecutionStatus;
import dev.rehm.lingua.tool.ToolExecutorService;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;


@Service
public class AgentService {

    private final ToolExecutorService executorService;
    private final OllamaService ollamaService;
    private final MessageService messageService;
    private final RequestFactory requestFactory;
    private final ConversationService conversationService;
    private final LlmProfileService llmProfileService;


    public AgentService(MessageService messageService, ToolExecutorService executorService, OllamaService ollamaService, RequestFactory requestFactory, ConversationService conversationService, LlmProfileService llmProfileService) {

        this.messageService = messageService;
        this.executorService = executorService;
        this.ollamaService = ollamaService;
        this.requestFactory = requestFactory;

        this.conversationService = conversationService;
        this.llmProfileService = llmProfileService;

    }

    public AgentExecutionResult executeAgentLoop(
            ChatRequest request
    ) {

        logLock("WAITING", request.conversationId());

        return conversationService.executeSerialized(
                request.conversationId(),
                () -> {
                    logLock("ACQUIRED", request.conversationId());

                    try {
                        return runAgentLoop(request);

                    } finally {
                        logLock("FINISHED", request.conversationId());
                    }
                }
        );
    }

    private AgentExecutionResult runAgentLoop(
            ChatRequest request
    ) {
        AgentExecutionContext context =
                new AgentExecutionContext(request, choiceLlm(request));

        messageService.saveMessages(context.getUuid(), context.getNewMessage());
        logIterationBudget(context);

        while (context.getHasToolCalling()) {
            List<OllamaMessageDto> messages = new ArrayList<>(conversationService.getMessageList(context.getUuid()));

            if (context.getAgentExecutionBudget().getRemainingIterations() <= 0) {
                AgentTerminationReason reason = AgentTerminationReason.ITERATION_LIMIT_REACHED;
                return finishAfterLimit( messages, reason, context);
            }

            executeAgentIteration(messages, context);

            if (context.getHasToolCalling()) {
                context.addToolResults(toolCallingProcess(context));

                messages = new ArrayList<>(conversationService.getMessageList(context.getUuid()));
                for (ToolExecutionResult toolExecutionResult : context.getResults()) {

                    if (toolExecutionResult.status() == ToolExecutionStatus.SKIPPED) {
                        AgentTerminationReason reason = AgentTerminationReason.TOOL_LIMIT_REACHED;
                        return finishAfterLimit( messages, reason, context);                    }
                }
            }
        }
        messageService.soutMessageList(conversationService.getMessageList(context.getUuid()));
        logIterationBudget(context);

        return createExecutionResult(AgentTerminationReason.COMPLETED, context.getAgentExecutionBudget(), context.getResponse(), context.getResults());
    }

    private LlmProfiles choiceLlm(ChatRequest request) {
        String model = request.model();

        if (model == null || model.isBlank()) {
            model = "qwen3.5:4b";
        }

        boolean think = request.think() == null || request.think();

        return llmProfileService.createLlmProfiles(model, think);
    }


    private void retryEmptyFinalResponse(AgentExecutionContext context, List<OllamaMessageDto> messages) {


        if (!hasToolCalling(context.getResponse())
                && (context.getResponse().message().content() == null
                || context.getResponse().message().content().isBlank())) {

            System.out.println("Empty final response. Retrying without tools.");

            context.setResponse(agentMessage(
                    requestFactory.createBaseRequest(messages, context.getLlmProfiles().finalProfile())));


            if (hasToolCalling(context.getResponse())
                    || context.getResponse().message().content() == null
                    || context.getResponse().message().content().isBlank()) {

                throw new IllegalStateException(
                        "Ollama returned an empty final response"
                );
            }
        }

    }

    private void executeAgentIteration(List<OllamaMessageDto> messages, AgentExecutionContext context) {
        OllamaChatRequest ollamaChatRequestWithTool = requestFactory.createRequestWithTool(messages, context.getLlmProfiles().agentProfile());

        context.getAgentExecutionBudget().loopStep();

        context.setResponse(agentMessage(ollamaChatRequestWithTool));

        retryEmptyFinalResponse(context, messages);

        messageService.saveMessages(context.getUuid(), context.getResponse().message());
        context.setHasToolCalling(hasToolCalling(context.getResponse()));
    }


    private OllamaChatResponse agentMessage(OllamaChatRequest request) {

        OllamaChatResponse response = ollamaService.messageSend(request);

        if (response == null) {
            throw new UnexpectedNullValueException(
                    "OllamaResponse",
                    "response",
                    null,
                    "Ollama returned a null response.",
                    "OLLAMA_RESPONSE_NULL"
            );
        }

        if (response.message() == null) {
            throw new UnexpectedNullValueException(
                    "OllamaResponse",
                    "message",
                    null,
                    "Ollama response does not contain a message.",
                    "OLLAMA_MESSAGE_NULL"
            );
        }


        System.out.printf(
                "OLLAMA DEBUG | done=%s | reason=%s | eval=%s | content='%s' | toolCalls=%s%n",
                response.done(),
                response.doneReason(),
                response.evalCount(),
                response.message().content(),
                response.message().toolCalls()
        );

        return response;

    }

    private AgentExecutionResult finishAfterLimit(List<OllamaMessageDto> messages,
                                                  AgentTerminationReason reason,
                                                  AgentExecutionContext context) {

        String message = reason == AgentTerminationReason.ITERATION_LIMIT_REACHED
                ? "Agent iteration limit reached. "
                : "Agent tool limit reached. ";

        messages.add(MessageFactory.createSystemMessageDto(
                message + "Report confirmed results and identify incomplete tasks."
        ));

        OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(messages, context.getLlmProfiles().finalProfile());

        context.setResponse(agentMessage(ollamaChatRequest));

        messageService.saveMessages(
                context.getUuid(),
                context.getResponse().message()
        );



        messageService.soutMessageList(conversationService.getMessageList(context.getUuid()));
        logIterationBudget(context);
        System.out.println("-------end-----------");

        return createExecutionResult(reason, context.getAgentExecutionBudget(), context.getResponse(), context.getResults());
    }

    private AgentExecutionResult createExecutionResult(AgentTerminationReason agentTerminationReason, AgentExecutionBudget agentExecutionBudget, OllamaChatResponse response, List<ToolExecutionResult> toolExecutionResults) {

        int successful = 0;
        int failed = 0;

        int toolCalls = toolExecutionResults.size();
        for (ToolExecutionResult toolExecutionResult : toolExecutionResults) {
            if (toolExecutionResult.status() == ToolExecutionStatus.SUCCES) {
                successful += 1;
            }
            if (toolExecutionResult.status() == ToolExecutionStatus.FAILED
                    || toolExecutionResult.status() == ToolExecutionStatus.SKIPPED
                    || toolExecutionResult.status() == ToolExecutionStatus.REJECTED
                    || toolExecutionResult.status() == ToolExecutionStatus.TIMEOUT) {
                failed += 1;
            }


        }

        Set<AgentResponseIssue> issues =
                EnumSet.noneOf(AgentResponseIssue.class);

        if ("length".equals(response.doneReason())) {
            issues.add(AgentResponseIssue.MODEL_OUTPUT_TRUNCATED);
        }

        if (response.message().content() == null
                || response.message().content().isBlank()) {
            issues.add(AgentResponseIssue.MODEL_RESPONSE_EMPTY);
        }

        return new AgentExecutionResult(
                response,
                agentExecutionBudget.getRemainingToolCall(),
                agentExecutionBudget.getRemainingIterations(),
                toolExecutionResults,
                toolCalls,
                successful,
                failed,
                agentTerminationReason,
                issues
        );
    }

    private List<ToolExecutionResult> toolCallingProcess(
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


            for (int i = 0; i < approved; i++) {
                OllamaToolCallResponse call = calls.get(i);
                int index = i;

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                toolExecutionBatch.setDeadlinesNanos(i,deadline);

                toolExecutionBatch.addFuture(
                        executor.submit(() -> {
                            toolExecutionBatch.setToolThread(index,Thread.currentThread());
                            Instant startedAt = Instant.now();
                            toolExecutionBatch.setToolStartedAt(index, startedAt);


                            try {
                                return executorService.executeTool(call, index, startedAt);
                            } finally {
                                toolExecutionBatch.setFinishedAtNanos(index,System.nanoTime());
                            }
                        })
                );
            }

            for (int i = 0; i < calls.size(); i++) {

                ToolExecutionResult result;

                if (i < approved) {
                    try {

                        long remainingNanos =
                                toolExecutionBatch.getDeadlinesNanos(i) - System.nanoTime();


                        result = toolExecutionBatch.getFutures().get(i).get(
                                Math.max(0, remainingNanos),
                                TimeUnit.NANOSECONDS
                        );

                        if (toolExecutionBatch.getFinishedAtNanos(i) - toolExecutionBatch.getDeadlinesNanos(i) > 0) {
                            throw new TimeoutException(
                                    "Tool completed after deadline"
                            );
                        }

                    } catch (TimeoutException e) {

                        long startNanos =
                                toolExecutionBatch.getDeadlinesNanos(i) - TimeUnit.SECONDS.toNanos(5);

                        long finishedNanos = toolExecutionBatch.getFinishedAtNanos(i);

                        long endNanos = finishedNanos != 0L
                                ? finishedNanos
                                : Math.min(System.nanoTime(), toolExecutionBatch.getDeadlinesNanos(i));

                        double durationMs =
                                Math.max(0L, endNanos - startNanos) / 1_000_000.0;

                        Instant executedAt = toolExecutionBatch.getToolStartedAt(i);

                        if (executedAt == null) {
                            executedAt = Instant.now();
                        }

                        toolExecutionBatch.getFutures().get(i).cancel(true);

                        OllamaToolCallResponse call = calls.get(i);

                        Thread workerThread = toolExecutionBatch.getToolThreads(i);

                        Long threadId = workerThread == null
                                ? null
                                : workerThread.threadId();

                        boolean isVirtual = workerThread != null
                                && workerThread.isVirtual();


                        result = new ToolExecutionResult(
                                call.function().name(),
                                call.function().arguments(),
                                "Tool execution timed out",
                                ToolExecutionStatus.TIMEOUT,
                                executedAt,
                                durationMs,
                                threadId,
                                isVirtual,
                                i
                        );

                    } catch (InterruptedException e) {
                        toolExecutionBatch.getFutures().forEach(f -> f.cancel(true));
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Tool execution interrupted", e
                        );

                    } catch (ExecutionException e) {
                        throw new IllegalStateException(
                                "Unexpected tool execution failure",
                                e.getCause()
                        );
                    }

                } else {
                    OllamaToolCallResponse call = calls.get(i);

                    result = new ToolExecutionResult(
                            call.function().name(),
                            call.function().arguments(),
                            "Tool limit reached",
                            ToolExecutionStatus.SKIPPED,
                            Instant.now(),
                            0,
                            null,
                            false,
                            i
                    );
                }

                results.add(result);

                messageService.createAndSaveToolMessage(
                        context.getUuid(),
                        result.toolName(),
                        result.status() == ToolExecutionStatus.SUCCES
                                ? result.result()
                                : result.status() + ": " + result.result()
                );
            }
        } finally {
            executor.shutdownNow();
        }

        return results;
    }

    private @NotNull boolean hasToolCalling(OllamaChatResponse response) {
        if (response.message().toolCalls() == null) {
            return false;
        }
        return !response.message().toolCalls().isEmpty();
    }

    private void logLock(String status, UUID uuid) {
        System.out.printf(
                "%s | UUID: %s | Thread: %d | Time: %s%n",
                status,
                uuid,
                Thread.currentThread().threadId(),
                Instant.now()
        );
    }

    private void logIterationBudget(AgentExecutionContext context) {
        System.out.println("------------------");
        context.getAgentExecutionBudget().printLoopLimits();
        System.out.println("------------------");

    }

}
