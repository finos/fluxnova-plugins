package org.finos.fluxnova.ai.mcp.process.engine.extractor;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.bpm.model.bpmn.instance.ExtensionElements;
import org.finos.fluxnova.bpm.model.bpmn.instance.StartEvent;
import org.finos.fluxnova.bpm.model.xml.instance.DomElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.finos.fluxnova.ai.mcp.process.model.MCPConstants.MCP_NAMESPACE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartEventToolExtractorTest {

    private StartEventToolExtractor extractor;

    @Mock
    private StartEvent startEvent;

    @Mock
    private ExtensionElements extensionElements;

    @Mock
    private DomElement domExtensions;

    @Mock
    private DomElement parametersElement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new StartEventToolExtractor();
    }

    @Test
    void shouldExtractToolDefinitionWithParameters() {
        // Setup tool attributes
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("GetWeather");
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "description")).thenReturn("Fetches weather data");
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "propagateBusinessKey")).thenReturn("true");

        // Setup extension elements
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup parameters element
        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(parametersElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);
        when(parametersElement.getLocalName()).thenReturn("parameters");
        when(parametersElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);

        // Setup parameter
        DomElement parameterElement = mock(DomElement.class);
        when(parameterElement.getLocalName()).thenReturn("parameter");
        when(parameterElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(parameterElement.getAttribute("paramName")).thenReturn("location");
        when(parameterElement.getAttribute("paramType")).thenReturn("String");

        List<DomElement> paramChildren = new ArrayList<>();
        paramChildren.add(parameterElement);
        when(parametersElement.getChildElements()).thenReturn(paramChildren);

        // Execute
        ToolDefinition result = extractor.extract(startEvent, "weather-process");

        // Verify
        assertNotNull(result);
        assertEquals("weather-process", result.processKey());
        assertEquals("GetWeather", result.toolName());
        assertEquals("Fetches weather data", result.description());
        assertEquals(2, result.parameters().size()); // location + businessKey
        assertTrue(result.propagateBusinessKey());
    }

    @Test
    void shouldReturnNullWhenNoToolName() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn(null);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenBlankToolName() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("  ");

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNull(result);
    }

    @Test
    void shouldUseEmptyDescriptionWhenNull() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "description")).thenReturn(null);
        when(startEvent.getExtensionElements()).thenReturn(null);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNotNull(result);
        assertEquals("", result.description());
    }

    @Test
    void shouldDefaultPropagateBusinessKeyToTrue() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "propagateBusinessKey")).thenReturn(null);
        when(startEvent.getExtensionElements()).thenReturn(null);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertTrue(result.propagateBusinessKey());
    }

    @Test
    void shouldNotAddBusinessKeyWhenPropagateIsFalse() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "propagateBusinessKey")).thenReturn("false");
        when(startEvent.getExtensionElements()).thenReturn(null);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertFalse(result.propagateBusinessKey());
        assertEquals(0, result.parameters().size());
    }

    @Test
    void shouldHandleNoExtensionElements() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getExtensionElements()).thenReturn(null);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNotNull(result);
        assertEquals(1, result.parameters().size()); // Only businessKey
    }

    @Test
    void shouldSkipInvalidParameters() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup parameters element
        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(parametersElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);
        when(parametersElement.getLocalName()).thenReturn("parameters");
        when(parametersElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);

        // Invalid parameter (empty name)
        DomElement invalidParam = mock(DomElement.class);
        when(invalidParam.getLocalName()).thenReturn("parameter");
        when(invalidParam.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(invalidParam.getAttribute("paramName")).thenReturn("");
        when(invalidParam.getAttribute("paramType")).thenReturn("String");

        List<DomElement> paramChildren = new ArrayList<>();
        paramChildren.add(invalidParam);
        when(parametersElement.getChildElements()).thenReturn(paramChildren);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertEquals(1, result.parameters().size()); // Only businessKey
    }

    @Test
    void shouldSkipNonMcpNamespaceParameters() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup parameters element
        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(parametersElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);
        when(parametersElement.getLocalName()).thenReturn("parameters");
        when(parametersElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);

        // Parameter with wrong namespace
        DomElement wrongNamespaceParam = mock(DomElement.class);
        when(wrongNamespaceParam.getLocalName()).thenReturn("parameter");
        when(wrongNamespaceParam.getNamespaceURI()).thenReturn("http://other.namespace");
        when(wrongNamespaceParam.getAttribute("paramName")).thenReturn("test");
        when(wrongNamespaceParam.getAttribute("paramType")).thenReturn("String");

        List<DomElement> paramChildren = new ArrayList<>();
        paramChildren.add(wrongNamespaceParam);
        when(parametersElement.getChildElements()).thenReturn(paramChildren);

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertEquals(1, result.parameters().size()); // Only businessKey
    }

    @Test
    void shouldReturnNullOnException() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName"))
                .thenThrow(new RuntimeException("Test exception"));

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNull(result);
    }

    @Test
    void shouldHandleNoParametersElement() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("Tool1");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Extension elements exist but no parameters element
        when(domExtensions.getChildElements()).thenReturn(new ArrayList<>());

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNotNull(result);
        assertEquals(1, result.parameters().size()); // Only businessKey
    }
}
