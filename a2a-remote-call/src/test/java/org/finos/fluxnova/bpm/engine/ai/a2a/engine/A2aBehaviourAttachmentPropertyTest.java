package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import net.jqwik.api.*;
import org.finos.fluxnova.bpm.engine.BpmnParseException;
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
 * Property-based test for Behaviour Attachment Correctness.
 *
 * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
 */
@Tag("Feature: a2a-visual-service-task, Property 3: Behaviour Attachment Correctness")
class A2aBehaviourAttachmentPropertyTest {

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");
    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    // ========================================================================
    // Property 3: Behaviour Attachment Correctness
    //
    // For any service task with fluxnova:type="a2a-remote": the
    // A2aRemoteCallActivityBehaviour SHALL be attached to the activity if and
    // only if the <a2a-remote:config> element is present with a non-blank url.
    // If url is blank, a BpmnParseException is thrown.
    // If <extensionElements> or <a2a-remote:config> is missing, no behaviour
    // is attached (early return).
    // ========================================================================

    /**
     * When fluxnova:type="a2a-remote" and a valid config is present with
     * non-blank url, the behaviour MUST be attached regardless of whether
     * description is blank or non-blank.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void validConfig_withNonBlankUrl_behaviourIsAttached(
            @ForAll("nonBlankUrls") String url,
            @ForAll("optionalDescriptions") String description) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockFullServiceTask(url, description);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: behaviour IS attached
        verify(activity).setActivityBehavior(any(A2aRemoteCallActivityBehaviour.class));
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), any());
    }

    /**
     * When fluxnova:type="a2a-remote" and config is present but url is blank,
     * a BpmnParseException MUST be thrown and no behaviour is attached.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void configPresent_withBlankUrl_throwsExceptionAndNoBehaviour(
            @ForAll("blankOrNull") String blankUrl,
            @ForAll("optionalDescriptions") String description) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockFullServiceTask(blankUrl, description);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute & Verify: BpmnParseException is thrown
        assertThrows(BpmnParseException.class,
                () -> listener.parseServiceTask(element, scope, activity));

        // Verify: no behaviour attached
        verify(activity, never()).setActivityBehavior(any());
        verify(activity, never()).setProperty(any(), any());
    }

    /**
     * When fluxnova:type="a2a-remote" but <extensionElements> is missing,
     * the parse listener returns early and no behaviour is attached.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void missingExtensionElements_noBehaviourAttached(
            @ForAll("nonBlankUrls") String url) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote");
        when(element.element("extensionElements")).thenReturn(null);

        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: no behaviour attached
        verify(activity, never()).setActivityBehavior(any());
        verify(activity, never()).setProperty(any(), any());
    }

    /**
     * When fluxnova:type="a2a-remote" and <extensionElements> exists but
     * <a2a-remote:config> element is missing, no behaviour is attached.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void missingConfigElement_noBehaviourAttached(
            @ForAll("nonBlankUrls") String url) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mock(Element.class);
        Element extensionElements = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote");
        when(element.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(null);

        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: no behaviour attached
        verify(activity, never()).setActivityBehavior(any());
        verify(activity, never()).setProperty(any(), any());
    }

    /**
     * When invocationService is null (listener created without it), behaviour
     * is NOT attached even when config is valid — but config IS still stored.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void nullInvocationService_configStoredButNoBehaviourAttached(
            @ForAll("nonBlankUrls") String url,
            @ForAll("optionalDescriptions") String description) {

        // Setup: no invocation service
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener();

        Element element = mockFullServiceTask(url, description);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Verify: config is stored but behaviour is NOT attached
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), any());
        verify(activity, never()).setActivityBehavior(any());
    }

    // ========================================================================
    // Generators / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> nonBlankUrls() {
        return Arbitraries.strings()
                .ofMinLength(5)
                .ofMaxLength(200)
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars(':', '/', '.', '-', '_')
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> optionalDescriptions() {
        return Arbitraries.oneOf(
                // non-blank descriptions
                Arbitraries.strings()
                        .ofMinLength(1)
                        .ofMaxLength(100)
                        .alpha()
                        .numeric()
                        .withChars(' ', '.', ',', '-', '_')
                        .filter(s -> !s.isBlank()),
                // blank/null descriptions (still should allow attachment)
                Arbitraries.of(null, "", " ", "\t")
        );
    }

    @Provide
    Arbitrary<String> blankOrNull() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just(" "),
                Arbitraries.just("  "),
                Arbitraries.just("\t")
        );
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Creates a fully mocked service task element with {@code fluxnova:type="a2a-remote"}
     * and a {@code <a2a-remote:config>} element with the specified url and description.
     */
    private Element mockFullServiceTask(String url, String description) {
        Element element = mock(Element.class);
        Element extensionElements = mock(Element.class);
        Element configElement = mock(Element.class);

        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote");
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
