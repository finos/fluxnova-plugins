package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link A2aAuthProviderImpl}.
 * Feature: a2a-auth, Property 1: Auth header resolution round-trip
 *
 * <p><strong>Validates: Requirements 1.1, 1.2, 1.3, 3.1, 3.2, 3.3, 8.2</strong></p>
 */
class A2aAuthProviderPropertyTest {

    private A2aAuthProperties properties;
    private A2aAuthProviderImpl provider;

    @BeforeTry
    void setUp() {
        properties = new A2aAuthProperties();
        provider = new A2aAuthProviderImpl(properties);
    }

    @Property(tries = 100)
    void bearerAuthResolvesToAuthorizationHeader(
            @ForAll("agentRefs") String agentRef,
            @ForAll("tokens") String token) {

        configureBearer(agentRef, token);

        Map<String, String> headers = provider.getAuthHeaders(agentRef);

        assertEquals(1, headers.size());
        assertEquals("Bearer " + token, headers.get("Authorization"));
    }

    @Property(tries = 100)
    void apiKeyAuthResolvesToCustomHeader(
            @ForAll("agentRefs") String agentRef,
            @ForAll("headerNames") String headerName,
            @ForAll("headerValues") String headerValue) {

        configureApiKey(agentRef, headerName, headerValue);

        Map<String, String> headers = provider.getAuthHeaders(agentRef);

        assertEquals(1, headers.size());
        assertEquals(headerValue, headers.get(headerName));
    }

    @Property(tries = 100)
    void noneAuthResolvesToEmptyHeaders(@ForAll("agentRefs") String agentRef) {
        configureNone(agentRef);

        Map<String, String> headers = provider.getAuthHeaders(agentRef);

        assertTrue(headers.isEmpty());
    }

    @Property(tries = 100)
    void nullAgentRefReturnsEmptyHeaders(@ForAll("tokens") String ignoredToken) {
        Map<String, String> headers = provider.getAuthHeaders(null);
        assertTrue(headers.isEmpty());
    }

    @Property(tries = 100)
    void blankAgentRefReturnsEmptyHeaders(@ForAll("blanks") String blank) {
        Map<String, String> headers = provider.getAuthHeaders(blank);
        assertTrue(headers.isEmpty());
    }

    @Property(tries = 100)
    void unknownAgentRefReturnsEmptyHeaders(@ForAll("agentRefs") String agentRef) {
        // No config registered
        Map<String, String> headers = provider.getAuthHeaders(agentRef);
        assertTrue(headers.isEmpty());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void configureBearer(String agentRef, String token) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.BEARER);
        auth.setToken(token);
        A2aAuthProperties.AgentEntry entry = new A2aAuthProperties.AgentEntry();
        entry.setAuth(auth);
        Map<String, A2aAuthProperties.AgentEntry> agents = new HashMap<>();
        agents.put(agentRef, entry);
        properties.setAgents(agents);
    }

    private void configureApiKey(String agentRef, String headerName, String headerValue) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.API_KEY);
        auth.setHeaderName(headerName);
        auth.setHeaderValue(headerValue);
        A2aAuthProperties.AgentEntry entry = new A2aAuthProperties.AgentEntry();
        entry.setAuth(auth);
        Map<String, A2aAuthProperties.AgentEntry> agents = new HashMap<>();
        agents.put(agentRef, entry);
        properties.setAgents(agents);
    }

    private void configureNone(String agentRef) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.NONE);
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
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(s -> s.toLowerCase() + "-agent");
    }

    @Provide
    Arbitrary<String> tokens() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> headerNames() {
        return Arbitraries.of("X-API-Key", "X-Agent-Token", "X-Auth", "Api-Key");
    }

    @Provide
    Arbitrary<String> headerValues() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(5).ofMaxLength(40);
    }

    @Provide
    Arbitrary<String> blanks() {
        return Arbitraries.of("", "   ", "\t", "\n");
    }
}
