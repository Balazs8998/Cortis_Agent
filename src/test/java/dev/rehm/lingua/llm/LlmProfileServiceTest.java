package dev.rehm.lingua.llm;

import dev.rehm.lingua.common.exception.classes.UnsupportedLlmModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmProfileServiceTest {

    private LlmProfileService llmProfileService;

    @BeforeEach
    void setUp(){
        this.llmProfileService = new LlmProfileService();
    }

    @Test
    void shouldCreateQwen35ThinkingProfile(){
        String model = "qwen3.5:4b";
        boolean think = true;
        LlmProfiles llmProfiles = llmProfileService.createLlmProfiles(model,think);

        assertEquals(model,llmProfiles.agentProfile().getModel());
        assertEquals(true,llmProfiles.agentProfile().isThink());

        assertEquals(model,llmProfiles.finalProfile().getModel());
        assertEquals(false,llmProfiles.finalProfile().isThink());

    }

    @Test
    void shouldCreateQwen35NormalProfile(){
        String model = "qwen3.5:4b";
        boolean think = false;
        LlmProfiles llmProfiles = llmProfileService.createLlmProfiles(model,think);

        assertEquals(model,llmProfiles.agentProfile().getModel());
        assertEquals(false,llmProfiles.agentProfile().isThink());

        assertEquals(model,llmProfiles.finalProfile().getModel());
        assertEquals(false,llmProfiles.finalProfile().isThink());
    }

    @Test
    void shouldCreateQwen3ThinkingProfile(){
        String model = "qwen3:8b";
        boolean think = true;
        LlmProfiles llmProfiles = llmProfileService.createLlmProfiles(model,think);

        assertEquals(model,llmProfiles.agentProfile().getModel());
        assertEquals(true,llmProfiles.agentProfile().isThink());

        assertEquals(model,llmProfiles.finalProfile().getModel());
        assertEquals(false,llmProfiles.finalProfile().isThink());
    }

    @Test
    void shouldCreateQwen3NormalProfile(){
        String model = "qwen3:8b";
        boolean think = false;
        LlmProfiles llmProfiles = llmProfileService.createLlmProfiles(model,think);

        assertEquals(model,llmProfiles.agentProfile().getModel());
        assertEquals(false,llmProfiles.agentProfile().isThink());

        assertEquals(model,llmProfiles.finalProfile().getModel());
        assertEquals(false,llmProfiles.finalProfile().isThink());
    }

    @Test
    void shouldCreateGptOssThinkingProfile(){
        String model = "gpt-oss:20b";
        boolean think = true;
        LlmProfiles llmProfiles = llmProfileService.createLlmProfiles(model,think);

        assertEquals(model,llmProfiles.agentProfile().getModel());
        assertEquals("high",llmProfiles.agentProfile().isThink());

        assertEquals(model,llmProfiles.finalProfile().getModel());
        assertEquals("low",llmProfiles.finalProfile().isThink());
    }

    @Test
    void shouldCreateGptOssNormalProfile(){
        String model = "gpt-oss:20b";
        boolean think = false;
        LlmProfiles llmProfiles = llmProfileService.createLlmProfiles(model,think);

        assertEquals(model,llmProfiles.agentProfile().getModel());
        assertEquals("low",llmProfiles.agentProfile().isThink());

        assertEquals(model,llmProfiles.finalProfile().getModel());
        assertEquals("low",llmProfiles.finalProfile().isThink());
    }

    @Test
    void shouldThrowExceptionWhenModelIsUnsupported(){
        String model = "Unknown";
        boolean think = false;

        UnsupportedLlmModelException exception = assertThrows(
                UnsupportedLlmModelException.class,
                () -> llmProfileService.createLlmProfiles(model, think)
        );

        assertEquals(
                "Unsupported LLM model: Unknown",
                exception.getMessage()
        );

    }

}