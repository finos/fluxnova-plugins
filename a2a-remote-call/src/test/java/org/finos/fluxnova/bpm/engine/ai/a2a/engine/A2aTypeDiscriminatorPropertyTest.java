package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import net.jqwik.api.*;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for Type Discriminator Recognition.
 *
 * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
 */
@Tag("Feature: a2a-visual-service-task, Property 1: Type Discriminator Recognition")
class A2aTypeDiscriminatorPropertyTest {

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");
    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    // ========================================================================
    // Property 1: Type Discriminator Recognition
    //
    // For any service task with fluxnova:type attribute: parse listener
    // processes it iff value is "a2a-remote"; all other values (including old
    // "a2a-remote-call") are ignored.
    // ========================================================================

    /**
     * When fluxnova:type is exactly "a2a-remote" and a valid config element is
     * present, the parse listener MUST store an A2aRemoteCallConfig on the activity.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void acceptedType_configIsStoredOnActivity(
            @ForAll("validUrls") String url,
            @ForAll("validDescriptions") String description) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockServiceTaskElement("a2a-remote", url, description);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: config is stored on the activity
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), any(A2aRemoteCallConfig.class));
    }

    /**
     * For any type value that is NOT "a2a-remote" (including null, blank, the
     * old "a2a-remote-call", or arbitrary strings), the parse listener MUST NOT
     * store any config on the activity.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void rejectedType_noConfigStoredOnActivity(
            @ForAll("nonA2aRemoteTypeValues") String typeValue) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(typeValue);

        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: no property stored, no behaviour attached
        verify(activity, never()).setProperty(any(), any());
        verify(activity, never()).setActivityBehavior(any());
    }

    /**
     * Specifically verify the old discriminator "a2a-remote-call" is rejected —
     * this was a deliberate design decision and must hold as an invariant.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void oldDiscriminator_a2aRemoteCall_isRejected(
            @ForAll("validUrls") String url,
            @ForAll("validDescriptions") String description) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        // Even though we provide a full valid config, "a2a-remote-call" should be rejected
        Element element = mockServiceTaskElement("a2a-remote-call", url, description);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: no processing happened
        verify(activity, never()).setProperty(any(), any());
        verify(activity, never()).setActivityBehavior(any());
    }

    // ========================================================================
    // Generators / Providers
    // ========================================================================

    /**
     * Generates arbitrary string values that are NOT equal to "a2a-remote".
     * Includes null, blank, the old "a2a-remote-call", and random strings.
     */
    @Provide
    Arbitrary<String> nonA2aRemoteTypeValues() {
        return Arbitraries.oneOf(
                // null value
                Arbitraries.just(null),
                // blank values
                Arbitraries.of("", " ", "  ", "\t"),
                // the old discriminator (explicitly deprecated)
                Arbitraries.just("a2a-remote-call"),
                // common type values that should be ignored
                Arbitraries.of("external", "java", "delegateExpression", "webservice"),
                // near-miss variations
                Arbitraries.of("a2a-Remote", "A2A-REMOTE", "a2a-remote ", " a2a-remote",
                        "a2a_remote", "a2aremote", "a2a-remote-"),
                // fully random strings
                Arbitraries.strings().ofMinLength(0).ofMaxLength(50)
                        .filter(s -> !"a2a-remote".equals(s))
        );
    }

    @Provide
    Arbitrary<String> validUrls() {
        return Arbitraries.strings()
                .ofMinLength(5)
                .ofMaxLength(200)
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars(':', '/', '.', '-', '_')
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200)
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '-', '_');
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Creates a fully mocked service task element with the given type value and
     * a valid {@code <a2a-remote:config>} containing the provided url and description.
     */
    private Element mockServiceTaskElement(String typeValue, String url, String description) {
        Element element = mock(Element.class);
        Element extensionElements = mock(Element.class);
        Element configElement = mock(Element.class);

        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(typeValue);
        when(element.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
        when(element.attribute("id")).thenReturn("testTask");

        when(configElement.attribute("url")).thenReturn(url);
        when(configElement.attribute("prompt")).thenReturn(null);
        when(configElement.attribute("name")).thenReturn("Test Agent");
        when(configElement.attribute("description")).thenReturn(description);
        when(configElement.attribute("agent-ref")).thenReturn("test-agent");
        when(configElement.attribute("timeout")).thenReturn(null);

        return element;
    }
}
