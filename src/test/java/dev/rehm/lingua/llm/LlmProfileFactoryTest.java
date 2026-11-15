
package dev.rehm.lingua.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmProfileFactoryTest {

    private void assertProfile(
            LlmProfile profile,
            String expectedModel,
            Object expectedThink,
            int expectedNumPredict,
            double expectedTemperature,
            double expectedTopP,
            int expectedTopK
    ) {
        assertNotNull(profile);

        assertAll(
                () -> assertEquals(expectedModel, profile.getModel()),
                () -> assertEquals(expectedThink, profile.isThink()),
                () -> assertEquals(8192, profile.getNumCtx()),
                () -> assertEquals(expectedNumPredict, profile.getNumPredict()),
                () -> assertEquals(expectedTemperature, profile.getTemperature(), 0.0001),
                () -> assertEquals(expectedTopP, profile.getTopP(), 0.0001),
                () -> assertEquals(0.0, profile.getMinP(), 0.0001),
                () -> assertEquals(expectedTopK, profile.getTopK()),
                () -> assertEquals(1.0, profile.getRepeatPenalty(), 0.0001)
        );
    }

    @Test
    void shouldCreateQwen35ThinkingProfile() {
        assertProfile(
                LlmProfileFactory.qwen35Thinking(),
                "qwen3.5:4b",
                true,
                4096,
                1.0,
                0.95,
                20
        );
    }

    @Test
    void shouldCreateQwen35NormalProfile() {
        assertProfile(
                LlmProfileFactory.qwen35Normal(),
                "qwen3.5:4b",
                false,
                2048,
                0.7,
                0.8,
                20
        );
    }

    @Test
    void shouldCreateQwen35FinalProfile() {
        assertProfile(
                LlmProfileFactory.qwen35Final(),
                "qwen3.5:4b",
                false,
                1024,
                0.7,
                0.8,
                20
        );
    }

    @Test
    void shouldCreateQwen3ThinkingProfile() {
        assertProfile(
                LlmProfileFactory.qwen3Thinking(),
                "qwen3:8b",
                true,
                4096,
                0.6,
                0.95,
                20
        );
    }

    @Test
    void shouldCreateQwen3NormalProfile() {
        assertProfile(
                LlmProfileFactory.qwen3Normal(),
                "qwen3:8b",
                false,
                2048,
                0.7,
                0.8,
                20
        );
    }

    @Test
    void shouldCreateQwen3FinalProfile() {
        assertProfile(
                LlmProfileFactory.qwen3Final(),
                "qwen3:8b",
                false,
                1024,
                0.7,
                0.8,
                20
        );
    }

    @Test
    void shouldCreateGptOssThinkingProfile() {
        assertProfile(
                LlmProfileFactory.gptOssThinking(),
                "gpt-oss:20b",
                "high",
                4096,
                1.0,
                1.0,
                0
        );
    }

    @Test
    void shouldCreateGptOssNormalProfile() {
        assertProfile(
                LlmProfileFactory.gptOssNormal(),
                "gpt-oss:20b",
                "low",
                2048,
                1.0,
                1.0,
                0
        );
    }

    @Test
    void shouldCreateGptOssFinalProfile() {
        assertProfile(
                LlmProfileFactory.gptOssFinal(),
                "gpt-oss:20b",
                "low",
                2048,
                1.0,
                1.0,
                0
        );
    }

    @Test
    void shouldCreateIndependentProfileInstances() {
        LlmProfile first = LlmProfileFactory.qwen35Thinking();
        LlmProfile second = LlmProfileFactory.qwen35Thinking();

        assertNotSame(first, second);

        first.setTemperature(99.0);

        assertEquals(99.0, first.getTemperature(), 0.0001);
        assertEquals(1.0, second.getTemperature(), 0.0001);
    }
}
