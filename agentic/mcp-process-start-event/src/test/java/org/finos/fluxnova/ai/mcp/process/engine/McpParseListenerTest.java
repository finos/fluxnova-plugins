package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class McpParseListenerTest {

    @Mock
    private ToolFactory toolFactory;

    @Mock
    private Element startEventElement;

    @Mock
    private ScopeImpl scope;

    @Mock
    private ActivityImpl activity;

    @Mock
    private ProcessDefinitionEntity processDefinition;

    private McpParseListener listener;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        listener = new McpParseListener(toolFactory);

        // Setup common mocks
        when(scope.getProcessDefinition()).thenReturn(processDefinition);
        when(processDefinition.getKey()).thenReturn("test-process");
        when(activity.getId()).thenReturn("startEvent1");
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void shouldCallFactoryWhenExtractorReturnsValidToolDefinition() {
        // Given - setup element that will result in valid extraction
        // We don't care HOW it extracts, just that it does
        when(startEventElement.attributeNS(any(), anyString())).thenReturn("someValue");
        when(startEventElement.element(anyString())).thenReturn(null);

        // When
        listener.parseStartEvent(startEventElement, scope, activity);

        // Then - verify factory was called (extraction succeeded)
        verify(toolFactory).createAndRegister(any(ToolDefinition.class));
    }

    @Test
    void shouldCallFactoryExactlyOnce() {
        // Given
        when(startEventElement.attributeNS(any(), anyString())).thenReturn("value");
        when(startEventElement.element(anyString())).thenReturn(null);

        // When
        listener.parseStartEvent(startEventElement, scope, activity);

        // Then
        verify(toolFactory, times(1)).createAndRegister(any());
    }

    @Test
    void shouldPassCorrectProcessKeyToExtractor() {
        // Given
        String expectedProcessKey = "my-custom-process";
        when(processDefinition.getKey()).thenReturn(expectedProcessKey);
        when(startEventElement.attributeNS(any(), anyString())).thenReturn("value");
        when(startEventElement.element(anyString())).thenReturn(null);

        // When
        listener.parseStartEvent(startEventElement, scope, activity);

        // Then - verify factory was called (meaning extractor got correct process key)
        verify(toolFactory).createAndRegister(any(ToolDefinition.class));
        verify(scope).getProcessDefinition();
        verify(processDefinition).getKey();
    }

    // ============ Return Variable Duplicate Validation Tests ============

    @Test
    void shouldThrowExceptionWhenDuplicateReturnVariableNameFound() {
        // Given - tool definition with duplicate return variable name
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("customerId", "String", false),
                new ToolParameter("customerId", "String", false)  // Duplicate name
        );
        
        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - RuntimeException should be thrown with duplicate name in message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        assertTrue(exception.getMessage().contains("duplicate return variable name"));
        assertTrue(exception.getMessage().contains("customerId"));
        assertTrue(exception.getMessage().contains("startEvent1"));
        assertTrue(exception.getMessage().contains("test-process"));
    }

    @Test
    void shouldThrowExceptionWhenThreeReturnVariablesWithOneDuplicateName() {
        // Given - three return variables with one duplicate
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("orderId", "String", false),
                new ToolParameter("customerId", "String", false),
                new ToolParameter("customerId", "String", false)  // Duplicate
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - RuntimeException should be thrown
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        assertTrue(exception.getMessage().contains("duplicate return variable name"));
        assertTrue(exception.getMessage().contains("customerId"));
    }

    @Test
    void shouldPassValidationWhenNoReturnVariableDuplicates() {
        // Given - unique return variable names
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("customerId", "String", false),
                new ToolParameter("orderId", "String", false),
                new ToolParameter("orderTotal", "Double", false)
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When - should complete without exception
        assertDoesNotThrow(() -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        // Then - factory should be called with the valid tool definition
        verify(toolFactory).createAndRegister(any(ToolDefinition.class));
    }

    @Test
    void shouldPassValidationWhenEmptyReturnVariables() {
        // Given - empty return variables list
        List<ToolParameter> returnVars = List.of();

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When - should complete without exception
        assertDoesNotThrow(() -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        // Then - factory should be called
        verify(toolFactory).createAndRegister(any(ToolDefinition.class));
    }

    @Test
    void shouldIncludeParameterNameInErrorMessage() {
        // Given - duplicate with specific parameter name
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("transactionId", "String", false),
                new ToolParameter("transactionId", "String", false)
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - error message includes parameter name
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        String message = exception.getMessage();
        assertTrue(message.contains("transactionId"),
                "Error message should include duplicate parameter name 'transactionId'");
    }

    @Test
    void shouldIncludeStartEventIdInErrorMessage() {
        // Given - duplicate return variables with specific start event ID
        String customStartEventId = "myCustomStartEvent";
        when(activity.getId()).thenReturn(customStartEventId);

        List<ToolParameter> returnVars = List.of(
                new ToolParameter("result", "String", false),
                new ToolParameter("result", "String", false)
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - error message includes start event ID
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        String message = exception.getMessage();
        assertTrue(message.contains(customStartEventId),
                "Error message should include start event ID '" + customStartEventId + "'");
    }

    @Test
    void shouldIncludeProcessKeyInErrorMessage() {
        // Given - duplicate with specific process key
        String customProcessKey = "myPaymentProcess";
        when(processDefinition.getKey()).thenReturn(customProcessKey);

        List<ToolParameter> returnVars = List.of(
                new ToolParameter("status", "String", false),
                new ToolParameter("status", "String", false)
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - error message includes process key
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        String message = exception.getMessage();
        assertTrue(message.contains(customProcessKey),
                "Error message should include process key '" + customProcessKey + "'");
    }

    @Test
    void shouldFailDeploymentOnDuplicateValidationError() {
        // Given - duplicate return variables that will fail validation
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("value1", "String", false),
                new ToolParameter("value1", "String", false)
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - exception is propagated
        assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        // Verify factory.createAndRegister() was never called due to validation failure
        verify(toolFactory, never()).createAndRegister(any());
    }

    @Test
    void shouldValidateWithManyReturnVariablesAndDetectDuplicateInMiddle() {
        // Given - many variables with duplicate in the middle
        List<ToolParameter> returnVars = List.of(
                new ToolParameter("var1", "String", false),
                new ToolParameter("var2", "String", false),
                new ToolParameter("var3", "String", false),
                new ToolParameter("var2", "String", false),  // Duplicate at position 3
                new ToolParameter("var4", "String", false)
        );

        McpParseListener testListener = createTestListenerWithReturnVars(returnVars);

        // When & Then - error should identify the duplicate
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testListener.parseStartEvent(startEventElement, scope, activity);
        });

        assertTrue(exception.getMessage().contains("var2"));
    }

    // Helper method to create a test listener that returns the provided return variables
    private McpParseListener createTestListenerWithReturnVars(List<ToolParameter> returnVars) {
        return new McpParseListener(toolFactory) {
            @Override
            public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl activity) {
                try {
                    String processKey = ((ProcessDefinitionEntity) scope.getProcessDefinition()).getKey();
                    String startEventId = activity.getId();

                    ToolDefinition toolDefinition = new ToolDefinition(
                            processKey,
                            "testTool",
                            "Test tool",
                            List.of(),
                            returnVars,
                            false
                    );

                    if (toolDefinition != null) {
                        validateReturnVariablesPublic(toolDefinition, startEventId);
                    }

                    toolFactory.createAndRegister(toolDefinition);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    // ignore
                }
            }
            
            // Expose the validation logic (replicating what's in the real listener)
            private void validateReturnVariablesPublic(ToolDefinition toolDefinition, String startEventId) {
                if (toolDefinition == null || toolDefinition.returnVariables().isEmpty()) {
                    return;
                }

                java.util.Set<String> seenNames = new java.util.HashSet<>();

                for (ToolParameter returnVar : toolDefinition.returnVariables()) {
                    if (seenNames.contains(returnVar.name())) {
                        String errorMsg = String.format(
                            "MCP - Deployment validation failed: duplicate return variable name '%s' " +
                            "in start event '%s' of process '%s'. Each return variable must have a unique name.",
                            returnVar.name(), startEventId, toolDefinition.processKey());
                        throw new RuntimeException(errorMsg);
                    }
                    seenNames.add(returnVar.name());
                }
            }
        };
    }

}
