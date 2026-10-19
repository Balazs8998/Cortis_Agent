package dev.rehm.lingua.service;

import dev.rehm.lingua.client.OllamaClient;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.memory.ConverationMemory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OllamaService {

    private final OllamaClient client;
    private final ConverationMemory memory;
    private final String model;
    private final boolean stream;

    public OllamaService(OllamaClient client, ConverationMemory memory ) {
        this.client = client;
        this.memory = memory;
        this.model = "gpt-oss:20b";
        this.stream = false;
    }

    public String sendMessage(String message){
        List<OllamaMessageDto> messages = new ArrayList<> (memory.getMessages());

        OllamaMessageDto newMessage = new OllamaMessageDto("user", message);
        messages.add(newMessage);

        OllamaChatRequest request = new OllamaChatRequest(model,messages,stream);

        OllamaChatResponse response = client.sendMessage(request);

        if(response != null &&  response.message() != null && response.message().content() != null && !response.message().content().isBlank()){
            memory.addMessage(newMessage);
            memory.addMessage(response.message());
            return response.message().content();
        }


        throw new IllegalStateException("Ollama returned no response to the chat request.");
    }

}
