package dev.rehm.lingua.llm.ollama;

import dev.rehm.lingua.agent.message.MessageService;
import dev.rehm.lingua.llm.LlmProfile;
import dev.rehm.lingua.llm.LlmProfileFactory;
import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import dev.rehm.lingua.llm.ollama.dto.request.OllamaChatRequest;
import dev.rehm.lingua.llm.ollama.dto.request.RequestTool;
import dev.rehm.lingua.tool.AgentTool;
import dev.rehm.lingua.tool.ToolRegistry;
import dev.rehm.lingua.tool.schema.ToolRequestFunction;
import dev.rehm.lingua.tool.schema.ToolRequestParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestFactoryTest {

    @Mock
    private MessageService messageService;

    @Mock
    private ToolRegistry toolRegistry;

    private RequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        when(messageService.firstSystemMessage())
                .thenReturn(new OllamaMessageDto(
                        "system", null, "You are CORTIS.", null
                ));

        requestFactory = new RequestFactory(messageService, toolRegistry);
    }

    private OllamaMessageDto createMessage(String content){
        return new OllamaMessageDto(
                "user", null, content, null);
    }

    private LlmProfile createLlmProfile() {
        return LlmProfileFactory.qwen35Normal();
    }

    private OllamaChatRequest createRequest(LlmProfile llmProfile){
        List<OllamaMessageDto> userMessage =new ArrayList<>();
      return requestFactory.createBaseRequest(userMessage,llmProfile);

    }
    private OllamaChatRequest createRequest(LlmProfile llmProfile, OllamaMessageDto messageDto){
        List<OllamaMessageDto> userMessage =new ArrayList<>();
        userMessage.add(messageDto);
        return requestFactory.createBaseRequest(userMessage,llmProfile);

    }

    @Test
    void shouldPrependSystemMessageToConversationHistory(){
        OllamaChatRequest request = createRequest(createLlmProfile(),createMessage("hello"));

        assertAll(
                () -> assertEquals("system", request.messages().getFirst().role()),
                () -> assertEquals("user", request.messages().get(1).role()),
                () -> assertEquals(2, request.messages().size())
        );

    }

    @Test
    void shouldApplyLlmProfileSettingsToRequest(){
        LlmProfile llmProfile = createLlmProfile();
        OllamaChatRequest request = createRequest(llmProfile);



        assertAll(
                () -> assertEquals(llmProfile.getModel(), request.model()),
                () -> assertEquals(llmProfile.isThink(), request.think()),

                // LLM options
                () -> assertNotNull(request.options()),
                () -> assertEquals(llmProfile.getNumCtx(), request.options().numCtx()),
                () -> assertEquals(llmProfile.getNumPredict(), request.options().numPredict()),
                () -> assertEquals(llmProfile.getTemperature(), request.options().temperature()),
                () -> assertEquals(llmProfile.getTopP(), request.options().topP()),
                () -> assertEquals(llmProfile.getMinP(), request.options().minP()),
                () -> assertEquals(llmProfile.getTopK(), request.options().topK()),
                () -> assertEquals(llmProfile.getRepeatPenalty(), request.options().repeatPenalty()),

                // Request configuration
                () -> assertFalse(request.stream()),
                () -> assertNull(request.format()),
                () -> assertEquals(-1, request.keepAlive()),
                () -> assertNotNull(request.tools()),
                () -> assertTrue(request.tools().isEmpty())
        );

    }

    @Test
    void shouldCreateBaseRequestWithoutTools(){
        LlmProfile llmProfile = createLlmProfile();
        OllamaChatRequest request = createRequest(llmProfile);

        assertEquals(0,request.tools().size());
    }

    @Test
    void shouldNotModifyOriginalMessageList(){
        LlmProfile llmProfile = createLlmProfile();
        List<OllamaMessageDto> messageDtoList = new ArrayList<>();
        OllamaMessageDto messageDto1 = createMessage("hello");
        OllamaMessageDto messageDto2 = createMessage("bello");
        messageDtoList.add(messageDto1);
        messageDtoList.add(messageDto2);

        assertEquals("hello", messageDtoList.getFirst().content());
        assertEquals("bello", messageDtoList.get(1).content());

        OllamaChatRequest request = requestFactory.createBaseRequest(messageDtoList,llmProfile);

        assertEquals("hello", messageDtoList.getFirst().content());
        assertEquals("bello", messageDtoList.get(1).content());
        assertAll(
                () -> assertEquals(2, messageDtoList.size()),
                () -> assertNotSame(messageDtoList, request.messages()),
                () -> assertEquals(3, request.messages().size())
        );
    }

    @Test
    void shouldIncludeRegisteredToolsInRequest() {

        ToolRequestParameters parameters = new ToolRequestParameters(
                "object",
                Map.of(),
                List.of()
        );

        ToolRequestFunction addDefinition = new ToolRequestFunction(
                "add",
                "Adds two numbers",
                parameters
        );

        ToolRequestFunction multiplyDefinition = new ToolRequestFunction(
                "multiply",
                "Multiplies two numbers",
                parameters
        );

        AgentTool addTool = mock(AgentTool.class);
        AgentTool multiplyTool = mock(AgentTool.class);

        when(addTool.definition()).thenReturn(addDefinition);
        when(multiplyTool.definition()).thenReturn(multiplyDefinition);

        when(toolRegistry.getToolregistry()).thenReturn(
                Map.of(
                        "add", addTool,
                        "multiply", multiplyTool
                )
        );

        OllamaChatRequest request = requestFactory.createRequestWithTool(
                List.of(createMessage("hello")),
                createLlmProfile()
        );

        assertAll(
                () -> assertEquals(2, request.tools().size()),

                () -> assertTrue(
                        request.tools().stream()
                                .allMatch(tool -> tool.type().equals("function"))
                ),

                () -> assertEquals(
                        Set.of(addDefinition, multiplyDefinition),
                        request.tools().stream()
                                .map(RequestTool::function)
                                .collect(Collectors.toSet())
                )
        );

        verify(toolRegistry).getToolregistry();
    }

    @Test
    void shouldCreateRequestWithEmptyToolListWhenRegistryIsEmpty() {

        when(toolRegistry.getToolregistry())
                .thenReturn(Map.of());

        OllamaChatRequest request = requestFactory.createRequestWithTool(
                List.of(createMessage("hello")),
                createLlmProfile()
        );

        assertAll(
                () -> assertNotNull(request),
                () -> assertNotNull(request.tools()),
                () -> assertTrue(request.tools().isEmpty())
        );

        verify(toolRegistry).getToolregistry();
    }


}