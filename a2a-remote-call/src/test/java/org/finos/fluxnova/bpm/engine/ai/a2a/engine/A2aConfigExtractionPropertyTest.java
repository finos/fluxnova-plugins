package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import net.jqwik.api.*;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for Config Extraction Preserves Attributes.
 *
 * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
 */
@Tag("Feature: a2a-visual-service-task, Property 2: Config Extraction Preserves Attributes")
class A2aConfigExtractionPropertyTest {

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");
    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    // ========================================================================
    // Property 2: Config Extraction Preserves Attributes
    //
    // For any <a2a-remote:config> with arbitrary non-blank url, name,
    // description, agent-ref, and timeout: the extracted A2aRemoteCallConfig
    // record contains exactly the same values; blank/absent optionals map
    // to defaults.
    // ========================================================================

    /**
     * When all config attributes are non-blank, the extracted config record
     * preserves every attribute value exactly as provided.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void allAttributesNonBlank_extractedConfigPreservesValues(
            @ForAll("nonBlankUrls") String url,
            @ForAll("nonBlankStrings") String name,
            @ForAll("nonBlankStrings") String description,
            @ForAll("nonBlankStrings") String agentRef,
            @ForAll("validTimeouts") Long timeout) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockConfigElement(url, null, name, description, agentRef, String.valueOf(timeout));
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Capture the stored config
        ArgumentCaptor<A2aRemoteCallConfig> captor = ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), captor.capture());

        A2aRemoteCallConfig config = captor.getValue();

        // Verify: all attributes preserved exactly
        assertEquals(url, config.url(), "url must be preserved");
        assertEquals(name, config.name(), "name must be preserved");
        assertEquals(description, config.description(), "description must be preserved");
        assertEquals(agentRef, config.agentRef(), "agentRef must be preserved");
        assertEquals(timeout, config.timeout(), "timeout must be preserved");
    }

    /**
     * When prompt is non-blank, it is preserved in the config record.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void nonBlankPrompt_preservedInConfig(
            @ForAll("nonBlankUrls") String url,
            @ForAll("nonBlankStrings") String prompt,
            @ForAll("nonBlankStrings") String description) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockConfigElement(url, prompt, "Agent", description, "agent-1", null);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Capture
        ArgumentCaptor<A2aRemoteCallConfig> captor = ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), captor.capture());

        A2aRemoteCallConfig config = captor.getValue();
        assertEquals(prompt, config.prompt(), "non-blank prompt must be preserved");
    }

    /**
     * When optional attributes are blank or absent, they map to defined defaults:
     * - blank/null prompt → null
     * - blank/null name → ""
     * - blank/null description → ""
     * - blank/null agentRef → null
     * - absent/invalid timeout → null
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void blankOrAbsentOptionals_mapToDefaults(
            @ForAll("nonBlankUrls") String url,
            @ForAll("blankOrNull") String prompt,
            @ForAll("blankOrNull") String name,
            @ForAll("blankOrNull") String description,
            @ForAll("blankOrNull") String agentRef) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockConfigElement(url, prompt, name, description, agentRef, null);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Capture
        ArgumentCaptor<A2aRemoteCallConfig> captor = ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), captor.capture());

        A2aRemoteCallConfig config = captor.getValue();

        // Verify defaults
        assertEquals(url, config.url(), "url is always preserved (it was non-blank)");
        assertNull(config.prompt(), "blank/null prompt must map to null");
        assertEquals("", config.name(), "blank/null name must map to empty string");
        assertEquals("", config.description(), "blank/null description must map to empty string");
        assertNull(config.agentRef(), "blank/null agentRef must map to null");
        assertNull(config.timeout(), "absent timeout must map to null");
    }

    /**
     * When timeout attribute is a non-numeric string, it is ignored and maps to null.
     *
     * <p><b>Validates: Requirements REQ-1, REQ-2</b></p>
     */
    @Property(tries = 100)
    void invalidTimeout_mapsToNull(
            @ForAll("nonBlankUrls") String url,
            @ForAll("nonBlankStrings") String description,
            @ForAll("invalidTimeoutStrings") String invalidTimeout) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallParseListener listener = new A2aRemoteCallParseListener(invocationService);

        Element element = mockConfigElement(url, null, "Agent", description, "agent-1", invalidTimeout);
        ScopeImpl scope = mock(ScopeImpl.class);
        ActivityImpl activity = mock(ActivityImpl.class);

        // Execute
        listener.parseServiceTask(element, scope, activity);

        // Capture
        ArgumentCaptor<A2aRemoteCallConfig> captor = ArgumentCaptor.forClass(A2aRemoteCallConfig.class);
        verify(activity).setProperty(eq(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY), captor.capture());

        A2aRemoteCallConfig config = captor.getValue();
        assertNull(config.timeout(), "invalid timeout string must map to null");
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
    Arbitrary<String> nonBlankStrings() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(100)
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '-', '_')
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<Long> validTimeouts() {
        return Arbitraries.longs().between(1L, 300_000L);
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

    @Provide
    Arbitrary<String> invalidTimeoutStrings() {
        return Arbitraries.oneOf(
                Arbitraries.of("abc", "12.5", "not-a-number", "1e5", "NaN", "Infinity"),
                Arbitraries.strings()
                        .ofMinLength(1)
                        .ofMaxLength(20)
                        .alpha()
                        .filter(s -> !s.isBlank())
                        .filter(s -> {
                            try {
                                Long.parseLong(s);
                                return false;
                            } catch (NumberFormatException e) {
                                return true;
                            }
                        })
        );
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Creates a fully mocked service task element with {@code fluxnova:type="a2a-remote"}
     * and an {@code <a2a-remote:config>} element with the specified attribute values.
     */
    private Element mockConfigElement(String url, String prompt, String name,
                                      String description, String agentRef, String timeout) {
        Element element = mock(Element.class);
        Element extensionElements = mock(Element.class);
        Element configElement = mock(Element.class);

        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote");
        when(element.element("extensionElements")).thenReturn(extensionElements);
        when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);
        when(element.attribute("id")).thenReturn("testTask");

        when(configElement.attribute("url")).thenReturn(url);
        when(configElement.attribute("prompt")).thenReturn(prompt);
        when(configElement.attribute("name")).thenReturn(name);
        when(configElement.attribute("description")).thenReturn(description);
        when(configElement.attribute("agent-ref")).thenReturn(agentRef);
        when(configElement.attribute("timeout")).thenReturn(timeout);

        return element;
    }
}
