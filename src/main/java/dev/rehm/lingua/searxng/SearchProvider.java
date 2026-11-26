package dev.rehm.lingua.searxng;


import dev.rehm.lingua.agent.dto.WebSearchResponse;

import java.util.List;

public interface SearchProvider {

    WebSearchResponse search(String query, int limit);

}
