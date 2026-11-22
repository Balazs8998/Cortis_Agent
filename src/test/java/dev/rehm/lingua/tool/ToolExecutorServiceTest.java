
package dev.rehm.lingua.tool;

import dev.rehm.lingua.agent.context.AgentExecutionContext;
import dev.rehm.lingua.chat.dto.ChatRequest;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolCallResponse;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolResponseFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class ToolExecutorServiceTest {

    private ToolExecutorService toolExecutorService;
    private AgentTool addTool;

    @BeforeEach
    void setUp() {
        ToolRegistry registry = mock(ToolRegistry.class);
        addTool = mock(AgentTool.class);

        when(registry.getTool("add")).thenReturn(addTool);
        when(addTool.execute(anyMap())).thenReturn(9);

        toolExecutorService = new ToolExecutorService(registry);
    }

    private OllamaToolCallResponse validCall() {
        return new OllamaToolCallResponse(
                "function",
                new OllamaToolResponseFunction(
                        0,
                        "add",
                        Map.of("a", 4, "b", 5)
                )
        );
    }

    private OllamaToolCallResponse invalidCall() {
        return new OllamaToolCallResponse(
                "function",
                null
        );
    }

    private AgentExecutionContext createContext(
            List<OllamaToolCallResponse> calls
    ) {
        ChatRequest request = new ChatRequest(
                UUID.randomUUID(),
                "Test tool execution",
                null,
                null
        );

        AgentExecutionContext context =
                new AgentExecutionContext(request, null);

        OllamaMessageDto assistantMessage =
                new OllamaMessageDto(
                        "assistant",
                        null,
                        null,
                        calls
                );

        context.setResponse(
                new OllamaChatResponse(
                        "test-model",
                        assistantMessage,
                        true,
                        "stop",
                        null,
                        null,
                        null
                )
        );

        return context;
    }

    @Test
    void shouldPreserveResultsWhenSecondToolHasNullFunction() {

        AgentExecutionContext context = createContext(
                List.of(validCall(), invalidCall())
        );

        List<ToolExecutionResult> results =
                assertDoesNotThrow(
                        () -> toolExecutorService.toolCallingProcess(context)
                );

        assertEquals(2, results.size());

        assertEquals(
                ToolExecutionStatus.SUCCES,
                results.get(0).status()
        );

        assertEquals(9, results.get(0).result());

        assertEquals(
                ToolExecutionStatus.FAILED,
                results.get(1).status()
        );

        assertEquals("UNKNOWN", results.get(1).toolName());

        long toolMessages = context.getPendingMessages()
                .stream()
                .filter(message -> "tool".equals(message.role()))
                .count();

        assertEquals(2, toolMessages);

        verify(addTool, times(1)).execute(anyMap());
    }

    @Test
    void shouldHandleNullFunctionWhenToolBudgetIsExhausted() {

        AgentExecutionContext context = createContext(
                List.of(validCall(), invalidCall())
        );

        context.getAgentExecutionBudget()
                .reserveBudget(6);

        assertEquals(
                0,
                context.getAgentExecutionBudget().getRemainingToolCall()
        );

        List<ToolExecutionResult> results =
                assertDoesNotThrow(
                        () -> toolExecutorService.toolCallingProcess(context)
                );

        assertEquals(2, results.size());

        assertEquals(
                ToolExecutionStatus.SKIPPED,
                results.get(0).status()
        );

        assertEquals(
                ToolExecutionStatus.FAILED,
                results.get(1).status()
        );

        long toolMessages = context.getPendingMessages()
                .stream()
                .filter(message -> "tool".equals(message.role()))
                .count();

        assertEquals(2, toolMessages);

        verifyNoInteractions(addTool);
    }
}
