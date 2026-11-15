package dev.rehm.lingua.conversation;

import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageFactoryTest {

    @Test
    void shouldCreateUserMessageDto() {
        String content = "Szia, Cortis!";

        OllamaMessageDto message =
                MessageFactory.createUserMessageDto(content);

        assertAll(
                () -> assertNotNull(message),
                () -> assertEquals("user", message.role()),
                () -> assertNull(message.toolName()),
                () -> assertEquals(content, message.content()),
                () -> assertNull(message.toolCalls())
        );
    }

    @Test
    void shouldCreateSystemMessageDto() {
        String content = "You are CORTIS.";

        OllamaMessageDto message =
                MessageFactory.createSystemMessageDto(content);

        assertAll(
                () -> assertNotNull(message),
                () -> assertEquals("system", message.role()),
                () -> assertNull(message.toolName()),
                () -> assertEquals(content, message.content()),
                () -> assertNull(message.toolCalls())
        );
    }
}