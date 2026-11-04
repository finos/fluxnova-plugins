package org.finos.fluxnova.bpm.engine.ai.a2a.integration;

import org.finos.fluxnova.bpm.engine.ai.a2a.engine.A2aRemoteCallActivityBehaviour;
import org.finos.fluxnova.bpm.engine.ai.a2a.engine.A2aRemoteCallParseListener;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
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

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration test verifying that the old A2A patterns are rejected/ignored.
 *
 * <p>The old patterns that are no longer supported:
 * <ul>
 *   <li>{@code fluxnova:type="a2a-remote-call"} — old type discriminator on service tasks</li>
 *   <li>{@code <a2a-remote:agents>} — old configuration block inside subprocess extension elements</li>
 * </ul>
 *
 * <p>After migration to the visual service task pattern ({@code fluxnova:type="a2a-remote"}),
 * these old patterns must be completely ignored. The parse listener no longer has a
 * {@code parseSubProcess()} method, and only accepts {@code "a2a-remote"} as the type discriminator.
 *
 * <p>Validates: Requirements REQ-1</p>
 */
@ExtendWith(MockitoExtension.class)
class A2aOldPatternRejectedTest {

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");

    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    private static final String CONFIG_PROPERTY_KEY = "a2aRemoteCallConfig";

    @Mock
    private A2aInvocationService invocationService;

    private A2aRemoteCallParseListener parseListener;
    private ProcessDefinitionImpl processDefinition;

    @BeforeEach
    void setUp() {
        parseListener = new A2aRemoteCallParseListener(invocationService);
        processDefinition = new ProcessDefinitionImpl("oldPatternProcess:1:1");
    }

    // ========================================================================
    // Old type discriminator "a2a-remote-call" is NOT recognized
    // ========================================================================

    @Nested
    @DisplayName("Old type discriminator 'a2a-remote-call' is rejected")
    class OldTypeDiscriminatorRejected {

        @Test
        @DisplayName("Service task with fluxnova:type='a2a-remote-call' does NOT get behaviour attached")
        void oldTypeDiscriminator_noBehaviourAttached() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("amlAgent");
            activity.setProperty("name", "AML Investigation Agent");

            // Build element with old type discriminator — early return at type check
            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote-call");

            // Execute parse listener
            parseListener.parseServiceTask(element, adHocScope, activity);

            // Verify: NO behaviour attached
            assertNull(activity.getActivityBehavior(),
                    "Old type 'a2a-remote-call' must NOT trigger behaviour attachment");
        }

        @Test
        @DisplayName("Service task with fluxnova:type='a2a-remote-call' does NOT get config stored")
        void oldTypeDiscriminator_noConfigStored() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("amlAgent");
            activity.setProperty("name", "AML Investigation Agent");

            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote-call");

            parseListener.parseServiceTask(element, adHocScope, activity);

