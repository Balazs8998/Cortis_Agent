package dev.rehm.lingua.service;

import dev.rehm.lingua.domain.ConversationMessage;
import dev.rehm.lingua.dto.OllamaMessageDto;
import dev.rehm.lingua.memory.ConversationMemory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ConversationService {

    private final ConversationMemory memory;


    public ConversationService(ConversationMemory memory) {
        this.memory = memory;
    }


    public String openNewChat(){
        ConversationMessage conversationMessage = new ConversationMessage();
        memory.addNewChat(conversationMessage);
        return conversationMessage.getUuid().toString();
    }

    public List<OllamaMessageDto> getMessageList(UUID uuid){
        return memory.getMessageList(uuid);
    }
}
