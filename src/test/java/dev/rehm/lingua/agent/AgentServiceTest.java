package dev.rehm.lingua.agent;

import dev.rehm.lingua.agent.inspection.AgentExecutionResult;
import dev.rehm.lingua.chat.dto.ChatRequest;
import dev.rehm.lingua.common.exception.classes.AgentEmptyFinalResponseException;
import dev.rehm.lingua.conversation.ConversationMemory;
import dev.rehm.lingua.conversation.ConversationService;
import dev.rehm.lingua.conversation.MessageService;
import dev.rehm.lingua.llm.LlmProfile;
import dev.rehm.lingua.llm.LlmProfileService;
import dev.rehm.lingua.llm.LlmProfiles;
import dev.rehm.lingua.llm.ollama.OllamaService;
import dev.rehm.lingua.llm.ollama.RequestFactory;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaChatResponse;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolCallResponse;
import dev.rehm.lingua.llm.ollama.dto.response.OllamaToolResponseFunction;
import dev.rehm.lingua.tool.ToolExecutionResult;
import dev.rehm.lingua.tool.ToolExecutionStatus;
import dev.rehm.lingua.tool.ToolExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Agent conversation regression tests.
 *
 * Uses REAL in-memory conversation storage and a spy ConversationService.
 * No Spring context, HTTP server or Ollama process is needed.
 *
 * One known bug in the supplied AgentExecutionContext is intentionally
 * caught by shouldNotDuplicatePreviouslyCommittedHistoryInLlmRequest().
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock private ToolExecutorService executorService;
    @Mock private OllamaService ollamaService;
    @Mock private MessageService messageService;
    @Mock private RequestFactory requestFactory;
    @Mock private LlmProfileService llmProfileService;

    private ConversationService conversationService;
    private AgentService agentService;
    private UUID conversationId;
    private ChatRequest chatRequest;

    @BeforeEach
    void setUp() {
        // Exercise the actual ConversationMemory and ConversationMessage code.
        conversationService = spy(new ConversationService(new ConversationMemory()));
        conversationId = UUID.fromString(conversationService.openNewChat());
        chatRequest = new ChatRequest(conversationId, "Hello Cortis", "qwen3:8b", false);

        agentService = new AgentService(
                messageService,
                executorService,
                ollamaService,
                requestFactory,
                conversationService,
                llmProfileService
        );

        LlmProfile agentProfile = mock(LlmProfile.class);
        LlmProfile finalProfile = mock(LlmProfile.class);
        when(llmProfileService.createLlmProfiles("qwen3:8b", false))
                .thenReturn(new LlmProfiles(agentProfile, finalProfile));

        // Copy the request messages so their contents can be inspected later.
        when(requestFactory.createRequestWithTool(anyList(), any(LlmProfile.class)))
                .thenAnswer(invocation -> requestOf(invocation.getArgument(0)));
    }

    @Test
    void shouldCommitCompleteTurnOnceAndSaveUserOnceAcrossThreeIterations() {
        stubSuccessfulTools();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(toolCallResponse("first_tool"),
                            toolCallResponse("second_tool"),
                            finalResponse("Done"));

        AgentExecutionResult result = agentService.executeAgentLoop(chatRequest);

        assertNotNull(result);
        verify(ollamaService, times(3)).messageSend(any(OllamaChatRequest.class));
        verify(executorService, times(2))
                .executeTool(any(OllamaToolCallResponse.class), anyInt(), any(Instant.class));

        List<OllamaMessageDto> committed = captureSingleCommit();
        assertEquals(committed, conversationService.getMessageList(conversationId));
        assertEquals(List.of("user", "assistant", "tool", "assistant", "tool", "assistant"),
                committed.stream().map(OllamaMessageDto::role).toList());
        assertEquals(1, countUserMessage(committed, "Hello Cortis"));
        assertEquals("first_tool", committed.get(2).toolName());
        assertEquals("second_tool", committed.get(4).toolName());
        assertEquals("Done", committed.get(5).content());
    }

    @Test
    void shouldIncludeUserMessageExactlyOnceInEachOfThreeOllamaRequests() {
        stubSuccessfulTools();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(toolCallResponse("first_tool"),
                            toolCallResponse("second_tool"),
                            finalResponse("Done"));

        agentService.executeAgentLoop(chatRequest);

        List<OllamaChatRequest> requests = captureRequests(3);
        assertEquals(List.of(1, 3, 5),
                requests.stream().map(r -> r.messages().size()).toList());

        for (int i = 0; i < requests.size(); i++) {
            assertEquals(1, countUserMessage(requests.get(i).messages(), "Hello Cortis"),
                    "Duplicated user message in iteration " + (i + 1));
        }
    }

    @Test
    void shouldNotDuplicatePreviouslyCommittedHistoryInLlmRequest() {
        List<OllamaMessageDto> earlierTurn = List.of(
                message("user", "Previous question"),
                message("assistant", "Previous answer")
        );
        conversationService.messageSave(conversationId, earlierTurn);
        clearInvocations(conversationService);
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(finalResponse("New answer"));

        agentService.executeAgentLoop(chatRequest);

        List<OllamaMessageDto> requestMessages = captureRequests(1).get(0).messages();
        assertEquals(List.of("Previous question", "Previous answer", "Hello Cortis"),
                requestMessages.stream().map(OllamaMessageDto::content).toList(),
                "Committed history must occur once, followed by the new user message");
        assertEquals(1, countUserMessage(requestMessages, "Hello Cortis"));

        List<OllamaMessageDto> committed = captureSingleCommit();
        assertEquals(List.of("user", "assistant"),
                committed.stream().map(OllamaMessageDto::role).toList());
        assertEquals(4, conversationService.getMessageList(conversationId).size());
    }

    @Test
    void shouldLeaveCommittedHistoryUnchangedIfSecondOllamaCallFails() {
        List<OllamaMessageDto> previous = List.of(
                message("user", "Old question"),
                message("assistant", "Old answer")
        );
        conversationService.messageSave(conversationId, previous);
        clearInvocations(conversationService);

        stubSuccessfulTools();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(toolCallResponse("first_tool"))
                .thenThrow(new IllegalStateException("Simulated upstream failure"));

        assertThrows(IllegalStateException.class,
                () -> agentService.executeAgentLoop(chatRequest));

        assertEquals(previous, conversationService.getMessageList(conversationId),
                "A failed agent turn must not leave partial messages in memory");
        verify(conversationService, never()).messageSave(eq(conversationId), anyList());
        verify(ollamaService, times(2)).messageSend(any(OllamaChatRequest.class));
    }

    @Test
    void shouldNotPublishHalfBatchWhenSecondToolDtoIsMalformed() {
        stubSuccessfulTools();
        OllamaToolCallResponse valid = new OllamaToolCallResponse(
                "function", new OllamaToolResponseFunction(0, "first_tool", Map.of("value", 1))
        );
        OllamaToolCallResponse malformed = new OllamaToolCallResponse("function", null);
        OllamaMessageDto assistant = new OllamaMessageDto(
                "assistant", null, "", List.of(valid, malformed)
        );
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(response(assistant));

        assertThrows(RuntimeException.class,
                () -> agentService.executeAgentLoop(chatRequest));

        assertTrue(conversationService.getMessageList(conversationId).isEmpty(),
                "A malformed tool call must not leave the earlier successful reply in history");
        verify(conversationService, never()).messageSave(eq(conversationId), anyList());
    }

    @Test
    void shouldStoreFailedToolReplyAndStillCommitCompleteTurn() {
        when(executorService.executeTool(any(OllamaToolCallResponse.class), anyInt(), any(Instant.class)))
                .thenThrow(new IllegalArgumentException("Simulated invalid tool DTO"));
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(toolCallResponse("invalid_tool"), finalResponse("Partial information"));

        agentService.executeAgentLoop(chatRequest);

        List<OllamaMessageDto> committed = captureSingleCommit();
        assertEquals(List.of("user", "assistant", "tool", "assistant"),
                committed.stream().map(OllamaMessageDto::role).toList());
        assertEquals("invalid_tool", committed.get(2).toolName());
        assertTrue(committed.get(2).content().contains("\"status\":\"FAILED\""),
                "Failed tool must be represented by a proper tool message");
        assertEquals("Partial information", committed.get(3).content());
    }

    @Test
    void shouldCommitToolLimitTurnWithAllSkippedAndSuccessfulToolReplies() {
        stubSuccessfulTools();
        stubFinalRequestFactory();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(toolCallResponse("tool_1", "tool_2", "tool_3", "tool_4",
                                             "tool_5", "tool_6", "tool_7"),
                            finalResponse("Tool limit reached"));

        agentService.executeAgentLoop(chatRequest);

        List<OllamaMessageDto> committed = captureSingleCommit();
        assertEquals(1, countUserMessage(committed, "Hello Cortis"));
        assertEquals("user", committed.get(0).role());
        assertEquals("assistant", committed.get(1).role());
        List<OllamaMessageDto> toolReplies = committed.stream()
                .filter(m -> "tool".equals(m.role())).toList();
        assertEquals(7, toolReplies.size(),
                "Every requested tool needs a result, including skipped ones");
        assertEquals(1, toolReplies.stream()
                .filter(m -> m.content().contains("\"status\":\"SKIPPED\"")).count());
        assertEquals("assistant", committed.get(committed.size() - 1).role());
        verify(executorService, times(6))
                .executeTool(any(OllamaToolCallResponse.class), anyInt(), any(Instant.class));

        List<OllamaChatRequest> requests = captureRequests(2);
        assertTrue(requests.get(1).tools().isEmpty(),
                "Finalization must not offer tools");
    }

    @Test
    void shouldIncludeCommittedHistoryInEmptyFinalRetry() {
        conversationService.messageSave(conversationId, List.of(
                message("user", "Earlier question"),
                message("assistant", "Earlier answer")
        ));
        stubFinalRequestFactory();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(finalResponse(""), finalResponse("Recovered answer"));

        agentService.executeAgentLoop(chatRequest);

        List<OllamaChatRequest> requests = captureRequests(2);
        for (OllamaChatRequest request : requests) {
            assertEquals(List.of("Earlier question", "Earlier answer", "Hello Cortis"),
                    request.messages().stream().map(OllamaMessageDto::content).toList(),
                    "Both initial and retry requests must contain exactly one copy of the history");
        }
    }

    @Test
    void shouldIncludeCommittedHistoryInToolLimitFinalization() {
        conversationService.messageSave(conversationId, List.of(
                message("user", "Earlier question"),
                message("assistant", "Earlier answer")
        ));
        stubSuccessfulTools();
        stubFinalRequestFactory();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(toolCallResponse("tool_1", "tool_2", "tool_3", "tool_4",
                                             "tool_5", "tool_6", "tool_7"),
                            finalResponse("Budget exhausted"));

        agentService.executeAgentLoop(chatRequest);

        List<OllamaChatRequest> requests = captureRequests(2);
        for (OllamaChatRequest request : requests) {
            assertEquals(1, countUserMessage(request.messages(), "Earlier question"),
                    "Previous user turn must occur exactly once");
            assertEquals(1, request.messages().stream()
                    .filter(m -> "assistant".equals(m.role())
                            && "Earlier answer".equals(m.content())).count(),
                    "Previous assistant turn must occur exactly once");
            assertEquals(1, countUserMessage(request.messages(), "Hello Cortis"));
        }
    }

    @Test
    void shouldNotCommitAnythingWhenEmptyFinalRetryAlsoFails() {
        stubFinalRequestFactory();
        when(ollamaService.messageSend(any(OllamaChatRequest.class)))
                .thenReturn(finalResponse(""), finalResponse(""));

        assertThrows(AgentEmptyFinalResponseException.class,
                () -> agentService.executeAgentLoop(chatRequest));

        assertTrue(conversationService.getMessageList(conversationId).isEmpty());
        verify(conversationService, never()).messageSave(eq(conversationId), anyList());
        verify(ollamaService, times(2)).messageSend(any(OllamaChatRequest.class));
    }

    // ---- Reusable stubs / assertions ----

    private void stubSuccessfulTools() {
        when(executorService.executeTool(any(OllamaToolCallResponse.class), anyInt(), any(Instant.class)))
                .thenAnswer(invocation -> {
                    OllamaToolCallResponse call = invocation.getArgument(0);
                    int index = invocation.getArgument(1);
                    Instant startedAt = invocation.getArgument(2);
                    return new ToolExecutionResult(
                            call.function().name(),
                            call.function().arguments(),
                            "Tool completed",
                            ToolExecutionStatus.SUCCES,
                            startedAt,
                            1.0,
                            Thread.currentThread().threadId(),
                            Thread.currentThread().isVirtual(),
                            index
                    );
                });
    }

    private void stubFinalRequestFactory() {
        when(requestFactory.createBaseRequest(anyList(), any(LlmProfile.class)))
                .thenAnswer(invocation -> requestOf(invocation.getArgument(0)));
    }

    private OllamaChatRequest requestOf(List<OllamaMessageDto> messages) {
        return new OllamaChatRequest(
                "qwen3:8b",
                new ArrayList<>(messages),
                new ArrayList<>(),
                null,
                null,
                false,
                false,
                -1
        );
    }

    private List<OllamaMessageDto> captureSingleCommit() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<OllamaMessageDto>> captor =
                (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(conversationService, times(1)).messageSave(eq(conversationId), captor.capture());
        return captor.getValue();
    }

    private List<OllamaChatRequest> captureRequests(int count) {
        ArgumentCaptor<OllamaChatRequest> captor =
                ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(ollamaService, times(count)).messageSend(captor.capture());
        return captor.getAllValues();
    }

    private long countUserMessage(List<OllamaMessageDto> messages, String text) {
        return messages.stream()
                .filter(m -> "user".equals(m.role()) && text.equals(m.content()))
                .count();
    }

    private OllamaMessageDto message(String role, String content) {
        return new OllamaMessageDto(role, null, content, null);
    }

    private OllamaChatResponse toolCallResponse(String... toolNames) {
        List<OllamaToolCallResponse> calls = new ArrayList<>();
        for (int i = 0; i < toolNames.length; i++) {
            calls.add(new OllamaToolCallResponse(
                    "function",
                    new OllamaToolResponseFunction(i, toolNames[i], Map.of("value", 1))
            ));
        }
        return response(new OllamaMessageDto("assistant", null, "", calls));
    }

    private OllamaChatResponse finalResponse(String content) {
        return response(new OllamaMessageDto("assistant", null, content, null));
    }

    private OllamaChatResponse response(OllamaMessageDto message) {
        return new OllamaChatResponse("qwen3:8b", message, true, "stop", 10, 20L, 100L);
    }
}
