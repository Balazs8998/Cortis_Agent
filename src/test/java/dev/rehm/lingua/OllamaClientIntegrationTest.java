package dev.rehm.lingua;

import dev.rehm.lingua.client.OllamaClient;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.request.RequestOption;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.service.OllamaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OllamaClientIntegrationTest {

    private OllamaClient client;
    private OllamaService ollamaService;

    @Autowired
    public OllamaClientIntegrationTest(OllamaClient client, OllamaService ollamaService) {
        this.client = client;
        this.ollamaService = ollamaService;
    }

    @Test
    public void shouldReturnNonEmptyResponseFromOllamaClient() {
        RequestOption options = new RequestOption(8192, 4096, 0.2, 0.9, 0.3, 40, 1.1);

        OllamaMessageDto messageDto = new OllamaMessageDto(
                "user",
                "What did I write earlier? Do you remember?",
                null,
                null
        );

        OllamaChatRequest request = new OllamaChatRequest(
                "gpt-oss:20b",
                List.of(messageDto),
                new ArrayList<>(),
                null,
                options,
                false,
                false,
                -1
        );

        OllamaChatResponse response = client.sendMessage(request);

        assertNotNull(response);
        boolean resContent = !response.message().content().isBlank();
        Assertions.assertTrue(resContent);
        System.out.println(response.message().content());
    }

    @Test
    public void shouldReturnNonEmptyResponseFromAgentLoop() {
        String message = "Hi, are you there?";

        String answer = this.ollamaService.executeAgentLoop(message);
        assertNotNull(answer);
        boolean answerIsBlank = answer.isBlank();
        assertFalse(answerIsBlank);
    }

    @Test
    public void shouldIncludePreviousMessageInAgentResponse() {
        String message1 = "Hi, are you there?";

        this.ollamaService.executeAgentLoop(message1);

        String message = "Repeat my previous message exactly as I wrote it, word for word, without any additions or modifications.";

        String answer = this.ollamaService.executeAgentLoop(message);
        assertTrue(answer.contains("Hi, are you there?"));
    }
}
