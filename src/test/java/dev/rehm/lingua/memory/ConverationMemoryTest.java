package dev.rehm.lingua.memory;

import dev.rehm.lingua.dto.OllamaMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConverationMemoryTest {

    private ConversationMemory conversationMemory;
    private List<OllamaMessageDto> messageDtoList;

    @BeforeEach
    void setUp() {
        this.conversationMemory = new ConversationMemory();
      this.messageDtoList = createMessages();

    }

    private List<OllamaMessageDto> createMessages(){

        OllamaMessageDto firstMessage = new OllamaMessageDto(
                "user",
                null,
                "Hello Agent World User",
                new ArrayList<>()
        );

        OllamaMessageDto secondMessage = new OllamaMessageDto(
                "tool",
                null,
                "Hello Agent World Tool",
                new ArrayList<>()
        );

        OllamaMessageDto thirdMessage = new OllamaMessageDto(
                "System",
                null,
                "Hello Agent World System",
                new ArrayList<>()
        );

        List<OllamaMessageDto> messages = new ArrayList<>();
        messages.add(firstMessage);
        messages.add(secondMessage);
        messages.add(thirdMessage);

        return messages;
    }

    @Test
    void shouldAddSingleMessageToMemory() {

        assertEquals(0, conversationMemory.getMessages().size());
        conversationMemory.addMessage(this.messageDtoList.get(1));

        assertEquals(1, conversationMemory.getMessages().size());


    }

    @Test
    void shouldAllowAddingMultipleMessagesToMemory() {


        assertEquals(0, conversationMemory.getMessages().size());

        conversationMemory.addMessage(this.messageDtoList.get(1));
        conversationMemory.addMessage(this.messageDtoList.get(2));

        assertEquals(2, conversationMemory.getMessages().size());

    }

    @Test
    void shouldStoreCorrectMessageInMemory() {
        conversationMemory.addMessage(this.messageDtoList.get(1));
        assertEquals(this.messageDtoList.get(1), conversationMemory.getMessages().getFirst());
    }

    @Test
    void shouldPreserveMessageOrderInMemory(){
        conversationMemory.addMessage(this.messageDtoList.get(1));
        conversationMemory.addMessage(this.messageDtoList.get(2));

        assertEquals(this.messageDtoList.get(1), conversationMemory.getMessages().getFirst());
        assertEquals(this.messageDtoList.get(2), conversationMemory.getMessages().get(1));
    }

    @Test
    void shouldInitializeMemoryWithEmptyMessageList() {
        ConversationMemory memory = new ConversationMemory();

        assertNotNull(memory.getMessages());
        assertTrue(memory.getMessages().isEmpty());
    }
}