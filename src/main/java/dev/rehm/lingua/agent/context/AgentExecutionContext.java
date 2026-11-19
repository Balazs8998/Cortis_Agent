package dev.rehm.lingua.agent.context;

import dev.rehm.lingua.agent.iteration.AgentExecutionBudget;
import dev.rehm.lingua.chat.dto.ChatRequest;
import dev.rehm.lingua.conversation.MessageFactory;
import dev.rehm.lingua.llm.LlmProfiles;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.tool.ToolExecutionResult;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentExecutionContext {

    private final String userMessage;
    private final UUID uuid;
    private final AgentExecutionBudget agentExecutionBudget;
    private final List<ToolExecutionResult> toolResults;
    private boolean hasToolCalling;
    private OllamaChatResponse response;
    private final OllamaMessageDto newMessage;
    private final LlmProfiles llmProfiles;
    private boolean userMessageCommited;
    private final List<OllamaMessageDto> pendingMessages =
            new ArrayList<>();
    private final List<OllamaMessageDto> committedHistory =
            new ArrayList<>();


    public AgentExecutionContext(ChatRequest request,
                                 LlmProfiles llmProfiles) {

        this.userMessage = request.message();
        this.uuid = request.conversationId();
        this.agentExecutionBudget = new AgentExecutionBudget(6,4);
        this.toolResults = new ArrayList<>();
        this.hasToolCalling = true;
        this.response = null;
        this.newMessage = MessageFactory.createUserMessageDto(userMessage);
        this.llmProfiles = llmProfiles;
        this.userMessageCommited = false;
        addPendingMessage(newMessage);
    }



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

    public void addToolResults(List<ToolExecutionResult> resultList){
        toolResults.addAll(resultList);
    }

    public UUID getUuid() {
        return uuid;
    }

    public AgentExecutionBudget getAgentExecutionBudget() {
        return agentExecutionBudget;
    }

    public List<ToolExecutionResult> getResults() {
        return List.copyOf(toolResults);
    }

    public Boolean getHasToolCalling() {
        return hasToolCalling;
    }

    public void setHasToolCalling(Boolean hasToolCalling) {
        this.hasToolCalling = hasToolCalling;
    }

    public OllamaChatResponse getResponse() {
        return response;
    }

    public void setResponse(OllamaChatResponse response) {
        this.response = response;
    }

    public OllamaMessageDto getNewMessage() {
        return newMessage;
    }

    public LlmProfiles getLlmProfiles() {
        return llmProfiles;
    }

    public boolean isUserMessageCommited() {
        return userMessageCommited;
    }

    public void setIsUserMessageCommited(boolean userMessageCommited) {
        this.userMessageCommited = userMessageCommited;
    }
}
