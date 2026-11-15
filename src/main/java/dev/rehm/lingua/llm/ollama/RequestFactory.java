package dev.rehm.lingua.llm.ollama;

import dev.rehm.lingua.llm.LlmProfile;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.request.RequestOption;
import dev.rehm.lingua.llm.ollama.dto.request.RequestTool;
import dev.rehm.lingua.tool.ToolRegistry;
import dev.rehm.lingua.conversation.MessageService;
import dev.rehm.lingua.tool.AgentTool;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RequestFactory {


    private final ToolRegistry toolRegistry;
    private final OllamaMessageDto systemMessage;

    public RequestFactory(MessageService messageService, ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.systemMessage = messageService.firstSystemMessage();
    }


    public  @NotNull OllamaChatRequest createBaseRequest(List<OllamaMessageDto> messages, LlmProfile llmProfile) {

        List<OllamaMessageDto> messageDtos = new ArrayList<>();
        messageDtos.add(systemMessage);
        messageDtos.addAll(messages);
        RequestOption options =
                new RequestOption(llmProfile.getNumCtx(),
                        llmProfile.getNumPredict(),
                        llmProfile.getTemperature(),
                        llmProfile.getTopP(),
                        llmProfile.getMinP(),
                        llmProfile.getTopK(),
                        llmProfile.getRepeatPenalty());

        return new OllamaChatRequest(llmProfile.getModel(), messageDtos, new ArrayList<>(), null, options, false, llmProfile.isThink(), -1);
    }

    public  @NotNull OllamaChatRequest createLastBaseRequest(List<OllamaMessageDto> messages,LlmProfile llmProfile) {

        List<OllamaMessageDto> messageDtos = new ArrayList<>();
        messageDtos.add(systemMessage);
        messageDtos.addAll(messages);
        RequestOption options =
                new RequestOption(llmProfile.getNumCtx(),
                        llmProfile.getNumPredict(),
                        llmProfile.getTemperature(),
                        llmProfile.getTopP(),
                        llmProfile.getMinP(),
                        llmProfile.getTopK(),
                        llmProfile.getRepeatPenalty());

        return new OllamaChatRequest(llmProfile.getModel(), messageDtos, new ArrayList<>(), null, options, false,llmProfile.isThink(), -1);
    }


    public  @NotNull OllamaChatRequest createRequestWithTool(List<OllamaMessageDto> messages, LlmProfile llmProfile) {
        OllamaChatRequest request =createBaseRequest(messages, llmProfile);
        request.tools().addAll(findToolsToRequest());

        return request;

    }

    private @NotNull List<RequestTool> findToolsToRequest() {
        Map<String, AgentTool> toolRequestFunctionMap = Map.copyOf(toolRegistry.getToolregistry());
        List<RequestTool> requestTools = new ArrayList<>();

        for (Map.Entry<String, AgentTool> entry : toolRequestFunctionMap.entrySet()) {
            requestTools.add(new RequestTool("function", entry.getValue().definition()));
        }

        return requestTools;
    }

}
