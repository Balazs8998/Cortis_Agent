
package dev.rehm.lingua.conversation;

import dev.rehm.lingua.common.exception.classes.ConversationNotFoundException;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationMemory memory;

    @InjectMocks
    private ConversationService service;

    @Test
    void shouldCreateAndStoreNewConversation() {
        String uuid = service.openNewChat();

        ArgumentCaptor<ConversationMessage> captor =
                ArgumentCaptor.forClass(ConversationMessage.class);

        verify(memory).addNewChat(captor.capture());

        ConversationMessage saved = captor.getValue();

        assertEquals(saved.getUuid().toString(), uuid);
    }

    @Test
    void shouldReturnMessageListFromMemory() {
        UUID uuid = UUID.randomUUID();

        List<OllamaMessageDto> expected = List.of(
                new OllamaMessageDto("user", null, "hello", null)
        );

        when(memory.getMessageList(uuid)).thenReturn(expected);

        List<OllamaMessageDto> actual = service.getMessageList(uuid);

        assertSame(expected, actual);
        verify(memory).getMessageList(uuid);
    }

    @Test
    void shouldDelegateSerializedExecutionToConversation() {
        UUID uuid = UUID.randomUUID();

        ConversationMessage conversation = mock(ConversationMessage.class);
        Supplier<String> action = () -> "completed";

        when(memory.getConversation(uuid)).thenReturn(conversation);
        when(conversation.executeSerialized(action)).thenReturn("completed");

        String result = service.executeSerialized(uuid, action);

        assertEquals("completed", result);

        verify(memory).getConversation(uuid);
        verify(conversation).executeSerialized(action);
    }

    @Test
    void shouldThrowExceptionWhenConversationDoesNotExist() {
        UUID uuid = UUID.randomUUID();

        when(memory.getConversation(uuid)).thenThrow(
                new ConversationNotFoundException(
                        "getConversation",
                        "conversationId",
                        uuid
                )
        );

        assertThrows(
                ConversationNotFoundException.class,
                () -> service.executeSerialized(uuid, () -> "test")
        );
    }
}
