package dev.rehm.lingua.controller;

import dev.rehm.lingua.dto.request.ChatRequest;
import dev.rehm.lingua.dto.response.ChatResponse;
import dev.rehm.lingua.service.OllamaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private final OllamaService ollamaService;


    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> ollamaChat(@RequestBody ChatRequest request){
        String answer = this.ollamaService.executeAgentLoop(request.message());

        ChatResponse response = new ChatResponse(answer);

        return ResponseEntity.ok(response);
    }
}
