package dev.rehm.lingua.controller;

import dev.rehm.lingua.dto.request.ChatRequest;
import dev.rehm.lingua.dto.response.ChatResponse;
import dev.rehm.lingua.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private final AgentService agentService;


    public OllamaController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> ollamaChat(@RequestBody ChatRequest request){
        String answer = this.agentService.executeAgentLoop(request.message());

        ChatResponse response = new ChatResponse(answer);

        return ResponseEntity.ok(response);
    }
}
