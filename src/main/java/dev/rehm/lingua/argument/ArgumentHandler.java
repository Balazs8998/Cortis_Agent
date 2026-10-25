package dev.rehm.lingua.argument;

import tools.jackson.databind.ObjectMapper;
import dev.rehm.lingua.dto.request.tool.ToolRequestProperties;
import dev.rehm.lingua.registry.ToolRegistry;
import org.springframework.stereotype.Component;

@Component
public class ArgumentHandler {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ArgumentHandler(ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public <T> T getArgument(String toolName, String argumentName, Object argumentValue, Class<T> targetType) {

        if (argumentValue == null) {
            throw new IllegalArgumentException(
                    "Argument value is missing or null. The operation has been aborted."
            );
        }

        if (!validateArgument(toolName, argumentName, argumentValue)) {
            throw new IllegalArgumentException(
                    "The argument does not meet the required conditions. The process has been aborted."
            );
        }

        return convertArgument(argumentValue, targetType);
    }

    private boolean validateArgument(String toolName, String argumentName, Object argumentValue) {
        ToolRequestProperties properties = null;



        if (toolRegistry != null
                && toolRegistry.getToolregistry() != null
                && toolRegistry.getToolregistry().get(toolName) != null
                && toolRegistry.getToolregistry().get(toolName).parameters() != null
                && toolRegistry.getToolregistry().get(toolName).parameters().properties() != null
                && !toolRegistry.getToolregistry().get(toolName).parameters().properties().isEmpty()
                && toolRegistry.getToolregistry().get(toolName).parameters().properties().get(argumentName) != null) {

            properties = toolRegistry.getToolregistry().get(toolName).parameters().properties().get(argumentName);
        }
        if(properties == null){
            throw new IllegalArgumentException(
                    "Argument properties not found. The operation has been aborted."
            );
        }

        if(properties.type() == null){
            throw new IllegalArgumentException(
                    "Argument type not found. The operation has been aborted."
            );
        }

        if (properties.type().equalsIgnoreCase("number")) {
            return argumentValue instanceof Number;
        } else {



            String className = argumentValue.getClass().getSimpleName();
            return properties.type().equalsIgnoreCase(className);
        }
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
