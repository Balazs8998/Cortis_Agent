package dev.rehm.lingua.controller;

import dev.rehm.lingua.dto.request.ChatRequest;
import dev.rehm.lingua.dto.response.ChatResponse;
import dev.rehm.lingua.inspection.dto.AgentExecutionResult;
import dev.rehm.lingua.service.AgentService;
import dev.rehm.lingua.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private final AgentService agentService;
    private final ConversationService conversationService;


    public OllamaController(AgentService agentService, ConversationService conversationService) {
        this.agentService = agentService;
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> ollamaChat(@RequestBody @Valid ChatRequest request){
        AgentExecutionResult answer = this.agentService.executeAgentLoop(request);

        ChatResponse response = new ChatResponse(
                request.conversationId(),
                answer.response().message().content(),
                answer.remainingToolCalls(),
                answer.remainingIterations(),
                answer.toolExecutionResults(),
                answer.toolCalls(),
                answer.successful(),
                answer.failed(),
                answer.agentTerminationReason(),
                answer.agentResponseIssues(),
                answer.response().doneReason()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations")
    public ResponseEntity<String> ollamaNewChat(){

        String conversationId = conversationService.openNewChat();

        return ResponseEntity.ok(conversationId);
    }
}
