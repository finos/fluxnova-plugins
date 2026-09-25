package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link A2aRemoteCallParseListener} edge cases.
 *
 * <p>Validates: Requirements REQ-1, REQ-2</p>
 */
@ExtendWith(MockitoExtension.class)
class A2aRemoteCallParseListenerTest {

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");

    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    @Mock
    private A2aInvocationService invocationService;

    @Mock
    private ScopeImpl scope;

    @Mock
    private ActivityImpl activity;

    private A2aRemoteCallParseListener listener;

    @BeforeEach
    void setUp() {
        listener = new A2aRemoteCallParseListener(invocationService);
    }

    // ========================================================================
    // Valid config
    // ========================================================================

    @Nested
    @DisplayName("Valid a2a-remote config")
    class ValidConfig {

        @Test
        @DisplayName("should attach behaviour and store config for valid a2a-remote service task")
        void validA2aRemoteConfigAttachesBehaviourAndStoresConfig() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("amlAgent");
            when(configElement.attribute("url")).thenReturn("http://localhost:8085/tasks/send");
            when(configElement.attribute("prompt")).thenReturn("Investigate AML");
            when(configElement.attribute("name")).thenReturn("AML Agent");
            when(configElement.attribute("description")).thenReturn("AML investigation agent");
            when(configElement.attribute("agent-ref")).thenReturn("aml-agent");
            when(configElement.attribute("timeout")).thenReturn("30000");

            listener.parseServiceTask(element, scope, activity);

            verify(activity).setProperty(eq("a2aRemoteCallConfig"), any(A2aRemoteCallConfig.class));
            verify(activity).setActivityBehavior(any(A2aRemoteCallActivityBehaviour.class));
        }

        @Test
        @DisplayName("should store correct config values")
        void validConfigStoresCorrectValues() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("amlAgent");
            when(configElement.attribute("url")).thenReturn("http://localhost:8085/tasks/send");
            when(configElement.attribute("prompt")).thenReturn("Investigate AML");
            when(configElement.attribute("name")).thenReturn("AML Agent");
            when(configElement.attribute("description")).thenReturn("AML investigation agent");
            when(configElement.attribute("agent-ref")).thenReturn("aml-agent");
            when(configElement.attribute("timeout")).thenReturn("30000");

            listener.parseServiceTask(element, scope, activity);

            var configCaptor = org.mockito.ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
            verify(activity).setProperty(eq("a2aRemoteCallConfig"), configCaptor.capture());

