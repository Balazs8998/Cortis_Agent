package dev.rehm.lingua.searxng.dto;

public record SearxngResultDto(
        String title,
        String url,
        String content,
        String engine,
        Double score,
        String publishedDate

) {}