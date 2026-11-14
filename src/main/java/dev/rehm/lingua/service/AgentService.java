package dev.rehm.lingua.service;

import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.dto.response.tool.ToolCallResponse;
import dev.rehm.lingua.exception.classes.UnexpectedNullValueException;
import dev.rehm.lingua.execution.AgentExecutionBudget;
import dev.rehm.lingua.inspection.dto.AgentExecutionResult;
import dev.rehm.lingua.factory.MessageFactory;
import dev.rehm.lingua.factory.RequestFactory;
import dev.rehm.lingua.inspection.dto.AgentTerminationReason;
import dev.rehm.lingua.inspection.dto.ToolExecutionResult;
import dev.rehm.lingua.inspection.dto.ToolExecutionStatus;
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


    public AgentService(MessageService messageService, ToolExecutorService executorService, OllamaService ollamaService, RequestFactory requestFactory, ConversationService conversationService) {

        this.messageService = messageService;
        this.executorService = executorService;
        this.ollamaService = ollamaService;
        this.requestFactory = requestFactory;

        this.conversationService = conversationService;
    }

    public AgentExecutionResult executeAgentLoop(
            UUID uuid,
            String userMessage
    ) {

        logLock("WAITING",uuid);

        return conversationService.executeSerialized(
                uuid,
                () -> {
                    logLock("ACQUIRED",uuid);

                    try{
                      return runAgentLoop(uuid, userMessage);

                    }finally {
                        logLock("FINISHED", uuid);
                    }
                }
        );
    }

    private AgentExecutionResult runAgentLoop(
            UUID uuid,
            String userMessage
    ) {
        AgentExecutionBudget agentExecutionBudget = new AgentExecutionBudget(6,4);
        List<ToolExecutionResult> toolExecutionResultList = new ArrayList<>();
        boolean hasToolCalling = true;
        OllamaChatResponse response = null;

        OllamaMessageDto newMessage = MessageFactory.createUserMessageDto(userMessage);
        messageService.saveMessages(uuid, newMessage);

        System.out.println("-------start-----------");
        agentExecutionBudget.printLoopLimits();
        System.out.println("------------------");

        while (hasToolCalling) {
            List<OllamaMessageDto> messages = new ArrayList<>(conversationService.getMessageList(uuid));

            if (agentExecutionBudget.getRemainingIterations() <= 0) {
                AgentTerminationReason reason = AgentTerminationReason.ITERATION_LIMIT_REACHED;
                return finishAfterLimit(uuid,messages,toolExecutionResultList,agentExecutionBudget,reason);
            }

            OllamaChatRequest ollamaChatRequestWithTool = requestFactory.createRequestWithTool(messages);

            agentExecutionBudget.loopStep();




            response = agentMessage(ollamaChatRequestWithTool);

            if (!hasToolCalling(response)
                    && (response.message().content() == null
                    || response.message().content().isBlank())) {

                System.out.println("Empty final response. Retrying without tools.");

                response = agentMessage(
                        requestFactory.createLastBaseRequest(messages)
                );

                if (hasToolCalling(response)
                        || response.message().content() == null
                        || response.message().content().isBlank()) {

                    throw new IllegalStateException(
                            "Ollama returned an empty final response"
                    );
                }
            }




            messageService.saveMessages(uuid, response.message());
            hasToolCalling = hasToolCalling(response);

            if (hasToolCalling) {
                toolExecutionResultList.addAll(toolCallingProcess(uuid, response, agentExecutionBudget)) ;
                messages = new ArrayList<>(conversationService.getMessageList(uuid));
                for (ToolExecutionResult toolExecutionResult : toolExecutionResultList) {

                    if (toolExecutionResult.status() == ToolExecutionStatus.SKIPPED){
                        AgentTerminationReason reason = AgentTerminationReason.TOOL_LIMIT_REACHED;
                        return finishAfterLimit(uuid,messages,toolExecutionResultList,agentExecutionBudget,reason);
                    }
                }
            }
        }
        messageService.soutMessageList(conversationService.getMessageList(uuid));
        agentExecutionBudget.printLoopLimits();
        System.out.println("-------end-----------");




        return createExecutionResult(AgentTerminationReason.COMPLETED,agentExecutionBudget,response,toolExecutionResultList);
    }



    private OllamaChatResponse agentMessage(OllamaChatRequest request){

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

    private AgentExecutionResult finishAfterLimit(UUID uuid, List<OllamaMessageDto> messages, List<ToolExecutionResult> toolExecutionResultList, AgentExecutionBudget agentExecutionBudget, AgentTerminationReason reason){

        OllamaMessageDto iterationMessage = null;

        if (reason == AgentTerminationReason.ITERATION_LIMIT_REACHED) {
            iterationMessage =
                    MessageFactory.createSystemMessageDto("Agent iteration limit reached. "
                            + "Report confirmed results and identify incomplete tasks.");
        }else
            iterationMessage =
                    MessageFactory.createSystemMessageDto("Agent tool limit reached. "
                            + "Report confirmed results and identify incomplete tasks.");

        messages.add(iterationMessage);

        OllamaChatRequest ollamaChatRequest = requestFactory.createLastBaseRequest(messages);

        OllamaChatResponse response = agentMessage(ollamaChatRequest);
        messageService.saveMessages(uuid, response.message());

        messageService.soutMessageList(conversationService.getMessageList(uuid));
        agentExecutionBudget.printLoopLimits();
        System.out.println("-------end-----------");

        return createExecutionResult(reason,agentExecutionBudget,response,toolExecutionResultList);
    }

    private AgentExecutionResult createExecutionResult(AgentTerminationReason agentTerminationReason, AgentExecutionBudget agentExecutionBudget, OllamaChatResponse response, List<ToolExecutionResult> toolExecutionResults){

        int successful = 0;
        int failed = 0;

        int toolCalls = toolExecutionResults.size();
        for (ToolExecutionResult toolExecutionResult : toolExecutionResults) {
            if (toolExecutionResult.status() == ToolExecutionStatus.SUCCES){
                successful += 1;
            }
            if (toolExecutionResult.status() == ToolExecutionStatus.FAILED
                    || toolExecutionResult.status() == ToolExecutionStatus.SKIPPED
                    || toolExecutionResult.status() == ToolExecutionStatus.REJECTED
                    || toolExecutionResult.status() == ToolExecutionStatus.TIMEOUT){
                failed += 1;
            }


        }

        return new AgentExecutionResult(
                response.message().content(),
                agentExecutionBudget.getRemainingToolCall(),
                agentExecutionBudget.getRemainingIterations(),
                toolExecutionResults,
                toolCalls,
                successful,
                failed,
                agentTerminationReason
        );
    }

    private List<ToolExecutionResult> toolCallingProcess(
            UUID uuid,
            OllamaChatResponse response,
            AgentExecutionBudget budget
    ) {
        List<ToolCallResponse> calls =
                response.message().toolCalls();

        int approved = budget.reserveBudget(calls.size());

        List<ToolExecutionResult> results = new ArrayList<>();


        ExecutorService executor =
                Executors.newVirtualThreadPerTaskExecutor();

        try{

            long[] deadlinesNanos =
                    new long[approved];

            List<Future<ToolExecutionResult>> futures =
                    new ArrayList<>();

            AtomicReferenceArray<Instant> toolStartedAt =
                    new AtomicReferenceArray<>(approved);

            AtomicLongArray finishedAtNanos =
                    new AtomicLongArray(approved);

            AtomicReferenceArray<Thread> toolThreads =
                    new AtomicReferenceArray<>(approved);

            for (int i = 0; i < approved; i++) {
                ToolCallResponse call = calls.get(i);
                int index = i;

                deadlinesNanos[i] = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

                futures.add(
                        executor.submit(() -> {
                            toolThreads.set(index, Thread.currentThread());
                            Instant startedAt = Instant.now();
                            toolStartedAt.set(index, startedAt);


                            try {
                                return executorService.executeTool(call, index,startedAt);
                            } finally {
                                finishedAtNanos.set(index, System.nanoTime());
                            }
                        })
                );
            }

            for (int i = 0; i < calls.size(); i++) {

                ToolExecutionResult result;

                if (i < approved) {
                    try {

                        long remainingNanos =
                                deadlinesNanos[i] - System.nanoTime();


                        result = futures.get(i).get(
                                Math.max(0, remainingNanos),
                                TimeUnit.NANOSECONDS
                        );

                        if (finishedAtNanos.get(i) - deadlinesNanos[i] > 0) {
                            throw new TimeoutException(
                                    "Tool completed after deadline"
                            );
                        }

                    }catch (TimeoutException e){

                        long startNanos =
                                deadlinesNanos[i] - TimeUnit.SECONDS.toNanos(5);

                        long finishedNanos = finishedAtNanos.get(i);

                        long endNanos = finishedNanos != 0L
                                ? finishedNanos
                                : Math.min(System.nanoTime(), deadlinesNanos[i]);

                        double durationMs =
                                Math.max(0L, endNanos - startNanos) / 1_000_000.0;

                        Instant executedAt = toolStartedAt.get(i);

                        if (executedAt == null) {
                            executedAt = Instant.now();
                        }

                        futures.get(i).cancel(true);

                        ToolCallResponse call = calls.get(i);

                        Thread workerThread = toolThreads.get(i);

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

                    }catch (InterruptedException e) {
                        futures.forEach(f -> f.cancel(true));
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
                    ToolCallResponse call = calls.get(i);

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
                        uuid,
                        result.toolName(),
                        result.status() == ToolExecutionStatus.SUCCES
                                ? result.result()
                                : result.status() + ": " + result.result()
                );
            }
        }finally {
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

}
