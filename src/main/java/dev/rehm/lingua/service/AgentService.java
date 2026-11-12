package dev.rehm.lingua.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import dev.rehm.lingua.service.ToolExecutorService;
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



    public AgentExecutionResult executeAgentLoop(UUID uuid, String userMessage) {

        AgentExecutionBudget agentExecutionBudget = new AgentExecutionBudget(3,2);
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
            return finishAfterIterationLimit(uuid,messages,toolExecutionResultList,agentExecutionBudget);
            }

            OllamaChatRequest ollamaChatRequestWithTool = requestFactory.createRequestWithTool(messages);

            agentExecutionBudget.loopStep();

            response = agentMessage(ollamaChatRequestWithTool);

            messageService.saveMessages(uuid, response.message());
            hasToolCalling = hasToolCalling(response);

            if (hasToolCalling) {
                toolExecutionResultList.addAll(toolCallingProcess(uuid, response, agentExecutionBudget)) ;
            }
        }
        messageService.soutMessageList(conversationService.getMessageList(uuid));
        agentExecutionBudget.printLoopLimits();
        System.out.println("-------end-----------");

        for (ToolExecutionResult toolExecutionResult : toolExecutionResultList) {

            if (toolExecutionResult.status() == ToolExecutionStatus.SKIPPED){
                return createExecutionResult(AgentTerminationReason.TOOL_LIMIT_REACHED,agentExecutionBudget,response,toolExecutionResultList);
            }
        }
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

        return response;

    }

    private AgentExecutionResult finishAfterIterationLimit(UUID uuid, List<OllamaMessageDto> messages, List<ToolExecutionResult> toolExecutionResultList, AgentExecutionBudget agentExecutionBudget){
        OllamaMessageDto iterationMessage =
                MessageFactory.createSystemMessageDto("Agent iteration limit reached. "
                        + "Report confirmed results and identify incomplete tasks.");

        messages.add(iterationMessage);

        OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(messages);

        OllamaChatResponse response = agentMessage(ollamaChatRequest);
        messageService.saveMessages(uuid, response.message());

        messageService.soutMessageList(conversationService.getMessageList(uuid));
        agentExecutionBudget.printLoopLimits();
        System.out.println("-------end-----------");

        return createExecutionResult(AgentTerminationReason.ITERATION_LIMIT_REACHED,agentExecutionBudget,response,toolExecutionResultList);
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
                    || toolExecutionResult.status() == ToolExecutionStatus.REJECTED){
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

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<ToolExecutionResult>> futures =
                    new ArrayList<>();

            for (int i = 0; i < approved; i++) {
                ToolCallResponse call = calls.get(i);
                int index = i;

                futures.add(
                        executor.submit(() -> executorService.executeTool(call,index))
                );
            }

            for (int i = 0; i < calls.size(); i++) {

                ToolExecutionResult result;

                if (i < approved) {
                    try {
                        result = futures.get(i).get();

                    } catch (InterruptedException e) {
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
        }

        return results;
    }

    private @NotNull boolean hasToolCalling(OllamaChatResponse response) {
        if (response.message().toolCalls() == null) {
            return false;
        }
        return !response.message().toolCalls().isEmpty();
    }

}
