package dev.rehm.lingua.conversation;

import dev.rehm.lingua.common.exception.classes.ConversationNotFoundException;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConversationMemoryTest {

    private ConversationMemory memory;
    private ConversationMessage message;

    @BeforeEach
    void setUp(){
        this.memory = new ConversationMemory();
        this.message = new ConversationMessage();
        memory.addNewChat(message);
    }

    private OllamaMessageDto createMessage(){
        return new OllamaMessageDto("user",null,"hello",null);

    }
    private OllamaMessageDto createMessage1(){
        return new OllamaMessageDto("user",null,"bello",null);

    }

    @Test
    void shouldReturnConversationWhenConversationExists(){
        UUID uuid = message.getUuid();

        ConversationMessage conversationMessage = assertDoesNotThrow(()->memory.getConversation(uuid));

        assertSame(message, conversationMessage);
        assertNotNull(conversationMessage);

    }

    @Test
    void shouldThrowConversationNotFoundExceptionWhenConversationDoesNotExist(){
        UUID uuid = UUID.randomUUID();

        ConversationNotFoundException exception = assertThrows(
                ConversationNotFoundException.class,
                () -> memory.getConversation(uuid)
        );

        assertEquals(
                "Conversation not found: " + uuid,
                exception.getMessage()
        );
    }

    @Test
    void shouldAddNewChatWhenUuidIsValid(){
        ConversationMessage conversationMessage = new ConversationMessage();

        assertEquals(1, memory.getMapSize());

        assertDoesNotThrow(()->memory.addNewChat(conversationMessage));
        assertEquals(2, memory.getMapSize());
    }


    @Test
    void shouldAddMessageWhenConversationExists(){
        UUID uuid = message.getUuid();

        assertEquals(0,memory.getConversation(uuid).getMessages().size());

        memory.addMessageToChat(uuid,createMessage());
        assertEquals(1,memory.getConversation(uuid).getMessages().size());
        assertEquals("hello", memory.getConversation(uuid).getMessages().getFirst().content());

    }

    @Test
    void shouldKeepMessagesSeparatedBetweenConversations(){
        UUID uuid = message.getUuid();
        ConversationMessage conversationMessage = new ConversationMessage();
        UUID uuid1 = conversationMessage.getUuid();
        memory.addNewChat(conversationMessage);

        assertEquals(0,memory.getConversation(uuid).getMessages().size());
        assertEquals(0,memory.getConversation(uuid1).getMessages().size());

        memory.addMessageToChat(uuid,createMessage());
        memory.addMessageToChat(uuid1,createMessage1());

        assertEquals(1,memory.getConversation(uuid).getMessages().size());
        assertEquals("hello", memory.getConversation(uuid).getMessages().getFirst().content());

        assertEquals(1,memory.getConversation(uuid1).getMessages().size());
        assertEquals("bello", memory.getConversation(uuid1).getMessages().getFirst().content());

    }

    @Test
    void shouldThrowConversationNotFoundExceptionWhenAddingMessageToNonExistingConversation(){
        UUID uuid = UUID.randomUUID();
      assertThrows(
                ConversationNotFoundException.class,
                () -> memory.addMessageToChat(uuid,createMessage())
        );

    }

    @Test
    void shouldReturnMessagesForExistingConversation(){
        UUID uuid = message.getUuid();
        memory.addMessageToChat(uuid,createMessage());

        List<OllamaMessageDto> list = memory.getMessageList(uuid);

        assertEquals(1, list.size());
        assertEquals("hello", list.getFirst().content());
    }

    @Test
    void shouldThrowConversationNotFoundExceptionWhenGettingMessagesFromNonExistingConversation(){
        UUID uuid = UUID.randomUUID();
        assertThrows(
                ConversationNotFoundException.class,
                () -> memory.getMessageList(uuid)
        );
    }




}