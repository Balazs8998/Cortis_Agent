package dev.rehm.lingua.llm.ollama;


import dev.rehm.lingua.common.exception.classes.OllamaTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


class OllamaClientTest {

    private OllamaClient ollamaClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:11434");

        mockServer = MockRestServiceServer.bindTo(builder).build();

        ollamaClient = new OllamaClient(builder.build());
    }


    @Test
    void shouldReturnOllamaResponseWhenRequestSucceeds() {
        OllamaChatRequest request = new OllamaChatRequest(
                "qwen3.5:4b",
                List.of(new OllamaMessageDto("user", null, "Szia!", null)),
                List.of(),
                null,
                null,
                false,
                false,
                null
        );

        mockServer.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                                {
                                  "model": "qwen3.5:4b",
                                  "message": {
                                    "role": "assistant",
                                    "content": "Szia! Miben segíthetek?"
                                  },
                                  "done": true
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        OllamaChatResponse response = ollamaClient.sendMessageToClient(request);

        assertNotNull(response);
        assertEquals("qwen3.5:4b", response.model());
        assertEquals("Szia! Miben segíthetek?", response.message().content());
        mockServer.verify();
    }

    @Test
    void shouldSendPostRequestToOllamaChatEndpoint() {

        OllamaChatRequest request = new OllamaChatRequest(
                "qwen3.5:4b",
                List.of(new OllamaMessageDto("user", null, "Szia!", null)),
                List.of(),
                null,
                null,
                false,
                false,
                null
        );

        mockServer.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("qwen3.5:4b"))
                .andExpect(jsonPath("$.messages[0].content").value("Szia!"))
                .andRespond(withSuccess(
                        """
                                {
                                  "model": "qwen3.5:4b",
                                  "message": {
                                    "role": "assistant",
                                    "content": "Szia!"
                                  },
                                  "done": true
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));
        ollamaClient.sendMessageToClient(request);


        mockServer.verify();
    }


    @Test
    void shouldThrowOllamaTimeoutExceptionWhenSocketTimeoutOccurs() {

        OllamaChatRequest request = new OllamaChatRequest(
                "qwen3.5:4b",
                List.of(new OllamaMessageDto("user", null, "Szia!", null)),
                List.of(),
                null,
                null,
                false,
                false,
                null
        );


        mockServer.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withException(new SocketTimeoutException("Read timed out"))
                );
        assertThrows(
                OllamaTimeoutException.class,
                () -> ollamaClient.sendMessageToClient(request)
        );

        mockServer.verify();
    }


    @Test
    void shouldPropagateResourceAccessExceptionForOtherConnectionErrors() {
        OllamaChatRequest request = new OllamaChatRequest(
                "qwen3.5:4b",
                List.of(new OllamaMessageDto("user", null, "Szia!", null)),
                List.of(),
                null,
                null,
                false,
                false,
                null
        );

        mockServer.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withException(new IOException())
                );
        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> ollamaClient.sendMessageToClient(request)
        );

        assertInstanceOf(
                IOException.class,
                exception.getMostSpecificCause()
        );
        mockServer.verify();

    }

}