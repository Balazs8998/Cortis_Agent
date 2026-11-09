package dev.rehm.lingua.tool;

import dev.rehm.lingua.dto.request.tool.ToolRequestFunction;

import java.util.Map;

public interface AgentTool {

    ToolRequestFunction definition();
    Object execute(AgentTool tool, Map<String, Object> arguments);
}
