package dev.rehm.lingua.llm.ollama;


import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.common.exception.classes.OllamaTimeoutException;
import dev.rehm.lingua.llm.client.ClientInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

@Component
public class OllamaClient implements ClientInterface<OllamaChatRequest, OllamaChatResponse> {

    private final RestClient client;

    public OllamaClient(
            @Qualifier("ollamaRestClient") RestClient client
    ) {
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
