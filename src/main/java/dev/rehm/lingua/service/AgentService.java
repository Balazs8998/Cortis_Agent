package dev.rehm.lingua.service;

import dev.rehm.lingua.argument.ArgumentHandler;
import dev.rehm.lingua.client.OllamaClient;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.dto.response.tool.ToolCallResponse;
import dev.rehm.lingua.execution.AgentExecutionBudget;
import dev.rehm.lingua.factory.MessageFactory;
import dev.rehm.lingua.factory.RequestFactory;
import dev.rehm.lingua.memory.ConversationMemory;
import dev.rehm.lingua.registry.ToolRegistry;
import dev.rehm.lingua.tool.CalculatorTools;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class AgentService {

    private final OllamaService ollamaService;
    private final MessageService messageService;
    private final ConversationMemory memory;
    private final CalculatorTools calculatorTools;
    private final ArgumentHandler argumentHandler;
    private final RequestFactory requestFactory;



    public AgentService(MessageService messageService, ConversationMemory memory, CalculatorTools calculatorTools, ToolRegistry toolRegistry, OllamaService ollamaService, ArgumentHandler argumentHandler, RequestFactory requestFactory) {

        this.messageService = messageService;
        this.memory = memory;
        this.calculatorTools = calculatorTools;
        this.ollamaService = ollamaService;
        this.argumentHandler = argumentHandler;
        this.requestFactory = requestFactory;
    }

    public String executeAgentLoop(String userMessage) {
        AgentExecutionBudget agentExecutionBudget = new AgentExecutionBudget(2);

        boolean hasToolCalling = true;
        OllamaChatResponse response = null;
        OllamaMessageDto newMessage = MessageFactory.createUserMessageDto(userMessage);
        messageService.saveMessages(newMessage);

        System.out.println("-------start-----------");
        agentExecutionBudget.printLoopLimits();
        System.out.println("------------------");


        while (hasToolCalling) {
            if (agentExecutionBudget.getRemainingIterations() <= 0) {

                return "Elfogyott a futási keret, így a feladat meg lett szakítva.";
            }
            agentExecutionBudget.decrementRemainingIterations();
            agentExecutionBudget.incrementIterations();

            List<OllamaMessageDto> messages = new ArrayList<>(memory.getMessages());

            OllamaChatRequest ollamaChatRequest = requestFactory.createBaseRequest(messages);
            OllamaChatRequest ollamaChatRequestWithTool = requestFactory.addToolsToRequest(ollamaChatRequest);

            response = ollamaService.messageSend(ollamaChatRequestWithTool);

            if (response == null || response.message() == null) {
                throw new IllegalStateException("nincs response");
            }
            messageService.saveMessages(response.message());
            hasToolCalling = ollamaService.hasToolCalling(response);

            if (hasToolCalling) {
                toolCallingProcess(response, agentExecutionBudget);
            }

        }
        ollamaService.soutMessageList(memory.getMessages());
        agentExecutionBudget.printLoopLimits();
        System.out.println("-------end-----------");

        return response.message().content();
    }

    private void choiceTool(ToolCallResponse toolCallResponse) {
        String toolName = toolCallResponse.function().name();
        Map<String, Object> arguments = toolCallResponse.function().arguments();
        switch (toolName) {
            case "add":
                Double a = argumentHandler.getArgument(toolName, "a", arguments.get("a"), Double.class);
                Double b = argumentHandler.getArgument(toolName, "b", arguments.get("b"), Double.class);
                Double result = calculatorTools.add(a, b);

                messageService.createAndSaveToolMessage(toolName, result);
                break;
            default:
                throw new IllegalStateException("valami elbaszoódott xDD");
        }

    }

    private void toolCallingProcess(OllamaChatResponse response, AgentExecutionBudget agentExecutionBudget) {
        List<ToolCallResponse> toolCallResponses = new ArrayList<>(response.message().toolCalls());

        for (int i = 0; i < toolCallResponses.size(); i++) {
            if (agentExecutionBudget.getRemainingToolCall() <= 0) {

                for (int j = i; j < toolCallResponses.size(); j++) {
                    messageService.createAndSaveToolErrorMessage(toolCallResponses.get(j).function().name());
                }

                return;
            }

            choiceTool(toolCallResponses.get(i));
            agentExecutionBudget.decrementRemainingTools();
        }
    }

}
