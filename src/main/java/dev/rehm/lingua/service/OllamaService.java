package dev.rehm.lingua.service;

import dev.rehm.lingua.client.ClientInterface;
import dev.rehm.lingua.client.OllamaClient;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import org.springframework.stereotype.Service;



@Service
public class OllamaService {

    private final ClientInterface<OllamaChatRequest,OllamaChatResponse> client;


    public OllamaService(ClientInterface<OllamaChatRequest,OllamaChatResponse> client) {
        this.client = client;

    }

    public OllamaChatResponse messageSend(OllamaChatRequest request) {
        return client.sendMessageToClient(request);
    }




}
