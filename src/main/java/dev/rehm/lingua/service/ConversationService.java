package dev.rehm.lingua.service;

import dev.rehm.lingua.domain.ConversationMessage;
import dev.rehm.lingua.memory.ConversationMemory;
import org.springframework.stereotype.Component;

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
}
