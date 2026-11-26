package dev.rehm.lingua.tool.tools;

import dev.rehm.lingua.tool.AgentTool;
import dev.rehm.lingua.tool.ArgumentHandler;
import dev.rehm.lingua.tool.schema.ToolRequestFunction;
import dev.rehm.lingua.tool.schema.ToolRequestParameters;
import dev.rehm.lingua.tool.schema.ToolRequestProperties;
import dev.rehm.lingua.searxng.SearchProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebSearchTool implements AgentTool {

    private static final String TOOL_NAME = "web_search";
    private static final int RESULT_LIMIT = 5;

    private final ArgumentHandler argumentHandler;
    private final SearchProvider searchProvider;

    public WebSearchTool(
            ArgumentHandler argumentHandler,
            SearchProvider searchProvider
    ) {
        this.argumentHandler = argumentHandler;
        this.searchProvider = searchProvider;
    }

    @Override
    public ToolRequestFunction definition() {

        ToolRequestProperties queryProperty =
                new ToolRequestProperties(
                        "string",
                        "Search query for finding current information " +
                                "on the internet. Use when information may be " +
                                "unknown, outdated, or needs verification."
                );

        ToolRequestParameters parameters =
                new ToolRequestParameters(
                        "object",
                        new HashMap<>(Map.of(
                                "query", queryProperty
                        )),
                        new ArrayList<>(List.of("query"))
                );

        return new ToolRequestFunction(
                TOOL_NAME,
                "Search the internet and return relevant results " +
                        "with titles, URLs, and summaries.",
                parameters
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments) {

        String query = argumentHandler.getArgument(
                definition(),
                TOOL_NAME,
                "query",
                arguments.get("query"),
                String.class
        );

        if (query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must not be blank."
            );
        }

        return searchProvider.search(query.trim(), RESULT_LIMIT);
    }
}
