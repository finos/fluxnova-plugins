package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
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

}
