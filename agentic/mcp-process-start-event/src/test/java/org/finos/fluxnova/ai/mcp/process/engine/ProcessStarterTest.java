package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.finos.fluxnova.bpm.engine.variable.VariableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessStarterTest {

    @Test
    void shouldStartProcessWithBusinessKey() {
        ToolDefinition definition = new ToolDefinition("weather-process", "GetWeather", "Description", List.of(), List.of(), true);
        Map<String, Object> arguments = Map.of("businessKey", "BK-123", "location", "London");

        // Fresh mock setup for this test
        RuntimeService mockRuntimeService = mock(RuntimeService.class);
        ProcessStarter starter = new ProcessStarter(mockRuntimeService);
        
        ProcessInstantiationBuilder mockBuilder = mock(ProcessInstantiationBuilder.class);
        when(mockRuntimeService.createProcessInstanceByKey("weather-process")).thenReturn(mockBuilder);
        when(mockBuilder.businessKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.setVariables(anyMap())).thenReturn(mockBuilder);
        
        ProcessInstanceWithVariables mockInstance = mock(ProcessInstanceWithVariables.class);
        when(mockInstance.getId()).thenReturn("instance-123");
        
        // Create mock BEFORE using it in when() chain to avoid strict mode issues
        VariableMap mockVariableMap = createMockVariableMap(arguments);
        when(mockInstance.getVariables()).thenReturn(mockVariableMap);
        
        when(mockBuilder.executeWithVariablesInReturn()).thenReturn(mockInstance);

        Map<String, Object> result = starter.startProcess(definition, arguments);

        assertEquals("instance-123", result.get("processInstanceId"));
        assertEquals("BK-123", result.get("businessKey"));
        assertNotNull(result.get("message"));
        assertTrue(result.get("message").toString().contains("started"));
    }

    @Test
    void shouldStartProcessWithoutBusinessKey() {
        ToolDefinition definition = new ToolDefinition("simple-process", "SimpleTool", "Description", List.of(), List.of(), false);
        Map<String, Object> arguments = Map.of("param1", "value1");

        // Fresh mock setup for this test
        RuntimeService mockRuntimeService = mock(RuntimeService.class);
        ProcessStarter starter = new ProcessStarter(mockRuntimeService);
        
        ProcessInstantiationBuilder mockBuilder = mock(ProcessInstantiationBuilder.class);
        when(mockRuntimeService.createProcessInstanceByKey("simple-process")).thenReturn(mockBuilder);
        when(mockBuilder.businessKey(isNull())).thenReturn(mockBuilder);
        when(mockBuilder.setVariables(anyMap())).thenReturn(mockBuilder);
        
        ProcessInstanceWithVariables mockInstance = mock(ProcessInstanceWithVariables.class);
        when(mockInstance.getId()).thenReturn("instance-456");
        
        // Create mock BEFORE using it in when() chain
        VariableMap mockVariableMap = createMockVariableMap(arguments);
        when(mockInstance.getVariables()).thenReturn(mockVariableMap);
        
        when(mockBuilder.executeWithVariablesInReturn()).thenReturn(mockInstance);

        Map<String, Object> result = starter.startProcess(definition, arguments);

        assertEquals("instance-456", result.get("processInstanceId"));
        assertEquals("", result.get("businessKey"));
        assertNotNull(result.get("message"));
        assertTrue(result.get("message").toString().contains("started"));
    }

    @Test
    void shouldThrowExceptionOnFailure() {
        ToolDefinition definition = new ToolDefinition("failing-process", "FailTool", "Description", List.of(), List.of(), true);
        Map<String, Object> arguments = Map.of();

        // Fresh mock setup for this test
        RuntimeService mockRuntimeService = mock(RuntimeService.class);
        ProcessStarter starter = new ProcessStarter(mockRuntimeService);
        
        ProcessInstantiationBuilder mockBuilder = mock(ProcessInstantiationBuilder.class);
        when(mockRuntimeService.createProcessInstanceByKey("failing-process")).thenReturn(mockBuilder);
        when(mockBuilder.executeWithVariablesInReturn()).thenThrow(new RuntimeException("Process start failed"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> starter.startProcess(definition, arguments));

        assertTrue(exception.getMessage().contains("Failed to start process"));
    }

    // ===================== Integration tests for return variables =====================

    @DisplayName("startProcess: should include values field when return variables configured")
    @Test
    void shouldIncludeValuesFieldWhenReturnVariablesConfigured() {
        ToolParameter returnVar = new ToolParameter("output", "string");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", 
            List.of(), List.of(returnVar), false);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("output", "test-value");

        // Fresh mock setup for this test
        RuntimeService mockRuntimeService = mock(RuntimeService.class);
        ProcessStarter starter = new ProcessStarter(mockRuntimeService);
        
        ProcessInstantiationBuilder mockBuilder = mock(ProcessInstantiationBuilder.class);
        when(mockRuntimeService.createProcessInstanceByKey("process")).thenReturn(mockBuilder);
        when(mockBuilder.businessKey(isNull())).thenReturn(mockBuilder);
        when(mockBuilder.setVariables(anyMap())).thenReturn(mockBuilder);
        
        ProcessInstanceWithVariables mockInstance = mock(ProcessInstanceWithVariables.class);
        when(mockInstance.getId()).thenReturn("instance-456");
        
        // Create mock BEFORE using it in when() chain
        VariableMap mockVariableMap = createMockVariableMap(variables);
        when(mockInstance.getVariables()).thenReturn(mockVariableMap);
        
        when(mockBuilder.executeWithVariablesInReturn()).thenReturn(mockInstance);

        Map<String, Object> result = starter.startProcess(definition, variables);

        assertTrue(result.containsKey("values"), "Response should include 'values' field when return variables are configured");
    }

    @DisplayName("startProcess: should not include values field when no return variables configured")
    @Test
    void shouldNotIncludeValuesFieldWithoutReturnVariables() {
        ToolDefinition definition = new ToolDefinition("simple-process", "SimpleTool", "Description", List.of(), List.of(), false);
        Map<String, Object> arguments = Map.of("param1", "value1");

        // Fresh mock setup for this test
        RuntimeService mockRuntimeService = mock(RuntimeService.class);
        ProcessStarter starter = new ProcessStarter(mockRuntimeService);
        
        ProcessInstantiationBuilder mockBuilder = mock(ProcessInstantiationBuilder.class);
        when(mockRuntimeService.createProcessInstanceByKey("simple-process")).thenReturn(mockBuilder);
        when(mockBuilder.businessKey(isNull())).thenReturn(mockBuilder);
        when(mockBuilder.setVariables(anyMap())).thenReturn(mockBuilder);
        
        ProcessInstanceWithVariables mockInstance = mock(ProcessInstanceWithVariables.class);
        when(mockInstance.getId()).thenReturn("instance-456");
        
        // Create mock BEFORE using it in when() chain
        VariableMap mockVariableMap = createMockVariableMap(arguments);
        when(mockInstance.getVariables()).thenReturn(mockVariableMap);
        
        when(mockBuilder.executeWithVariablesInReturn()).thenReturn(mockInstance);

        Map<String, Object> result = starter.startProcess(definition, arguments);

        assertFalse(result.containsKey("values"));
    }

    @DisplayName("startProcess: response includes processInstanceId, businessKey, and message")
    @Test
    void shouldIncludeResponseFields() {
        ToolDefinition definition = new ToolDefinition("test-process", "TestTool", "Description", List.of(), List.of(), false);
        Map<String, Object> arguments = Map.of("businessKey", "BK-TEST", "data", "test");

        // Fresh mock setup for this test
        RuntimeService mockRuntimeService = mock(RuntimeService.class);
        ProcessStarter starter = new ProcessStarter(mockRuntimeService);
        
        ProcessInstantiationBuilder mockBuilder = mock(ProcessInstantiationBuilder.class);
        when(mockRuntimeService.createProcessInstanceByKey("test-process")).thenReturn(mockBuilder);
        when(mockBuilder.businessKey("BK-TEST")).thenReturn(mockBuilder);
        when(mockBuilder.setVariables(anyMap())).thenReturn(mockBuilder);
        
        ProcessInstanceWithVariables mockInstance = mock(ProcessInstanceWithVariables.class);
        when(mockInstance.getId()).thenReturn("inst-12345");
        
        // Create mock BEFORE using it in when() chain
        VariableMap mockVariableMap = createMockVariableMap(arguments);
        when(mockInstance.getVariables()).thenReturn(mockVariableMap);
        
        when(mockBuilder.executeWithVariablesInReturn()).thenReturn(mockInstance);

        Map<String, Object> result = starter.startProcess(definition, arguments);

        assertEquals("inst-12345", result.get("processInstanceId"));
        assertEquals("BK-TEST", result.get("businessKey"));
        assertNotNull(result.get("message"));
        assertTrue(result.get("message").toString().contains("test-process"));
        assertTrue(result.get("message").toString().contains("inst-12345"));
    }

    // ===================== Helper methods =====================

    /**
     * Creates a mock VariableMap for testing that properly implements the interface.
     * This ensures tests use the same type as production code, eliminating the need for
     * exception handling to distinguish between production and test code paths.
     */
    private VariableMap createMockVariableMap(Map<String, Object> entries) {
        VariableMap mock = mock(VariableMap.class);
        when(mock.entrySet()).thenReturn(entries.entrySet());
        return mock;
    }
}
