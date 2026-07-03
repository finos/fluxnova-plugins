package org.finos.fluxnova.ai.mcp.process.engine.extractor;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElementToolExtractorTest {

    private ElementToolExtractor extractor;

    @Mock
    private Element startEventElement;

    @Mock
    private Element extensionElements;

    @Mock
    private Element parametersElement;

    @Mock
    private Element parameterElement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new ElementToolExtractor();
    }

    @Test
    void shouldExtractToolDefinitionWithParameters() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("GetWeather");
        when(startEventElement.attributeNS(any(Namespace.class), eq("description"))).thenReturn("Fetches weather data");
        when(startEventElement.attributeNS(any(Namespace.class), eq("propagateBusinessKey"))).thenReturn("true");

        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(parametersElement);
        when(parametersElement.elementsNS(any(Namespace.class), eq("parameter"))).thenReturn(List.of(parameterElement));

        when(parameterElement.attribute("paramName")).thenReturn("location");
        when(parameterElement.attribute("paramType")).thenReturn("String");

        ToolDefinition result = extractor.extract(startEventElement, "weather-process");

        assertNotNull(result);
        assertEquals("weather-process", result.processKey());
        assertEquals("GetWeather", result.toolName());
        assertEquals("Fetches weather data", result.description());
        assertEquals(2, result.parameters().size()); // location + businessKey
        assertTrue(result.propagateBusinessKey());
    }

    @Test
    void shouldReturnNullWhenNoToolName() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenBlankToolName() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("  ");

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertNull(result);
    }

    @Test
    void shouldUseEmptyDescriptionWhenNull() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("Tool1");
        when(startEventElement.attributeNS(any(Namespace.class), eq("description"))).thenReturn(null);
        when(startEventElement.element("extensionElements")).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertNotNull(result);
        assertEquals("", result.description());
    }

    @Test
    void shouldDefaultPropagateBusinessKeyToTrue() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("Tool1");
        when(startEventElement.attributeNS(any(Namespace.class), eq("propagateBusinessKey"))).thenReturn(null);
        when(startEventElement.element("extensionElements")).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "process-1");
        
        assertNotNull(result);
        assertTrue(result.propagateBusinessKey());
    }

    // ==================== Return Variables Parsing Tests (4.3) ====================

    @Test
    void shouldExtractZeroReturnVariablesWhenElementMissing() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("Tool1");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(null);
        when(extensionElements.elementNS(any(Namespace.class), eq("returnVariables"))).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertTrue(result.propagateBusinessKey());
    }

    @Test
    void shouldNotAddBusinessKeyWhenPropagateIsFalse() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("Tool1");
        when(startEventElement.attributeNS(any(Namespace.class), eq("propagateBusinessKey"))).thenReturn("false");
        when(startEventElement.element("extensionElements")).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertFalse(result.propagateBusinessKey());
        assertEquals(0, result.parameters().size());
    }

    @Test
    void shouldHandleNoExtensionElements() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("Tool1");
        when(startEventElement.element("extensionElements")).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertNotNull(result);
        assertEquals(1, result.parameters().size()); // Only businessKey
    }

    @Test
    void shouldSkipInvalidParameters() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("Tool1");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(parametersElement);

        Element invalidParam = mock(Element.class);
        when(invalidParam.attribute("paramName")).thenReturn("");
        when(invalidParam.attribute("paramType")).thenReturn("String");

        when(parametersElement.elementsNS(any(Namespace.class), eq("parameter"))).thenReturn(List.of(invalidParam));

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertEquals(1, result.parameters().size()); // Only businessKey
    }

    @Test
    void shouldReturnNullOnException() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenThrow(new RuntimeException("Test exception"));

        ToolDefinition result = extractor.extract(startEventElement, "process-1");

        assertNull(result);
    }

    @Test
    void shouldExtractReturnVariables() {
        Element returnVariablesElement = mock(Element.class);
        Element returnVariableElement = mock(Element.class);

        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("MyTool");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(null);
        when(extensionElements.elementNS(any(Namespace.class), eq("returnVariables"))).thenReturn(returnVariablesElement);
        when(returnVariablesElement.elementsNS(any(Namespace.class), eq("returnVariable"))).thenReturn(List.of(returnVariableElement));

        when(returnVariableElement.attribute("paramName")).thenReturn("orderId");
        when(returnVariableElement.attribute("paramType")).thenReturn("String");

        ToolDefinition result = extractor.extract(startEventElement, "order-process");

        assertNotNull(result);
        assertEquals(1, result.returnVariables().size());
        assertEquals("orderId", result.returnVariables().get(0).name());
        assertEquals("String", result.returnVariables().get(0).type());
    }

    @Test
    void shouldHandleEmptyReturnVariables() {
        Element returnVariablesElement = mock(Element.class);

        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("MyTool");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(null);
        when(extensionElements.elementNS(any(Namespace.class), eq("returnVariables"))).thenReturn(returnVariablesElement);
        when(returnVariablesElement.elementsNS(any(Namespace.class), eq("returnVariable"))).thenReturn(List.of());

        ToolDefinition result = extractor.extract(startEventElement, "order-process");

        assertNotNull(result);
        assertEquals(0, result.returnVariables().size());
    }

    @Test
    void shouldHandleNoReturnVariablesElement() {
        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("MyTool");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(null);
        when(extensionElements.elementNS(any(Namespace.class), eq("returnVariables"))).thenReturn(null);

        ToolDefinition result = extractor.extract(startEventElement, "order-process");

        assertNotNull(result);
        assertEquals(0, result.returnVariables().size());
    }

    @Test
    void shouldExtractMultipleReturnVariables() {
        Element returnVariablesElement = mock(Element.class);
        Element returnVar1 = mock(Element.class);
        Element returnVar2 = mock(Element.class);
        Element returnVar3 = mock(Element.class);

        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("MyTool");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(null);
        when(extensionElements.elementNS(any(Namespace.class), eq("returnVariables"))).thenReturn(returnVariablesElement);
        when(returnVariablesElement.elementsNS(any(Namespace.class), eq("returnVariable")))
                .thenReturn(List.of(returnVar1, returnVar2, returnVar3));

        when(returnVar1.attribute("paramName")).thenReturn("orderId");
        when(returnVar1.attribute("paramType")).thenReturn("String");

        when(returnVar2.attribute("paramName")).thenReturn("total");
        when(returnVar2.attribute("paramType")).thenReturn("Double");

        when(returnVar3.attribute("paramName")).thenReturn("status");
        when(returnVar3.attribute("paramType")).thenReturn("String");

        ToolDefinition result = extractor.extract(startEventElement, "order-process");

        assertNotNull(result);
        assertEquals(3, result.returnVariables().size());
        assertEquals("orderId", result.returnVariables().get(0).name());
        assertEquals("total", result.returnVariables().get(1).name());
        assertEquals("status", result.returnVariables().get(2).name());
    }

    @Test
    void shouldSkipInvalidReturnVariables() {
        Element returnVariablesElement = mock(Element.class);
        Element validVar = mock(Element.class);
        Element invalidVar = mock(Element.class);

        when(startEventElement.attributeNS(any(Namespace.class), eq("toolName"))).thenReturn("MyTool");
        when(startEventElement.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("parameters"))).thenReturn(null);
        when(extensionElements.elementNS(any(Namespace.class), eq("returnVariables"))).thenReturn(returnVariablesElement);
        when(returnVariablesElement.elementsNS(any(Namespace.class), eq("returnVariable")))
                .thenReturn(List.of(validVar, invalidVar));

        when(validVar.attribute("paramName")).thenReturn("orderId");
        when(validVar.attribute("paramType")).thenReturn("String");

        when(invalidVar.attribute("paramName")).thenReturn(null);
        when(invalidVar.attribute("paramType")).thenReturn("String");

        ToolDefinition result = extractor.extract(startEventElement, "order-process");

        assertNotNull(result);
        assertEquals(1, result.returnVariables().size());
        assertEquals("orderId", result.returnVariables().get(0).name());
    }
}
