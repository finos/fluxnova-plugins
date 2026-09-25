package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link A2aAuthConfigValidator}.
 * Feature: a2a-auth, Property 2: Invalid configuration rejection
 *
 * <p><strong>Validates: Requirements 1.4, 1.5, 1.6</strong></p>
 */
class A2aAuthConfigValidationPropertyTest {

    private A2aAuthProperties properties;
    private A2aAuthConfigValidator validator;

    @BeforeTry
    void setUp() {
        properties = new A2aAuthProperties();
        validator = new A2aAuthConfigValidator(properties);
    }

    @Property(tries = 100)
    void bearerWithBlankTokenIsRejected(@ForAll("agentRefs") String agentRef,
                                         @ForAll("blanksOrNull") String token) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.BEARER);
        auth.setToken(token);
        configureAgent(agentRef, auth);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate());
        assertTrue(ex.getMessage().contains(agentRef));
        assertTrue(ex.getMessage().contains("token"));
    }

    @Property(tries = 100)
    void apiKeyWithBlankHeaderNameIsRejected(@ForAll("agentRefs") String agentRef,
                                              @ForAll("blanksOrNull") String headerName,
                                              @ForAll("validValues") String headerValue) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.API_KEY);
        auth.setHeaderName(headerName);
        auth.setHeaderValue(headerValue);
        configureAgent(agentRef, auth);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate());
        assertTrue(ex.getMessage().contains(agentRef));
        assertTrue(ex.getMessage().contains("header-name"));
    }

    @Property(tries = 100)
    void apiKeyWithBlankHeaderValueIsRejected(@ForAll("agentRefs") String agentRef,
                                               @ForAll("validValues") String headerName,
                                               @ForAll("blanksOrNull") String headerValue) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.API_KEY);
        auth.setHeaderName(headerName);
        auth.setHeaderValue(headerValue);
        configureAgent(agentRef, auth);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate());
        assertTrue(ex.getMessage().contains(agentRef));
        assertTrue(ex.getMessage().contains("header-value"));
    }

    @Property(tries = 100)
    void validBearerConfigPassesValidation(@ForAll("agentRefs") String agentRef,
                                            @ForAll("validValues") String token) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.BEARER);
        auth.setToken(token);
        configureAgent(agentRef, auth);

        assertDoesNotThrow(() -> validator.validate());
    }

    @Property(tries = 100)
    void validApiKeyConfigPassesValidation(@ForAll("agentRefs") String agentRef,
                                            @ForAll("validValues") String headerName,
                                            @ForAll("validValues") String headerValue) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.API_KEY);
        auth.setHeaderName(headerName);
        auth.setHeaderValue(headerValue);
        configureAgent(agentRef, auth);

        assertDoesNotThrow(() -> validator.validate());
    }

    @Property(tries = 50)
    void noneConfigAlwaysPassesValidation(@ForAll("agentRefs") String agentRef) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.NONE);
        configureAgent(agentRef, auth);

        assertDoesNotThrow(() -> validator.validate());
    }

    @Example
    void emptyAgentsConfigPassesValidation() {
        assertDoesNotThrow(() -> validator.validate());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void configureAgent(String agentRef, A2aAgentAuthConfig auth) {
        A2aAuthProperties.AgentEntry entry = new A2aAuthProperties.AgentEntry();
        entry.setAuth(auth);
        Map<String, A2aAuthProperties.AgentEntry> agents = new HashMap<>();
        agents.put(agentRef, entry);
        properties.setAgents(agents);
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> agentRefs() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(15)
                .map(s -> s.toLowerCase() + "-agent");
    }

    @Provide
    Arbitrary<String> blanksOrNull() {
        return Arbitraries.of(null, "", "   ", "\t");
    }

    @Provide
    Arbitrary<String> validValues() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(3).ofMaxLength(30);
    }
}
