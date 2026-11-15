package dev.rehm.lingua.conversation;

import dev.rehm.lingua.common.exception.classes.ConversationNotFoundException;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
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
            throw new ConversationNotFoundException(
                    "getConversation",
                    "conversationId",
                    uuid
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

    public int getMapSize(){
       return this.messageMap.size();
    }

}
