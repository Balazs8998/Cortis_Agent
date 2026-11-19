package dev.rehm.lingua.conversation;

import dev.rehm.lingua.common.util.JsonMapperUtil;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import jakarta.validation.constraints.NotNull;



public class MessageFactory {


    public static @NotNull OllamaMessageDto createUserMessageDto(String userMessage) {
        return new OllamaMessageDto("user", null, userMessage, null);
    }

    public static @NotNull OllamaMessageDto createSystemMessageDto(String systemMessage) {
        return new OllamaMessageDto("system", null, systemMessage, null);
    }

    public static @NotNull OllamaMessageDto createToolMessageDto(String toolName, Object result) {
        String json = JsonMapperUtil.toJson(result);

        return new OllamaMessageDto("tool", toolName, json, null);
    }


}
