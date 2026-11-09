package dev.rehm.lingua.tool.calculator;

import dev.rehm.lingua.argument.ArgumentHandler;
import dev.rehm.lingua.dto.request.tool.ToolRequestFunction;
import dev.rehm.lingua.dto.request.tool.ToolRequestParameters;
import dev.rehm.lingua.dto.request.tool.ToolRequestProperties;
import dev.rehm.lingua.tool.AgentTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AddTool implements AgentTool {

    private final ArgumentHandler argumentHandler;

    public AddTool(ArgumentHandler argumentHandler) {
        this.argumentHandler = argumentHandler;
    }

    @Override
    public ToolRequestFunction definition() {

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


        return calculatorAddFunction;
    }

    @Override
    public Object execute(AgentTool agentTool,Map<String, Object> arguments) {
        double a = argumentHandler.getArgument(agentTool,"add","b", arguments.get("b"),Double.class);
        double b = argumentHandler.getArgument(agentTool,"add","a",arguments.get("a"),Double.class);

        return a + b;
    }
}
