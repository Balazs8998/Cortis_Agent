package dev.rehm.lingua.tool;

import dev.rehm.lingua.tool.schema.ToolRequestFunction;

import java.util.Map;

public interface AgentTool {

    ToolRequestFunction definition();
    Object execute(Map<String, Object> arguments);
}
