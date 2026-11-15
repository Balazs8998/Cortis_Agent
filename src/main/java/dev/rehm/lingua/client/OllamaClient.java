package dev.rehm.lingua.client;


import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.exception.classes.OllamaTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Instant;

@Component
public class OllamaClient implements ClientInterface<OllamaChatRequest, OllamaChatResponse> {

    private final RestClient client;

    public OllamaClient(RestClient client) {
        this.client = client;
    }

    public OllamaChatResponse sendMessageToClient(
            OllamaChatRequest request) {

        try {
            return client.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

        } catch (ResourceAccessException e) {

            if (e.getMostSpecificCause()
                    instanceof SocketTimeoutException) {

                throw new OllamaTimeoutException(
                        "sendMessageToClient",
                        "ollamaUrl",
                        "ollama-api/chat"
                );
            }

            throw e;
        }
    }

}
