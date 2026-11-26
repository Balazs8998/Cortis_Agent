package dev.rehm.lingua.llm.ollama;

import dev.rehm.lingua.common.exception.classes.OllamaTimeoutException;
import dev.rehm.lingua.llm.client.ClientInterface;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class OllamaServiceTest {

    @Mock
    private ClientInterface<OllamaChatRequest, OllamaChatResponse> client;

    @InjectMocks
    private OllamaService service;


    @Test
    void shouldDelegateRequestToClientAndReturnResponse() {

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

        OllamaChatResponse expectedResponse = new OllamaChatResponse(
                "qwen3.5:4b",
                new OllamaMessageDto("assistant", null, "Szia!", null),
                true,
                "stop",
                10,
                23L,
                2L        );

        when(client.sendMessageToClient(request))
                .thenReturn(expectedResponse);
        OllamaChatResponse actual = service.messageSend(request);
        assertSame(expectedResponse, actual);

        verify(client).sendMessageToClient(request);
    }

    @Test
    void shouldPropagateOllamaTimeoutExceptionWhenClientTimesOut(){
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

      when(client.sendMessageToClient(request))
              .thenThrow(new OllamaTimeoutException(
                      "sendMessageToClient",
                      "ollamaUrl",
                      "/api/chat"
              ));

      assertThrows(OllamaTimeoutException.class,()-> service.messageSend(request));
      verify(client).sendMessageToClient(request);
  }

}