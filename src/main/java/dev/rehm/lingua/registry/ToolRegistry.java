package dev.rehm.lingua.registry;

import dev.rehm.lingua.dto.request.tool.ToolRequestFunction;
import dev.rehm.lingua.dto.request.tool.ToolRequestParameters;
import dev.rehm.lingua.dto.request.tool.ToolRequestProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, ToolRequestFunction> toolregistry = new HashMap<>();



    public void loadToolRegistry(){

        ToolRequestProperties calculatorAddPropertiesA = new ToolRequestProperties(
                "number",
                "Szám amihez hozzáadjuk a második számot.");

        ToolRequestProperties calculatorAddPropertiesB = new ToolRequestProperties(
                "number",
                "Szám amit hozzáadunk az első számhoz");

        ToolRequestParameters calculatorAddParameters = new ToolRequestParameters(
                "object",
                new HashMap<>(Map.of("a", calculatorAddPropertiesA,"b",calculatorAddPropertiesB)),
                new ArrayList<>(List.of("a","b"))
        );

        ToolRequestFunction calculatorAddFunction = new ToolRequestFunction(
                "add",
                "Az a és b paraméterek összeadását végzi el.",
                calculatorAddParameters);

        toolregistry.put("add",calculatorAddFunction);
    }

    public ToolRequestFunction getToolFunction(String toolName){

        return toolregistry.get(toolName);

    }

    public Map<String, ToolRequestFunction> getToolregistry() {

        return toolregistry;
    }
}
