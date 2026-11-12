package dev.rehm.lingua.registry;

import dev.rehm.lingua.tool.AgentTool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, AgentTool> toolregistry = new HashMap<>();


    public ToolRegistry(List<AgentTool> tools) {
        for (AgentTool tool : tools) {
            toolregistry.put(tool.definition().name(), tool);
        }
    }

    public AgentTool getTool(String toolName){
        return toolregistry.get(toolName);
    }

    public Map<String, AgentTool> getToolregistry() {

        return toolregistry;
    }
}
