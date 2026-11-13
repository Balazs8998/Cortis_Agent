package dev.rehm.lingua.memory;

import dev.rehm.lingua.domain.ConversationMessage;
import dev.rehm.lingua.dto.OllamaMessageDto;
import org.springframework.stereotype.Component;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationMemory {

    private final Map<UUID, ConversationMessage> messageMap;

    public ConversationMemory() {
        this.messageMap = new ConcurrentHashMap<>();
    }

    public ConversationMessage getConversation(UUID uuid) {

        ConversationMessage conversation = messageMap.get(uuid);

        if (conversation == null) {
            throw new NoSuchElementException(
                    "Conversation not found: " + uuid
            );
        }

        return conversation;
    }

    public void addNewChat(ConversationMessage message){
        messageMap.put(message.getUuid(),message);
    }

    public void addMessageToChat(UUID uuid, OllamaMessageDto message) {
        getConversation(uuid).addMessage(message);
    }

    public List<OllamaMessageDto> getMessageList(UUID uuid) {
        return getConversation(uuid).getMessages();
    }

    public Map<UUID, ConversationMessage> getMessageMap() {
        return Map.copyOf(messageMap);
    }
}
