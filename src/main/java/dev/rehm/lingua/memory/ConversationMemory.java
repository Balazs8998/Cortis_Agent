package dev.rehm.lingua.memory;

import dev.rehm.lingua.dto.OllamaMessageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConversationMemory {
    private final List<OllamaMessageDto> messages = new ArrayList<>();


    public void addMessage(OllamaMessageDto messageDto){
        this.messages.add(messageDto);
    }

    public List<OllamaMessageDto> getMessages() {
        return List.copyOf(messages);
    }

}
