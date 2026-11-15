
package dev.rehm.lingua.conversation;

import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ConversationMemory memory;

    @InjectMocks
    private MessageService service;

    @Test
    void shouldDelegateMessageSavingToMemory() {
        UUID uuid = UUID.randomUUID();

        OllamaMessageDto message =
                new OllamaMessageDto("user", null, "hello", null);

        service.saveMessages(uuid, message);

        verify(memory).addMessageToChat(uuid, message);
    }

    @Test
    void shouldCreateAndSaveToolMessage() {
        UUID uuid = UUID.randomUUID();

        service.createAndSaveToolMessage(uuid, "add", 42);

        ArgumentCaptor<OllamaMessageDto> captor =
                ArgumentCaptor.forClass(OllamaMessageDto.class);

        verify(memory).addMessageToChat(eq(uuid), captor.capture());

        OllamaMessageDto saved = captor.getValue();

        assertEquals("tool", saved.role());
        assertEquals("add", saved.toolName());
        assertEquals("42", saved.content());
        assertNull(saved.toolCalls());
    }

    @Test
    void shouldConvertNullToolResultToString() {
        UUID uuid = UUID.randomUUID();

        service.createAndSaveToolMessage(uuid, "add", null);

        ArgumentCaptor<OllamaMessageDto> captor =
                ArgumentCaptor.forClass(OllamaMessageDto.class);

        verify(memory).addMessageToChat(eq(uuid), captor.capture());

        assertEquals("null", captor.getValue().content());
    }

    @Test
    void shouldCreateSystemMessageWithConfiguredPrompt() {
        OllamaMessageDto message = service.firstSystemMessage();

        assertNotNull(message);
        assertEquals("system", message.role());
        assertNull(message.toolName());
        assertNull(message.toolCalls());

        assertTrue(message.content().contains("Your name is CORTIS."));
        assertTrue(message.content().contains("Tool usage rules:"));
    }
}
