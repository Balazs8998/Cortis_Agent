package dev.rehm.lingua.llm.ollama;

import dev.rehm.lingua.llm.client.ClientInterface;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
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
