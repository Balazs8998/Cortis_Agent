package dev.rehm.lingua.memory;

import dev.rehm.lingua.domain.ConversationMessage;
import dev.rehm.lingua.dto.OllamaMessageDto;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationMemory {

    private final Map<UUID, ConversationMessage> messageMap;

    public ConversationMemory() {
        this.messageMap = new ConcurrentHashMap<>();
    }

    public void addNewChat(ConversationMessage message){
        messageMap.put(message.getUuid(),message);
    }

    public void addMessageToChat(UUID uuid,OllamaMessageDto message){
        messageMap.get(uuid).addMessage(message);
    }

    public Map<UUID, ConversationMessage> getMessageMap() {
        return Map.copyOf(messageMap);
    }
}
