package dev.rehm.lingua.service;

import dev.rehm.lingua.argument.ArgumentHandler;

import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.dto.response.tool.ToolCallResponse;
import dev.rehm.lingua.exception.classes.UnexpectedNullValueException;
import dev.rehm.lingua.execution.AgentExecutionBudget;
import dev.rehm.lingua.factory.MessageFactory;
import dev.rehm.lingua.factory.RequestFactory;
import dev.rehm.lingua.memory.ConversationMemory;
import dev.rehm.lingua.registry.ToolRegistry;
import dev.rehm.lingua.tool.CalculatorTools;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class AgentService {

    private final OllamaService ollamaService;
    private final MessageService messageService;
    private final ConversationMemory memory;
    private final CalculatorTools calculatorTools;
    private final ArgumentHandler argumentHandler;
    private final RequestFactory requestFactory;
    private final ConversationService conversationService;


    public AgentService(MessageService messageService, ConversationMemory memory, CalculatorTools calculatorTools, ToolRegistry toolRegistry, OllamaService ollamaService, ArgumentHandler argumentHandler, RequestFactory requestFactory, ConversationService conversationService) {

        this.messageService = messageService;
        this.memory = memory;
        this.calculatorTools = calculatorTools;
        this.ollamaService = ollamaService;
        this.argumentHandler = argumentHandler;
        this.requestFactory = requestFactory;
        this.conversationService = conversationService;
    }

    public String executeAgentLoop(UUID uuid, String userMessage) {

        AgentExecutionBudget agentExecutionBudget = new AgentExecutionBudget(3,2);

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
                 return response.message().content();
            }

            OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(messages);
            OllamaChatRequest ollamaChatRequestWithTool = requestFactory.addToolsToRequest(ollamaChatRequest);

            agentExecutionBudget.decrementRemainingIterations();
            agentExecutionBudget.incrementIterations();

            response = agentMessage(ollamaChatRequestWithTool);

            messageService.saveMessages(uuid, response.message());
            hasToolCalling = hasToolCalling(response);

            if (hasToolCalling) {
                toolCallingProcess(uuid, response, agentExecutionBudget);
            }

        }
        ollamaService.soutMessageList(conversationService.getMessageList(uuid));
        agentExecutionBudget.printLoopLimits();
        System.out.println("-------end-----------");

        return response.message().content();
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

    private void choiceTool(UUID uuid,ToolCallResponse toolCallResponse) {
        String toolName = toolCallResponse.function().name();
        Map<String, Object> arguments = toolCallResponse.function().arguments();
        switch (toolName) {
            case "add":
                Double a = argumentHandler.getArgument(toolName, "a", arguments.get("a"), Double.class);
                Double b = argumentHandler.getArgument(toolName, "b", arguments.get("b"), Double.class);
                Double result = calculatorTools.add(a, b);

                messageService.createAndSaveToolMessage(uuid, toolName, result);
                break;
            default:
                throw new IllegalStateException("valami elbaszoódott xDD");
        }

    }

    private void toolCallingProcess(UUID uuid,OllamaChatResponse response, AgentExecutionBudget agentExecutionBudget) {
        List<ToolCallResponse> toolCallResponses = new ArrayList<>(response.message().toolCalls());

        for (int i = 0; i < toolCallResponses.size(); i++) {
            if (agentExecutionBudget.getRemainingToolCall() <= 0) {

                for (int j = i; j < toolCallResponses.size(); j++) {
                    messageService.saveToolCallLimitExceededMessage(uuid,toolCallResponses.get(j).function().name());
                }

                return;
            }

            choiceTool(uuid, toolCallResponses.get(i));
            agentExecutionBudget.decrementRemainingTools();
        }
    }

    private @NotNull boolean hasToolCalling(OllamaChatResponse response) {
        if (response.message().toolCalls() == null) {
            return false;
        }
        return !response.message().toolCalls().isEmpty();
    }

}
