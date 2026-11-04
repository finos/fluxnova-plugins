package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.finos.fluxnova.bpm.engine.ai.a2a.discovery.AgentCardCache;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentCard;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.junit.jupiter.api.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based integration tests for A2A authentication.
 * Validates that auth headers are sent correctly on HTTP requests.
 *
 * <p><strong>Validates: Requirements 3.1, 3.2, 3.4, 4.1, 5.1, 5.2</strong></p>
 */
class A2aAuthIntegrationTest {

    private static WireMockServer wireMockServer;
    private A2aInvocationService invocationService;
    private AgentCardCache agentCardCache;
    private A2aAuthProperties properties;
    private A2aAuthProviderImpl authProvider;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        properties = new A2aAuthProperties();
        authProvider = new A2aAuthProviderImpl(properties);

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        invocationService = new A2aInvocationService(restClient, authProvider);
        agentCardCache = new AgentCardCache(restClient, authProvider);
    }

    // ========================================================================
    // Task 13.1: Successful authenticated invocation
    // ========================================================================

    @Test
    @DisplayName("Bearer token sent on invocation request")
    void bearerTokenSentOnInvocation() {
        String token = "test-bearer-token-xyz";
        configureBearerAgent("aml-agent", token);

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(completedResponse())));

        String url = wireMockServer.baseUrl() + "/";
        A2aInvocationResult result = invocationService.invoke(url, "test prompt", "aml-agent");

        assertTrue(result.success());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Authorization", equalTo("Bearer " + token)));
    }

    @Test
    @DisplayName("API key header sent on invocation request")
    void apiKeyHeaderSentOnInvocation() {
        String headerName = "X-API-Key";
        String headerValue = "secret-key-123";
        configureApiKeyAgent("vendor-agent", headerName, headerValue);

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(completedResponse())));

        String url = wireMockServer.baseUrl() + "/";
        A2aInvocationResult result = invocationService.invoke(url, "test prompt", "vendor-agent");

        assertTrue(result.success());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader(headerName, equalTo(headerValue)));
    }

    @Test
    @DisplayName("No auth header sent when agentRef is null")
    void noAuthHeaderWhenAgentRefNull() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(completedResponse())));

        String url = wireMockServer.baseUrl() + "/";
        A2aInvocationResult result = invocationService.invoke(url, "test prompt", null);

        assertTrue(result.success());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/"))
                .withoutHeader("Authorization"));
    }

    @Test
    @DisplayName("Bearer token sent on agent card GET request")
    void bearerTokenSentOnAgentCardFetch() {
        String token = "card-fetch-token";
        configureBearerAgent("aml-agent", token);

        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent-card.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(agentCardJson())));

        Optional<AgentCard> card = agentCardCache.getOrFetch(wireMockServer.baseUrl(), "aml-agent");

        assertTrue(card.isPresent());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/.well-known/agent-card.json"))
                .withHeader("Authorization", equalTo("Bearer " + token)));
    }

    // ========================================================================
    // Task 13.2: Auth error handling
    // ========================================================================

    @Test
    @DisplayName("HTTP 401 throws A2aAuthenticationException")
    void http401ThrowsAuthException() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("{\"error\":\"invalid_token\"}")));

        String url = wireMockServer.baseUrl() + "/";

        A2aAuthenticationException ex = assertThrows(A2aAuthenticationException.class,
                () -> invocationService.invoke(url, "prompt", "aml-agent"));

        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    @DisplayName("HTTP 403 throws A2aAuthenticationException")
    void http403ThrowsAuthException() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("Forbidden")));

        String url = wireMockServer.baseUrl() + "/";

        A2aAuthenticationException ex = assertThrows(A2aAuthenticationException.class,
                () -> invocationService.invoke(url, "prompt", null));

        assertEquals(403, ex.getStatusCode());
    }

    // ========================================================================
    // Task 13.3: Auto-configuration smoke (no-config startup)
    // ========================================================================

    @Test
    @DisplayName("No auth properties → validator passes, provider returns empty")
    void noConfigStartsCleanly() {
        A2aAuthProperties emptyProps = new A2aAuthProperties();
        A2aAuthConfigValidator validator = new A2aAuthConfigValidator(emptyProps);
        A2aAuthProviderImpl provider = new A2aAuthProviderImpl(emptyProps);

        assertDoesNotThrow(validator::validate);
        assertTrue(provider.getAuthHeaders("anything").isEmpty());
        assertTrue(provider.getAuthHeaders(null).isEmpty());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void configureBearerAgent(String agentRef, String token) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.BEARER);
        auth.setToken(token);
        A2aAuthProperties.AgentEntry entry = new A2aAuthProperties.AgentEntry();
        entry.setAuth(auth);
        Map<String, A2aAuthProperties.AgentEntry> agents = new HashMap<>(properties.getAgents());
        agents.put(agentRef, entry);
        properties.setAgents(agents);
    }

    private void configureApiKeyAgent(String agentRef, String headerName, String headerValue) {
        A2aAgentAuthConfig auth = new A2aAgentAuthConfig();
        auth.setType(A2aAgentAuthConfig.AuthType.API_KEY);
        auth.setHeaderName(headerName);
        auth.setHeaderValue(headerValue);
        A2aAuthProperties.AgentEntry entry = new A2aAuthProperties.AgentEntry();
        entry.setAuth(auth);
        Map<String, A2aAuthProperties.AgentEntry> agents = new HashMap<>(properties.getAgents());
        agents.put(agentRef, entry);
        properties.setAgents(agents);
    }

    private String completedResponse() {
        return """
                {"jsonrpc":"2.0","id":"1","result":{"status":{"state":"completed"},"artifacts":[{"parts":[{"kind":"text","text":"OK"}]}]}}
                """;
    }

    private String agentCardJson() {
        return """
                {"name":"Test Agent","description":"Test","protocolVersion":"0.3.0","skills":[]}
                """;
    }
}
