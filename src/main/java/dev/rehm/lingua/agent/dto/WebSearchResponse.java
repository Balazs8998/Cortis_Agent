package dev.rehm.lingua.agent.dto;

import java.util.List;

public record WebSearchResponse(
        String query,
        List<WebSearchResult> results
) {}