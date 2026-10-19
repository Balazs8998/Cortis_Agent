package dev.rehm.lingua.client;


import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaClient {

    private final RestClient client;

    public OllamaClient(RestClient client) {
        this.client = client;
    }


    public OllamaChatResponse sendMessage(OllamaChatRequest request){

       return client.post().uri("/api/chat").body(request).retrieve().body(OllamaChatResponse.class);
    }


}
