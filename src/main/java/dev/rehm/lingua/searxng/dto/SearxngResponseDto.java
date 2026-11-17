package dev.rehm.lingua.searxng.dto;


import java.util.List;

public record SearxngResponseDto(
        String query,
        List<SearxngResultDto> results
) {


}
