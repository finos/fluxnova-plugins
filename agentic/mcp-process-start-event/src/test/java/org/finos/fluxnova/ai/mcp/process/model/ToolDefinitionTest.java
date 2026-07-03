package org.finos.fluxnova.ai.mcp.process.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolDefinitionTest {

    @Test
    void shouldCreateValidDefinition() {
        List<ToolParameter> params = List.of(
                new ToolParameter("location", "String", false)
        );

        ToolDefinition def = new ToolDefinition(
                "weather-process",
                "GetWeather",
                "Fetches weather data",
                params,
                List.of(),
                true
        );

        assertEquals("weather-process", def.processKey());
        assertEquals("GetWeather", def.toolName());
        assertEquals("Fetches weather data", def.description());
        assertEquals(1, def.parameters().size());
        assertEquals(0, def.returnVariables().size());
        assertTrue(def.propagateBusinessKey());
    }

    @Test
    void shouldUseDefaultPropagateBusinessKey() {
        ToolDefinition def = new ToolDefinition(
                "process-1",
                "Tool1",
                "Description",
                List.of(),
                List.of(),
                true
        );

        assertTrue(def.propagateBusinessKey());
    }

    @Test
    void shouldCreateImmutableParametersList() {
        List<ToolParameter> params = List.of(
                new ToolParameter("param1", "String", false)
        );

        ToolDefinition def = new ToolDefinition(
                "process-1",
                "Tool1",
                "Description",
                params,
                List.of(),
                true
        );

        assertThrows(UnsupportedOperationException.class,
                () -> def.parameters().add(new ToolParameter("param2", "String", false)));
    }

    @Test
    void shouldHandleNullParameters() {
        ToolDefinition def = new ToolDefinition(
                "process-1",
                "Tool1",
                "Description",
                null,
                List.of(),
                true
        );

        assertNotNull(def.parameters());
        assertTrue(def.parameters().isEmpty());
    }

    @Test
    void shouldThrowExceptionForNullProcessKey() {
        assertThrows(NullPointerException.class,
                () -> new ToolDefinition(null, "Tool1", "Desc", List.of(), List.of(), true));
    }

    @Test
    void shouldThrowExceptionForNullToolName() {
        assertThrows(NullPointerException.class,
                () -> new ToolDefinition("process-1", null, "Desc", List.of(), List.of(), true));
    }

    @Test
    void shouldThrowExceptionForNullDescription() {
        assertThrows(NullPointerException.class,
                () -> new ToolDefinition("process-1", "Tool1", null, List.of(), List.of(), true));
    }

    @Test
    void shouldHandleNullReturnVariables() {
        ToolDefinition def = new ToolDefinition(
                "process-1",
                "Tool1",
                "Description",
                List.of(),
                null,
                true
        );

        assertNotNull(def.returnVariables());
        assertTrue(def.returnVariables().isEmpty());
    }

    @Test
    void shouldCreateImmutableReturnVariablesList() {
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("customerId", "String", false)
        );

        ToolDefinition def = new ToolDefinition(
                "process-1",
                "Tool1",
                "Description",
                List.of(),
                returnVars,
                true
        );

        assertThrows(UnsupportedOperationException.class,
                () -> def.returnVariables().add(new ToolParameter("orderId", "String", false)));
    }

    @Test
    void shouldStoreReturnVariables() {
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("customerId", "String", false),
                new ToolParameter("orderTotal", "Double", false)
        );

        ToolDefinition def = new ToolDefinition(
                "order-process",
                "CreateOrder",
                "Creates an order",
                List.of(),
                returnVars,
                true
        );

        assertEquals(2, def.returnVariables().size());
        assertEquals("customerId", def.returnVariables().get(0).name());
        assertEquals("orderTotal", def.returnVariables().get(1).name());
    }
}
