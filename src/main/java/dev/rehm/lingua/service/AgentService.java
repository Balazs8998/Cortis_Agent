package dev.rehm.lingua.service;

import dev.rehm.lingua.argument.ArgumentHandler;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.ChatResponse;
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
import dev.rehm.lingua.memory.ConversationMemory;
import dev.rehm.lingua.registry.ToolRegistry;
import dev.rehm.lingua.tool.AgentTool;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;


@Service
public class AgentService {

    private final OllamaService ollamaService;
    private final MessageService messageService;

    private final RequestFactory requestFactory;
    private final ToolRegistry toolRegistry;
    private final ConversationService conversationService;


    public AgentService(MessageService messageService, ToolRegistry toolRegistry, OllamaService ollamaService, RequestFactory requestFactory, ConversationService conversationService) {

        this.messageService = messageService;

        this.ollamaService = ollamaService;
        this.requestFactory = requestFactory;
        this.toolRegistry = toolRegistry;
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

            OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(messages);
            OllamaChatRequest ollamaChatRequestWithTool = requestFactory.addToolsToRequest(ollamaChatRequest);

            agentExecutionBudget.decrementRemainingIterations();
            agentExecutionBudget.incrementIterations();

            response = agentMessage(ollamaChatRequestWithTool);

            messageService.saveMessages(uuid, response.message());
            hasToolCalling = hasToolCalling(response);

            if (hasToolCalling) {
                toolExecutionResultList.addAll(toolCallingProcess(uuid, response, agentExecutionBudget)) ;
            }
        }
        ollamaService.soutMessageList(conversationService.getMessageList(uuid));
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

        ollamaService.soutMessageList(conversationService.getMessageList(uuid));
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



    private Object choiceTool(UUID uuid,ToolCallResponse toolCallResponse) {
        String toolName = toolCallResponse.function().name();
        Map<String, Object> arguments = toolCallResponse.function().arguments();

        AgentTool tool = toolRegistry.getTool(toolName);


        Object result = tool.execute(arguments);
        messageService.createAndSaveToolMessage(uuid, toolName, result);

        return result;

    }

    private List<ToolExecutionResult> toolCallingProcess(
            UUID uuid,
            OllamaChatResponse response,
            AgentExecutionBudget budget
    ) {
        List<ToolExecutionResult> results = new ArrayList<>();

        for (ToolCallResponse toolCall : response.message().toolCalls()) {

            String toolName = toolCall.function().name();
            Map<String, Object> arguments =
                    new HashMap<>(toolCall.function().arguments());

            Instant executedAt = Instant.now();
            long start = System.nanoTime();

            if (budget.getRemainingToolCall() <= 0) {

                messageService.saveToolCallLimitExceededMessage(uuid, toolName);

                double durationMs =
                        (System.nanoTime() - start) / 1_000_000.0;

                results.add(new ToolExecutionResult(
                        toolName,
                        arguments,
                        "Tool execution denied: limit reached",
                        ToolExecutionStatus.SKIPPED,
                        executedAt,
                        durationMs
                ));

                continue;
            }

            Object result = choiceTool(uuid, toolCall);

            budget.decrementRemainingTools();

            double durationMs =
                    (System.nanoTime() - start) / 1_000_000.0;

            results.add(new ToolExecutionResult(
                    toolName,
                    arguments,
                    result,
                    ToolExecutionStatus.SUCCES,
                    executedAt,
                    durationMs
            ));
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
