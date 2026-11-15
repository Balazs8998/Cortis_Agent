package dev.rehm.lingua.dto.llm;


import dev.rehm.lingua.domain.LlmProfile;

public record LlmProfiles(
        LlmProfile agentProfile,
        LlmProfile finalProfile
) {
}