package dev.rehm.lingua.factory;

import dev.rehm.lingua.dto.OllamaMessageDto;
import jakarta.validation.constraints.NotNull;



public class MessageFactory {


    public static @NotNull OllamaMessageDto createUserMessageDto(String userMessage) {
        return new OllamaMessageDto("user", null, userMessage, null);
    }

    public static @NotNull OllamaMessageDto createSystemMessageDto(String systemMessage) {
        return new OllamaMessageDto("system", null, systemMessage, null);
    }

}
