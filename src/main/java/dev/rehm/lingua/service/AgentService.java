package dev.rehm.lingua.service;

import dev.rehm.lingua.argument.ArgumentHandler;
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
    private final ConversationMemory memory;
    private final ArgumentHandler argumentHandler;
    private final RequestFactory requestFactory;
    private final ToolRegistry toolRegistry;
    private final ConversationService conversationService;


    public AgentService(MessageService messageService, ConversationMemory memory, ToolRegistry toolRegistry, OllamaService ollamaService, ArgumentHandler argumentHandler, RequestFactory requestFactory, ConversationService conversationService) {

        this.messageService = messageService;
        this.memory = memory;

        this.ollamaService = ollamaService;
        this.argumentHandler = argumentHandler;
        this.requestFactory = requestFactory;
        this.toolRegistry = toolRegistry;
        this.conversationService = conversationService;
    }

    public AgentExecutionResult executeAgentLoop(UUID uuid, String userMessage) {

        AgentExecutionBudget agentExecutionBudget = new AgentExecutionBudget(4,2);

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

                OllamaMessageDto iterationMessage =
                        MessageFactory.createSystemMessageDto("Agent iteration limit reached. "
                                + "Report confirmed results and identify incomplete tasks.");

                 messages.add(iterationMessage);

                OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(messages);

                response = agentMessage(ollamaChatRequest);
                messageService.saveMessages(uuid, response.message());


                ollamaService.soutMessageList(conversationService.getMessageList(uuid));
                agentExecutionBudget.printLoopLimits();
                System.out.println("-------end-----------");


                return createResponse(response,agentExecutionBudget,toolExecutionResultList,AgentTerminationReason.ITERATION_LIMIT_REACHED);
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

                return createResponse(response,agentExecutionBudget,toolExecutionResultList, AgentTerminationReason.TOOL_LIMIT_REACHED);


            }
        }



        return createResponse(response,agentExecutionBudget,toolExecutionResultList, AgentTerminationReason.COMPLETED);
    }

    private AgentExecutionResult createResponse(
            OllamaChatResponse response,
            AgentExecutionBudget budget,
            List<ToolExecutionResult> executionResults,
            AgentTerminationReason terminationReason
    ) {

        int successful = 0;
        int failed = 0;


        int toolCalls = executionResults.size();
        for (ToolExecutionResult toolExecutionResult : executionResults) {
            if (toolExecutionResult.status() == ToolExecutionStatus.SUCCES){
                successful += 1;
            }
            if (toolExecutionResult.status() == ToolExecutionStatus.SKIPPED
            || toolExecutionResult.status() == ToolExecutionStatus.FAILED
            || toolExecutionResult.status() == ToolExecutionStatus.REJECTED){
                failed += 1;
            }

        }


        return new AgentExecutionResult(
                response.message().content(),
                budget.getRemainingToolCall(),
                budget.getRemainingIterations(),
                executionResults,
                toolCalls,
                successful,
                failed,
                terminationReason
        );
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
