package dev.rehm.lingua;

import dev.rehm.lingua.client.OllamaClient;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.dto.request.OllamaChatRequest;
import dev.rehm.lingua.dto.response.OllamaChatResponse;
import dev.rehm.lingua.service.OllamaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    public void sendMessage() {
        OllamaMessageDto messageDto = new OllamaMessageDto(
                "user",
                "Mit írtam az előbb? emlékszel ?"
        );
        OllamaChatRequest request = new OllamaChatRequest(
                "gpt-oss:20b",
                List.of(messageDto),
                false
        );

        OllamaChatResponse response = client.sendMessage(request);

        assertNotNull(response);
        boolean resContent = !response.message().content().isBlank();
        Assertions.assertTrue(resContent);
        System.out.println(response.message().content());
    }

    @Test
    public void sendMessageFromService() {
        String message = "szia, itt vagy ?";

        String answer = this.ollamaService.sendMessage(message);
        assertNotNull(answer);
        boolean answerIsBlank = answer.isBlank();
        assertFalse(answerIsBlank);
    }

    @Test
    public void sendMessageFromService_WhatIsTheFirstMessage() {
        String message1 = "szia, itt vagy?";

        this.ollamaService.sendMessage(message1);
        String message = "Mit kérdeztem az előbb?";

        String answer = this.ollamaService.sendMessage(message);
        assertTrue(answer.contains("szia, itt vagy?"));

    }

}
