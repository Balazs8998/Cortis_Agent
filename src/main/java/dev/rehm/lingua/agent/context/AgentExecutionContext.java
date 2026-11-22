package dev.rehm.lingua.agent.context;

import dev.rehm.lingua.agent.iteration.AgentExecutionBudget;
import dev.rehm.lingua.chat.dto.ChatRequest;
import dev.rehm.lingua.agent.message.MessageFactory;
import dev.rehm.lingua.llm.LlmProfiles;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.tool.ToolExecutionResult;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentExecutionContext {

    private final UUID uuid;
    private final AgentExecutionBudget agentExecutionBudget;
    private final List<ToolExecutionResult> toolResults;
    private boolean hasToolCalling;
    private OllamaChatResponse response;
    private final LlmProfiles llmProfiles;
    private final AgentConversationContext conversationContextcontext;




    public AgentExecutionContext(ChatRequest request,
                                 LlmProfiles llmProfiles) {
        this.conversationContextcontext = new AgentConversationContext();

        String userMessage = request.message();
        OllamaMessageDto newMessage = MessageFactory.createUserMessageDto(userMessage);
        this.uuid = request.conversationId();
        this.llmProfiles = llmProfiles;

        this.agentExecutionBudget = new AgentExecutionBudget(6,4);

        this.toolResults = new ArrayList<>();
        this.hasToolCalling = true;

        this.response = null;

        this.conversationContextcontext.addPendingMessage(newMessage);
    }



    public List<OllamaMessageDto> getMessagesForLlm() {
        return this.conversationContextcontext.getMessagesForLlm();
    }

    public void setCommittedHistory(List<OllamaMessageDto> messageHistory){
        this.conversationContextcontext.setCommittedHistory(messageHistory);
    }

    public void addPendingMessage(OllamaMessageDto message) {
        this.conversationContextcontext.addPendingMessage(message);
    }

    public List<OllamaMessageDto> getPendingMessages() {
        return conversationContextcontext.getPendingMessages();
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



    public LlmProfiles getLlmProfiles() {
        return llmProfiles;
    }


}
