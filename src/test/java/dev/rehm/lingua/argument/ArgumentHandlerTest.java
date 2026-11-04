package dev.rehm.lingua.argument;

import dev.rehm.lingua.dto.request.tool.ToolRequestFunction;
import dev.rehm.lingua.dto.request.tool.ToolRequestParameters;
import dev.rehm.lingua.dto.request.tool.ToolRequestProperties;
import dev.rehm.lingua.registry.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentHandlerTest {

    private ArgumentHandler argumentHandler;
    private ToolRegistry toolRegistry;
    private ObjectMapper objectMapper;

    private String toolName = "add";
    private String argumentName = "a";

    @BeforeEach
    void setUp(){
        this.objectMapper = new ObjectMapper();
        this.toolRegistry = new ToolRegistry();
        this.argumentHandler = new ArgumentHandler(toolRegistry,objectMapper);

    }

    @Test
    void shouldThrowExceptionWhenToolRegistryIsEmpty() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> argumentHandler.getArgument(
                        "add",
                        "a",
                        "string",
                        Double.class
                )
        );

        assertNull(toolRegistry.getToolregistry().get("add"));
        assertEquals(
                "Argument properties not found. The operation has been aborted.",
                exception.getMessage()
        );
    }


    @Test
    void shouldThrowExceptionWhenArgumentValueIsNull() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> argumentHandler.getArgument(
                        "add",
                        "a",
                        null,
                        Double.class
                )
        );

        assertEquals(
                "Argument value is missing or null. The operation has been aborted.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenArgumentTypeStringRequiredConditionTypeDouble() {
        toolRegistry.loadToolRegistry();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> argumentHandler.getArgument(
                        "add",
                        "a",
                        "Hello",
                        Double.class
                )
        );

        assertEquals(
                "The argument does not meet the required conditions. The process has been aborted.",
                exception.getMessage()
        );
    }


    @Test
    void shouldThrowExceptionWhenNoFindFunction(){
        toolRegistry.loadToolRegistry();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> argumentHandler.getArgument(
                        "test",
                        "a",
                        2.0,
                        Double.class
                )
        );
        assertEquals(
                "Argument properties not found. The operation has been aborted.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenArgumentTypeBooleanRequiredConditionTypeDouble() {
        toolRegistry.loadToolRegistry();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> argumentHandler.getArgument(
                        "add",
                        "a",
                        false,
                        Double.class
                )
        );

        assertEquals(
                "The argument does not meet the required conditions. The process has been aborted.",
                exception.getMessage()
        );
    }


    @Test
    void shouldReturnCorrectDoubleValue() {
        toolRegistry.loadToolRegistry();
        Double result = argumentHandler.getArgument(
                "add",
                "a",
                2.0,
                Double.class
        );
        assertEquals(2.0, result);
        assertEquals(Double.class, result.getClass());

    }

     @Test
    void shouldReturnCorrectStringValue(){
         ToolRequestProperties testB = new ToolRequestProperties(
                 "string",
                 "test");

         ToolRequestParameters testString = new ToolRequestParameters(
                 "object",
                 new HashMap<>(Map.of("b",testB)),
                 new ArrayList<>(List.of("b"))
         );

         ToolRequestFunction calculatorAddFunction = new ToolRequestFunction(
                 "StringTest",
                 "test",
                 testString);

         this.toolRegistry.getToolregistry().put("StringTest",calculatorAddFunction);

         String result = argumentHandler.getArgument("StringTest",
                 "b",
                 "test",
                 String.class);

         assertEquals("test",result);
         assertEquals(String.class, result.getClass());

     }


}