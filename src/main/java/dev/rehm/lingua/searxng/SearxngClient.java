package dev.rehm.lingua.searxng;

import dev.rehm.lingua.agent.dto.WebSearchResult;
import dev.rehm.lingua.common.exception.classes.SearxngTimeOutException;
import dev.rehm.lingua.llm.client.ClientInterface;
import dev.rehm.lingua.searxng.dto.SearxngRequestDto;
import dev.rehm.lingua.searxng.dto.SearxngResponseDto;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

@Component
public class SearxngClient implements ClientInterface<SearxngRequestDto, SearxngResponseDto> {

    private final RestClient client;

    public SearxngClient(
            @Qualifier("SearxngRestClient") RestClient client
    ) {
        this.client = client;
    }

    @Override
    public SearxngResponseDto sendMessageToClient(SearxngRequestDto request) {

        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", request.query())
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SearxngResponseDto.class);

        } catch (ResourceAccessException e) {

            if (e.getMostSpecificCause()
                    instanceof SocketTimeoutException) {

                throw new SearxngTimeOutException(
                        "sendMessageToClient",
                        "searxngUrl",
                        "/search"
                );
            }

            throw e;
        }
    }
}
