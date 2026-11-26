package dev.rehm.lingua.searxng;

import dev.rehm.lingua.agent.dto.WebSearchResponse;
import dev.rehm.lingua.agent.dto.WebSearchResult;
import dev.rehm.lingua.searxng.dto.SearxngRequestDto;
import dev.rehm.lingua.searxng.dto.SearxngResponseDto;
import dev.rehm.lingua.searxng.dto.SearxngResultDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearxngSearchProvider implements SearchProvider{

    private final SearxngClient client;

    public SearxngSearchProvider(SearxngClient client) {
        this.client = client;
    }

    @Override
    public WebSearchResponse search(String query, int limit) {

        SearxngRequestDto request = new SearxngRequestDto(query,limit);
        SearxngResponseDto response = client.sendMessageToClient(request);

        List<WebSearchResult> results = new ArrayList<>();
        int count = Math.min(limit, response.results().size());

        for (int i = 0; i < count; i++) {
            SearxngResultDto searxngResultDto = response.results().get(i);
            results.add(new WebSearchResult(
                    searxngResultDto.title(),
                    searxngResultDto.url(),
                    searxngResultDto.content(),
                    searxngResultDto.engine(),
                    searxngResultDto.score(),
                    searxngResultDto.publishedDate()
            ));
        }

        return new WebSearchResponse(request.query(), results);
    }
}
