package dev.rehm.lingua.agent.dto;

public record WebSearchResult(
        String title,
        String url,
        String snippet,
        String engine,
        Double score,
        String publishedDate
) {}