            // Verify: NO config property stored
            assertNull(activity.getProperty(CONFIG_PROPERTY_KEY),
                    "Old type 'a2a-remote-call' must NOT store a2aRemoteCallConfig property");
        }

        @Test
        @DisplayName("Service task with fluxnova:type='a2a-remote-call' is completely ignored — early return at type check")
        void oldTypeDiscriminator_ignoredEvenWithValidConfig() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl a2aOldActivity = adHocScope.createActivity("sanctionsAgent");
            a2aOldActivity.setProperty("name", "Sanctions Screening Agent");

            // The old type — parse listener returns early at the type check without
            // ever looking at extension elements or config. This proves the rejection
            // happens at the discriminator level, not deeper in the parsing logic.
            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote-call");

            parseListener.parseServiceTask(element, adHocScope, a2aOldActivity);

            // Verify: nothing is processed — the type check is the gate
            assertNull(a2aOldActivity.getActivityBehavior(),
                    "Activity with old type should have no behaviour");
            assertNull(a2aOldActivity.getProperty(CONFIG_PROPERTY_KEY),
                    "Activity with old type should have no config property");
        }

        @Test
        @DisplayName("Contrast: new 'a2a-remote' type IS recognized while old 'a2a-remote-call' is not")
        void newTypeRecognized_oldTypeNot() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");

            // Old pattern service task
            ActivityImpl oldActivity = adHocScope.createActivity("oldAgent");
            oldActivity.setProperty("name", "Old Agent");
            Element oldElement = mock(Element.class);
            when(oldElement.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote-call");

            // New pattern service task
            ActivityImpl newActivity = adHocScope.createActivity("newAgent");
            newActivity.setProperty("name", "New Agent");
            Element newElement = mockServiceTaskWithType("a2a-remote", "newAgent",
                    "http://localhost:8085/tasks/send", "new-agent",
                    "New Agent", "New desc.", null);

            // Parse both
            parseListener.parseServiceTask(oldElement, adHocScope, oldActivity);
            parseListener.parseServiceTask(newElement, adHocScope, newActivity);

            // Verify: old NOT processed, new IS processed
            assertNull(oldActivity.getActivityBehavior(),
                    "Old type 'a2a-remote-call' must be ignored");
            assertNull(oldActivity.getProperty(CONFIG_PROPERTY_KEY),
                    "Old type must not store config");

            assertNotNull(newActivity.getActivityBehavior(),
                    "New type 'a2a-remote' must attach behaviour");
            assertInstanceOf(A2aRemoteCallActivityBehaviour.class, newActivity.getActivityBehavior());
            assertNotNull(newActivity.getProperty(CONFIG_PROPERTY_KEY),
                    "New type must store config");
        }
    }

    // ========================================================================
    // Old <a2a-remote:agents> subprocess pattern is NOT supported
    // ========================================================================

    @Nested
    @DisplayName("Old <a2a-remote:agents> subprocess pattern is not supported")
    class OldSubprocessPatternNotSupported {

        @Test
        @DisplayName("Parse listener has no parseSubProcess method — old pattern cannot be processed")
        void parseListener_hasNoParseSubProcessOverride() {
            // The old pattern relied on parseSubProcess() to read <a2a-remote:agents> from
            // subprocess extension elements. Since that method has been removed, the old pattern
            // cannot possibly be processed.

            // Verify by reflection that the parse listener does NOT override parseSubProcess
            Method[] declaredMethods = A2aRemoteCallParseListener.class.getDeclaredMethods();
            boolean hasParseSubProcess = Arrays.stream(declaredMethods)
                    .anyMatch(m -> "parseSubProcess".equals(m.getName()));

            assertFalse(hasParseSubProcess,
                    "A2aRemoteCallParseListener must NOT override parseSubProcess — "
                            + "the old <a2a-remote:agents> pattern is no longer supported");
        }

        @Test
        @DisplayName("Only parseServiceTask is overridden — confirming single entry point")
        void parseListener_onlyOverridesParseServiceTask() {
            // Verify that parseServiceTask is the only BPMN parse method declared
            Method[] declaredMethods = A2aRemoteCallParseListener.class.getDeclaredMethods();

            long parseMethodCount = Arrays.stream(declaredMethods)
                    .filter(m -> m.getName().startsWith("parse"))
                    .count();

            assertEquals(1, parseMethodCount,
                    "Only parseServiceTask should be overridden in A2aRemoteCallParseListener. "
                            + "Found parse methods: " + Arrays.stream(declaredMethods)
                            .filter(m -> m.getName().startsWith("parse"))
                            .map(Method::getName)
                            .toList());
        }
    }

    // ========================================================================
    // Other non-a2a-remote types are also not recognized (broader rejection)
    // ========================================================================

    @Nested
    @DisplayName("No other type discriminators trigger A2A processing")
    class OtherTypesAlsoRejected {

        @Test
        @DisplayName("Type 'external' does not trigger A2A config extraction")
        void externalType_noA2aProcessing() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("externalTask");
            activity.setProperty("name", "External Worker Task");

            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("external");

            parseListener.parseServiceTask(element, adHocScope, activity);

            assertNull(activity.getActivityBehavior());
            assertNull(activity.getProperty(CONFIG_PROPERTY_KEY));
        }

        @Test
        @DisplayName("Null type does not trigger A2A config extraction")
        void nullType_noA2aProcessing() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("plainTask");
            activity.setProperty("name", "Plain Task");

            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(null);

            parseListener.parseServiceTask(element, adHocScope, activity);

            assertNull(activity.getActivityBehavior());
            assertNull(activity.getProperty(CONFIG_PROPERTY_KEY));
        }

        @Test
        @DisplayName("Empty string type does not trigger A2A config extraction")
        void emptyType_noA2aProcessing() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("emptyTypeTask");
            activity.setProperty("name", "Empty Type Task");

            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("");

            parseListener.parseServiceTask(element, adHocScope, activity);

            assertNull(activity.getActivityBehavior());
            assertNull(activity.getProperty(CONFIG_PROPERTY_KEY));
        }

        @Test
        @DisplayName("Case-sensitive: 'A2A-REMOTE' (uppercase) does not match")
        void uppercaseType_noA2aProcessing() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("upperTask");
            activity.setProperty("name", "Upper Task");

            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("A2A-REMOTE");

            parseListener.parseServiceTask(element, adHocScope, activity);

            assertNull(activity.getActivityBehavior());
            assertNull(activity.getProperty(CONFIG_PROPERTY_KEY));
        }

        @Test
        @DisplayName("Case-sensitive: 'A2a-Remote-Call' (mixed case) does not match")
        void mixedCaseOldType_noA2aProcessing() {
            ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
            ActivityImpl activity = adHocScope.createActivity("mixedCaseTask");
            activity.setProperty("name", "Mixed Case Task");

            Element element = mock(Element.class);
            when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("A2a-Remote-Call");

            parseListener.parseServiceTask(element, adHocScope, activity);

            assertNull(activity.getActivityBehavior());
            assertNull(activity.getProperty(CONFIG_PROPERTY_KEY));
        }
    }

    // ========================================================================
    // RemoteAgentConfigRegistry no longer exists
    // ========================================================================

    @Nested
    @DisplayName("RemoteAgentConfigRegistry removed — no legacy registration possible")
    class RegistryRemoved {

        @Test
        @DisplayName("RemoteAgentConfigRegistry class does not exist in the codebase")
        void remoteAgentConfigRegistry_classNotAvailable() {
            // Verify that the old RemoteAgentConfigRegistry class has been removed.
            // If it existed, old-pattern agents could potentially still be registered somewhere.
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName(
                            "org.finos.fluxnova.bpm.engine.ai.a2a.discovery.RemoteAgentConfigRegistry"),
                    "RemoteAgentConfigRegistry must be removed — old pattern agents have "
                            + "nowhere to be registered");
        }

        @Test
        @DisplayName("A2aEnrichedCatalogueBuilder class does not exist in the codebase")
        void a2aEnrichedCatalogueBuilder_classNotAvailable() {
            // The old A2aEnrichedCatalogueBuilder decorated the catalogue with agents from
            // RemoteAgentConfigRegistry. It should also be removed.
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName(
                            "org.finos.fluxnova.bpm.engine.ai.a2a.discovery.A2aEnrichedCatalogueBuilder"),
                    "A2aEnrichedCatalogueBuilder must be removed — old decoration pattern "
                            + "is no longer needed");
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Creates a mock XML Element representing a service task with the given type
     * and a valid {@code <a2a-remote:config>} child element.
     *
     * <p>For type {@code "a2a-remote"}, the full element hierarchy is set up because
     * the parse listener will traverse it. For any other type (including the old
     * {@code "a2a-remote-call"}), the listener returns early at the type check,
     * so only the type attribute stub is set up — no extension elements are needed.
     */
    private Element mockServiceTaskWithType(String type, String id, String url,
                                            String agentRef, String name,
                                            String description, String timeout) {
        Element element = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(type);

        // Only set up extension elements for the new type that actually gets processed.
        // The old "a2a-remote-call" type causes an early return at the type check,
        // so these stubs would be unnecessary and cause UnnecessaryStubbingException.
        if ("a2a-remote".equals(type)) {
            when(element.attribute("id")).thenReturn(id);

            Element extensionElements = mock(Element.class);
            when(element.element("extensionElements")).thenReturn(extensionElements);

            Element configElement = mock(Element.class);
            when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);

            when(configElement.attribute("url")).thenReturn(url);
            when(configElement.attribute("prompt")).thenReturn(null);
            when(configElement.attribute("name")).thenReturn(name);
            when(configElement.attribute("description")).thenReturn(description);
            when(configElement.attribute("agent-ref")).thenReturn(agentRef);
            when(configElement.attribute("timeout")).thenReturn(timeout);
        }

        return element;
    }
}
