package dev.rehm.lingua.factory;

import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.request.RequestOption;
import dev.rehm.lingua.dto.request.RequestTool;
import dev.rehm.lingua.registry.ToolRegistry;
import dev.rehm.lingua.service.MessageService;
import dev.rehm.lingua.tool.AgentTool;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RequestFactory {

    private  final MessageService messageService;
    private final ToolRegistry toolRegistry;
    private static final String model = "qwen3:8b";

    public RequestFactory(MessageService messageService, ToolRegistry toolRegistry) {
        this.messageService = messageService;
        this.toolRegistry = toolRegistry;
    }


    public  @NotNull OllamaChatRequest createBaseRequest(List<OllamaMessageDto> messages) {
        OllamaMessageDto systemMessage = messageService.firstSystemMessage();
        List<OllamaMessageDto> messageDtos = new ArrayList<>();
        messageDtos.add(systemMessage);
        messageDtos.addAll(messages);
        RequestOption options = new RequestOption(8192, 4096, 0.2, 0.9, 0.3, 40, 1.1);

        return new OllamaChatRequest(model, messageDtos, new ArrayList<>(), null, options, false, false, -1);
    }

    public  @NotNull OllamaChatRequest addToolsToRequest(OllamaChatRequest request) {
        request.tools().addAll(createRequestTools());

        return request;
    }

    private @NotNull List<RequestTool> createRequestTools() {
        Map<String, AgentTool> toolRequestFunctionMap = Map.copyOf(toolRegistry.getToolregistry());
        List<RequestTool> requestTools = new ArrayList<>();

        for (Map.Entry<String, AgentTool> entry : toolRequestFunctionMap.entrySet()) {
            requestTools.add(new RequestTool("function", entry.getValue().definition()));
        }

        return requestTools;
    }

}
