package dev.rehm.lingua.domain;

import dev.rehm.lingua.dto.OllamaMessageDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ConversationMessage {

    private final UUID uuid;
    private final List<OllamaMessageDto> messages;
    private final ReentrantLock executionLock = new ReentrantLock();

    public ConversationMessage() {
        this.uuid = UUID.randomUUID();
        this.messages = new ArrayList<>();
    }

    public <T> T executeSerialized(Supplier<T> action) {

        executionLock.lock();

        try {
            return action.get();
        } finally {
            executionLock.unlock();
        }
    }


    public synchronized void addMessage(OllamaMessageDto message){
        this.messages.add(message);
    }

    public synchronized List<OllamaMessageDto> getMessages() {
        return List.copyOf(messages);
    }

    public UUID getUuid() {
        return uuid;
    }

}
