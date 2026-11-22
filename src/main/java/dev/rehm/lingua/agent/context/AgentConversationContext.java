package dev.rehm.lingua.agent.context;

import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;

import java.util.ArrayList;
import java.util.List;

public class AgentConversationContext {

    private final List<OllamaMessageDto> pendingMessages =
            new ArrayList<>();
    private final List<OllamaMessageDto> committedHistory =
            new ArrayList<>();





    public List<OllamaMessageDto> getMessagesForLlm() {

        List<OllamaMessageDto> messagesForLlm =
                new ArrayList<>(committedHistory);

        messagesForLlm.addAll(pendingMessages);

        return messagesForLlm;
    }

    public void setCommittedHistory(List<OllamaMessageDto> messageHistory){
        this.committedHistory.addAll(messageHistory);
    }

    public void addPendingMessage(OllamaMessageDto message) {
        pendingMessages.add(message);
    }

    public List<OllamaMessageDto> getPendingMessages() {
        return List.copyOf(pendingMessages);
    }

}
