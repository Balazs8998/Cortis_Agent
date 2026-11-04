package dev.rehm.lingua.domain;

import dev.rehm.lingua.dto.OllamaMessageDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationMessage {

    private UUID uuid;
    private List<OllamaMessageDto> messages;

    public ConversationMessage() {
        this.uuid = UUID.randomUUID();
        this.messages = new ArrayList<>();
    }


    public void addMessage(OllamaMessageDto message){
        this.messages.add(message);
    }


    public UUID getUuid() {
        return uuid;
    }

    public List<OllamaMessageDto> getMessages() {
        return List.copyOf(messages);
    }
}
