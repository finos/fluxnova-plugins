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

    // ==================== Return Variables Parsing Tests (4.4) ====================

    @Test
    void shouldExtractZeroReturnVariablesWhenElementMissing() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("MyTool");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Extension elements exist but no returnVariables element
        when(domExtensions.getChildElements()).thenReturn(new ArrayList<>());

        ToolDefinition result = extractor.extract(startEvent, "process-1");

        assertNotNull(result);
        assertEquals(0, result.returnVariables().size());
    }

    @Test
    void shouldExtractSingleReturnVariable() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("MyTool");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup return variables element
        DomElement returnVarsElement = mock(DomElement.class);
        when(returnVarsElement.getLocalName()).thenReturn("returnVariables");
        when(returnVarsElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);

        // Setup return variable
        DomElement returnVarElement = mock(DomElement.class);
        when(returnVarElement.getLocalName()).thenReturn("returnVariable");
        when(returnVarElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(returnVarElement.getAttribute("paramName")).thenReturn("orderId");
        when(returnVarElement.getAttribute("paramType")).thenReturn("String");

        List<DomElement> returnVarChildren = new ArrayList<>();
        returnVarChildren.add(returnVarElement);
        when(returnVarsElement.getChildElements()).thenReturn(returnVarChildren);

        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(returnVarsElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);

        ToolDefinition result = extractor.extract(startEvent, "order-process");

        assertNotNull(result);
        assertEquals(1, result.returnVariables().size());
        assertEquals("orderId", result.returnVariables().get(0).name());
        assertEquals("String", result.returnVariables().get(0).type());
    }

    @Test
    void shouldExtractMultipleReturnVariables() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("MyTool");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup return variables element
        DomElement returnVarsElement = mock(DomElement.class);
        when(returnVarsElement.getLocalName()).thenReturn("returnVariables");
        when(returnVarsElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);

        // Setup multiple return variables
        DomElement returnVar1 = mock(DomElement.class);
        when(returnVar1.getLocalName()).thenReturn("returnVariable");
        when(returnVar1.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(returnVar1.getAttribute("paramName")).thenReturn("orderId");
        when(returnVar1.getAttribute("paramType")).thenReturn("String");

        DomElement returnVar2 = mock(DomElement.class);
        when(returnVar2.getLocalName()).thenReturn("returnVariable");
        when(returnVar2.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(returnVar2.getAttribute("paramName")).thenReturn("total");
        when(returnVar2.getAttribute("paramType")).thenReturn("Double");

        DomElement returnVar3 = mock(DomElement.class);
        when(returnVar3.getLocalName()).thenReturn("returnVariable");
        when(returnVar3.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(returnVar3.getAttribute("paramName")).thenReturn("status");
        when(returnVar3.getAttribute("paramType")).thenReturn("String");

        List<DomElement> returnVarChildren = new ArrayList<>();
        returnVarChildren.add(returnVar1);
        returnVarChildren.add(returnVar2);
        returnVarChildren.add(returnVar3);
        when(returnVarsElement.getChildElements()).thenReturn(returnVarChildren);

        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(returnVarsElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);

        ToolDefinition result = extractor.extract(startEvent, "order-process");

        assertNotNull(result);
        assertEquals(3, result.returnVariables().size());
        assertEquals("orderId", result.returnVariables().get(0).name());
        assertEquals("total", result.returnVariables().get(1).name());
        assertEquals("status", result.returnVariables().get(2).name());
    }

    @Test
    void shouldSkipInvalidReturnVariables() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("MyTool");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup return variables element
        DomElement returnVarsElement = mock(DomElement.class);
        when(returnVarsElement.getLocalName()).thenReturn("returnVariables");
        when(returnVarsElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);

        // Valid return variable
        DomElement validVar = mock(DomElement.class);
        when(validVar.getLocalName()).thenReturn("returnVariable");
        when(validVar.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(validVar.getAttribute("paramName")).thenReturn("orderId");
        when(validVar.getAttribute("paramType")).thenReturn("String");

        // Invalid return variable (null paramName)
        DomElement invalidVar = mock(DomElement.class);
        when(invalidVar.getLocalName()).thenReturn("returnVariable");
        when(invalidVar.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(invalidVar.getAttribute("paramName")).thenReturn(null);
        when(invalidVar.getAttribute("paramType")).thenReturn("String");

        List<DomElement> returnVarChildren = new ArrayList<>();
        returnVarChildren.add(validVar);
        returnVarChildren.add(invalidVar);
        when(returnVarsElement.getChildElements()).thenReturn(returnVarChildren);

        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(returnVarsElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);

        ToolDefinition result = extractor.extract(startEvent, "order-process");

        assertNotNull(result);
        assertEquals(1, result.returnVariables().size());
        assertEquals("orderId", result.returnVariables().get(0).name());
    }

    @Test
    void shouldHandleEmptyReturnVariables() {
        when(startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName")).thenReturn("MyTool");
        when(startEvent.getExtensionElements()).thenReturn(extensionElements);
        when(extensionElements.getDomElement()).thenReturn(domExtensions);

        // Setup empty return variables element
        DomElement returnVarsElement = mock(DomElement.class);
        when(returnVarsElement.getLocalName()).thenReturn("returnVariables");
        when(returnVarsElement.getNamespaceURI()).thenReturn(MCP_NAMESPACE);
        when(returnVarsElement.getChildElements()).thenReturn(new ArrayList<>());

        List<DomElement> extensionChildren = new ArrayList<>();
        extensionChildren.add(returnVarsElement);
        when(domExtensions.getChildElements()).thenReturn(extensionChildren);

        ToolDefinition result = extractor.extract(startEvent, "order-process");

        assertNotNull(result);
        assertEquals(0, result.returnVariables().size());
    }
}
