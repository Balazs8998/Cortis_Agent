package dev.rehm.lingua.argument;

import dev.rehm.lingua.dto.request.tool.ToolRequestFunction;
import dev.rehm.lingua.dto.request.tool.ToolRequestParameters;
import dev.rehm.lingua.exception.classes.ArgumentValidationException;
import dev.rehm.lingua.exception.classes.UnexpectedNullValueException;
import dev.rehm.lingua.tool.AgentTool;
import tools.jackson.databind.ObjectMapper;
import dev.rehm.lingua.dto.request.tool.ToolRequestProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ArgumentHandler {

    
    private final ObjectMapper objectMapper;

    public ArgumentHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T getArgument(AgentTool agentTool, String toolName, String argumentName, Object argumentValue, Class<T> targetType) {

        if (argumentValue == null) {
            throw new IllegalArgumentException(
                    "Argument value is missing or null. The operation has been aborted."
            );
        }

        if (toolName == null){
            throw new IllegalArgumentException("Tool name is missing or null. The operation ha been aborted.");
        }

        if (argumentName == null){
            throw new IllegalArgumentException("Argument name is missing or null. The operation ha been aborted.");
        }

        if (targetType == null){
            throw new IllegalArgumentException("Target type is missing or null. The operation ha been aborted.");
        }

        if (!validateArgument(agentTool, toolName, argumentName, argumentValue)) {
            throw new IllegalArgumentException(
                    "The argument does not meet the required conditions. The process has been aborted."
            );
        }

        return convertArgument(argumentValue, targetType);
    }

    private boolean validateArgument(AgentTool agentTool,String toolName, String argumentName, Object argumentValue) {

        ToolRequestFunction toolFunction = agentTool.definition();

        if (toolFunction == null) {
            throw new UnexpectedNullValueException(
                    "ArgumentHandler",
                    "toolFunction",
                    null,
                    "No tool definition found for tool: " + toolName,
                    "TOOL_FUNCTION_NOT_FOUND"
            );
        }

        ToolRequestParameters parameters = toolFunction.parameters();

        if (parameters == null) {
            throw new UnexpectedNullValueException(
                    "ArgumentHandler",
                    "toolFunction.parameters",
                    null,
                    "Parameter definition is null for tool: " + toolName,
                    "TOOL_PARAMETERS_NULL"
            );
        }

        Map<String, ToolRequestProperties> properties = parameters.properties();

        if (properties == null) {
            throw new UnexpectedNullValueException(
                    "ArgumentHandler",
                    "toolFunction.parameters.properties",
                    null,
                    "Parameter properties are null for tool: " + toolName,
                    "TOOL_PROPERTIES_NULL"
            );
        }

        ToolRequestProperties argumentProperty = properties.get(argumentName);

        if (argumentProperty == null) {
            throw new ArgumentValidationException(
                    toolName,
                    "ToolRegistry/Properties/" + argumentName,
                    argumentName,
                    "Argument '" + argumentName + "' is not defined for tool: " + toolName,
                    "TOOL_ARGUMENT_NOT_DEFINED"
            );
        }

        String expectedType = argumentProperty.type();

        if (expectedType == null || expectedType.isBlank()) {
            throw new ArgumentValidationException(
                    toolName,
                    "ToolRegistry/Properties/" + argumentName + "/type",
                    expectedType,
                    "Argument type is missing for argument '" + argumentName
                            + "' in tool: " + toolName,
                    "TOOL_PROPERTIES_TYPE_NULL"
            );
        }

        if (argumentValue == null) {
            return false;
        }

        if (expectedType.equalsIgnoreCase("number")) {
            return argumentValue instanceof Number;
        }

        return switch (expectedType.toLowerCase()) {
            case "number" -> argumentValue instanceof Number;
            case "integer" -> argumentValue instanceof Byte
                    || argumentValue instanceof Short
                    || argumentValue instanceof Integer
                    || argumentValue instanceof Long;
            case "string" -> argumentValue instanceof String;
            case "boolean" -> argumentValue instanceof Boolean;
            case "object" -> argumentValue instanceof Map;
            case "array" -> argumentValue instanceof List;
            default -> false;
        };
        }




    private <T> T convertArgument(Object argumentValue, Class<T> targetType) {

        if(targetType == Double.class){
            return targetType.cast(((Number)argumentValue).doubleValue());
        }

        if(targetType == Float.class){
            return targetType.cast(((Number)argumentValue).floatValue());
        }

        if(targetType == Byte.class){
            return targetType.cast(((Number)argumentValue).byteValue());
        }

        if(targetType == Short.class){
            return targetType.cast(((Number)argumentValue).shortValue());
        }

        if(targetType == Integer.class){
            return targetType.cast(((Number)argumentValue).intValue());
        }
        if(targetType == Long.class){
            return targetType.cast(((Number)argumentValue).longValue());
        }
        if (targetType == String.class || targetType == Boolean.class){
            return targetType.cast(argumentValue);
        }


        return objectMapper.convertValue(argumentValue,targetType);
    }

}