            A2aRemoteCallConfig config = configCaptor.getValue();
            assertEquals("http://localhost:8085/tasks/send", config.url());
            assertEquals("Investigate AML", config.prompt());
            assertEquals("AML Agent", config.name());
            assertEquals("AML investigation agent", config.description());
            assertEquals("aml-agent", config.agentRef());
            assertEquals(30000L, config.timeout());
        }
    }

    // ========================================================================
    // a2a-remote-call rejected (old type no longer recognized)
    // ========================================================================

    @Nested
    @DisplayName("Old a2a-remote-call type rejected")
    class OldTypeRejected {

        @Test
        @DisplayName("should not process service task with fluxnova:type='a2a-remote-call'")
        void a2aRemoteCallTypeIsNotRecognized() {
            Element element = mockServiceTaskElement("a2a-remote-call");

            listener.parseServiceTask(element, scope, activity);

            verify(activity, never()).setProperty(any(), any());
            verify(activity, never()).setActivityBehavior(any());
        }

        @Test
        @DisplayName("should not process service task with unrelated type")
        void unrelatedTypeIsIgnored() {
            Element element = mockServiceTaskElement("external");

            listener.parseServiceTask(element, scope, activity);

            verify(activity, never()).setProperty(any(), any());
            verify(activity, never()).setActivityBehavior(any());
        }

        @Test
        @DisplayName("should not process service task with null type")
        void nullTypeIsIgnored() {
            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(null);

            listener.parseServiceTask(element, scope, activity);

            verify(activity, never()).setProperty(any(), any());
            verify(activity, never()).setActivityBehavior(any());
        }
    }

    // ========================================================================
    // Missing url (throws BpmnParseException)
    // ========================================================================

    @Nested
    @DisplayName("Missing or blank URL throws")
    class MissingUrlThrows {

        @Test
        @DisplayName("should throw BpmnParseException when url is null")
        void nullUrlThrows() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn(null);

            BpmnParseException exception = assertThrows(BpmnParseException.class,
                    () -> listener.parseServiceTask(element, scope, activity));

            assertTrue(exception.getMessage().contains("url"));
        }

        @Test
        @DisplayName("should throw BpmnParseException when url is blank")
        void blankUrlThrows() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("   ");

            BpmnParseException exception = assertThrows(BpmnParseException.class,
                    () -> listener.parseServiceTask(element, scope, activity));

            assertTrue(exception.getMessage().contains("url"));
        }

        @Test
        @DisplayName("should throw BpmnParseException when url is empty string")
        void emptyUrlThrows() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("");

            assertThrows(BpmnParseException.class,
                    () -> listener.parseServiceTask(element, scope, activity));
        }
    }

    // ========================================================================
    // Blank description (warns but still attaches)
    // ========================================================================

    @Nested
    @DisplayName("Blank description warns but attaches")
    class BlankDescriptionWarnsButAttaches {

        @Test
        @DisplayName("should attach behaviour when description is blank")
        void blankDescriptionStillAttaches() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("http://example.com/agent");
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn("Agent");
            when(configElement.attribute("description")).thenReturn("");
            when(configElement.attribute("agent-ref")).thenReturn("ref");
            when(configElement.attribute("timeout")).thenReturn(null);

            listener.parseServiceTask(element, scope, activity);

            // Still attaches behaviour
            verify(activity).setActivityBehavior(any(A2aRemoteCallActivityBehaviour.class));
            verify(activity).setProperty(eq("a2aRemoteCallConfig"), any(A2aRemoteCallConfig.class));
        }

        @Test
        @DisplayName("should attach behaviour when description is null")
        void nullDescriptionStillAttaches() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("http://example.com/agent");
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn("Agent");
            when(configElement.attribute("description")).thenReturn(null);
            when(configElement.attribute("agent-ref")).thenReturn("ref");
            when(configElement.attribute("timeout")).thenReturn(null);

            listener.parseServiceTask(element, scope, activity);

            verify(activity).setActivityBehavior(any(A2aRemoteCallActivityBehaviour.class));
        }
    }

    // ========================================================================
    // Missing extensionElements (skips)
    // ========================================================================

    @Nested
    @DisplayName("Missing extensionElements skips")
    class MissingExtensionElements {

        @Test
        @DisplayName("should skip when extensionElements is null")
        void noExtensionElementsSkips() {
            Element element = mockServiceTaskElement("a2a-remote");
            when(element.element("extensionElements")).thenReturn(null);

            listener.parseServiceTask(element, scope, activity);

            verify(activity, never()).setProperty(any(), any());
            verify(activity, never()).setActivityBehavior(any());
        }
    }

    // ========================================================================
    // Missing config element (skips)
    // ========================================================================

    @Nested
    @DisplayName("Missing config element skips")
    class MissingConfigElement {

        @Test
        @DisplayName("should skip when a2a-remote:config element is missing")
        void noConfigElementSkips() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(null);

            listener.parseServiceTask(element, scope, activity);

            verify(activity, never()).setProperty(any(), any());
            verify(activity, never()).setActivityBehavior(any());
        }
    }

    // ========================================================================
    // Timeout parsing
    // ========================================================================

    @Nested
    @DisplayName("Timeout parsing")
    class TimeoutParsing {

        @Test
        @DisplayName("should parse valid numeric timeout")
        void validTimeoutParsed() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("http://example.com/agent");
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn("Agent");
            when(configElement.attribute("description")).thenReturn("Description");
            when(configElement.attribute("agent-ref")).thenReturn("ref");
            when(configElement.attribute("timeout")).thenReturn("5000");

            listener.parseServiceTask(element, scope, activity);

            var configCaptor = org.mockito.ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
            verify(activity).setProperty(eq("a2aRemoteCallConfig"), configCaptor.capture());
            assertEquals(5000L, configCaptor.getValue().timeout());
        }

        @Test
        @DisplayName("should set timeout to null for invalid string value")
        void invalidTimeoutStringResultsInNull() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("http://example.com/agent");
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn("Agent");
            when(configElement.attribute("description")).thenReturn("Description");
            when(configElement.attribute("agent-ref")).thenReturn("ref");
            when(configElement.attribute("timeout")).thenReturn("not-a-number");

            listener.parseServiceTask(element, scope, activity);

            var configCaptor = org.mockito.ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
            verify(activity).setProperty(eq("a2aRemoteCallConfig"), configCaptor.capture());
            assertNull(configCaptor.getValue().timeout());
        }

        @Test
        @DisplayName("should set timeout to null when absent")
        void absentTimeoutResultsInNull() {
            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("http://example.com/agent");
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn("Agent");
            when(configElement.attribute("description")).thenReturn("Description");
            when(configElement.attribute("agent-ref")).thenReturn("ref");
            when(configElement.attribute("timeout")).thenReturn(null);

            listener.parseServiceTask(element, scope, activity);

            var configCaptor = org.mockito.ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
            verify(activity).setProperty(eq("a2aRemoteCallConfig"), configCaptor.capture());
            assertNull(configCaptor.getValue().timeout());
        }
    }

    // ========================================================================
    // No invocation service — config stored but no behaviour attached
    // ========================================================================

    @Nested
    @DisplayName("No invocation service provided")
    class NoInvocationService {

        @Test
        @DisplayName("should store config but not attach behaviour when invocation service is null")
        void configStoredWithoutBehaviourWhenNoInvocationService() {
            A2aRemoteCallParseListener listenerWithoutService =
                    new A2aRemoteCallParseListener();

            Element element = mockServiceTaskElement("a2a-remote");
            Element extensionElements = mock(Element.class);
            Element configElement = mock(Element.class);

            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
            when(element.attribute("id")).thenReturn("task1");
            when(configElement.attribute("url")).thenReturn("http://example.com/agent");
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn("Agent");
            when(configElement.attribute("description")).thenReturn("Description");
            when(configElement.attribute("agent-ref")).thenReturn("ref");
            when(configElement.attribute("timeout")).thenReturn(null);

            listenerWithoutService.parseServiceTask(element, scope, activity);

            verify(activity).setProperty(eq("a2aRemoteCallConfig"), any(A2aRemoteCallConfig.class));
            verify(activity, never()).setActivityBehavior(any());
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Creates a mock Element that returns the given type for
     * {@code element.attributeNS(FLUXNOVA_NS, "type")}.
     */
    private Element mockServiceTaskElement(String type) {
        Element element = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(type);
        return element;
    }
}
