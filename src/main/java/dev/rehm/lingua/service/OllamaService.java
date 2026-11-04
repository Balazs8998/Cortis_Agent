package dev.rehm.lingua.service;

import dev.rehm.lingua.client.OllamaClient;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;

import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;


import java.util.List;


@Service
public class OllamaService {

    private final OllamaClient client;


    public OllamaService(OllamaClient client) {
        this.client = client;

    }

    public OllamaChatResponse messageSend(OllamaChatRequest request) {
        return client.sendMessage(request);
    }

    public @NotNull boolean hasToolCalling(OllamaChatResponse response) {
        if (response.message().toolCalls() == null) {
            return false;
        }
        return !response.message().toolCalls().isEmpty();
    }

    public void soutMessageList(List<OllamaMessageDto> messageDtos) {
        for (OllamaMessageDto messageDto : messageDtos) {
            System.out.println(messageDto);
        }
        System.out.println("------------------------------------");
    }

}
