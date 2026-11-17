package dev.rehm.lingua.searxng.dto;

public record SearxngRequestDto(
        String query,
        int maxResults
) {
}
