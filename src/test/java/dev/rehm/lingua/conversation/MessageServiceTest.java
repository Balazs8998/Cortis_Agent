package dev.rehm.lingua.conversation;

import dev.rehm.lingua.agent.message.MessageFactory;
import dev.rehm.lingua.agent.message.MessageService;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageServiceTest {

    @Test
    void shouldDelegateMessageSavingToMemory() {
        ConversationMemory memory = mock(ConversationMemory.class);
        ConversationService service = new ConversationService(memory);
        UUID uuid = UUID.randomUUID();

        OllamaMessageDto message =
                new OllamaMessageDto("user", null, "hello", null);
        List<OllamaMessageDto> messages = List.of(message);

        service.messageSave(uuid, messages);

        verify(memory).addMessageToChat(uuid, messages);
    }

    @Test
    void shouldCreateToolMessage() {
        OllamaMessageDto saved = MessageFactory.createToolMessageDto("add", 42);

        assertEquals("tool", saved.role());
        assertEquals("add", saved.toolName());
        assertEquals("42", saved.content());
        assertNull(saved.toolCalls());
    }

    @Test
    void shouldConvertNullToolResultToString() {
        OllamaMessageDto message = MessageFactory.createToolMessageDto("add", null);
        assertEquals("null", message.content());
    }

    @Test
    void shouldCreateSystemMessageWithConfiguredPrompt() {
        MessageService service = new MessageService(JsonMapper.builder().build());
        OllamaMessageDto message = service.firstSystemMessage();

        assertNotNull(message);
        assertEquals("system", message.role());
        assertNull(message.toolName());
        assertNull(message.toolCalls());

        assertTrue(message.content().contains("You are CORTIS"));
        assertTrue(message.content().contains("Tools and Capabilities"));
    }
}
