package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for auth error handling in {@link A2aInvocationService}.
 * Feature: a2a-auth, Property 4: Auth error code mapping
 *
 * <p><strong>Validates: Requirements 5.1, 5.2, 5.3, 5.4</strong></p>
 */
class A2aInvocationServiceAuthErrorPropertyTest {

    private static WireMockServer wireMockServer;
    private A2aInvocationService service;

    @BeforeContainer
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterContainer
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeTry
    void setUp() {
        wireMockServer.resetAll();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();
        service = new A2aInvocationService(restClient, agentRef -> Map.of());
    }

    @Property(tries = 50)
    void http401ThrowsA2aAuthenticationException(@ForAll("responseBodies") String responseBody) {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody(responseBody)));

        String url = wireMockServer.baseUrl() + "/";

        A2aAuthenticationException ex = assertThrows(A2aAuthenticationException.class,
                () -> service.invoke(url, "test prompt", null));

        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("401"));
    }

    @Property(tries = 50)
    void http403ThrowsA2aAuthenticationException(@ForAll("responseBodies") String responseBody) {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody(responseBody)));

        String url = wireMockServer.baseUrl() + "/";

        A2aAuthenticationException ex = assertThrows(A2aAuthenticationException.class,
                () -> service.invoke(url, "test prompt", null));

        assertEquals(403, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("403"));
    }

    @Property(tries = 20)
    void http500DoesNotThrowAuthException(@ForAll("responseBodies") String responseBody) {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody(responseBody)));

        String url = wireMockServer.baseUrl() + "/";

        // Should NOT throw A2aAuthenticationException — returns failure result instead
        var result = service.invoke(url, "test prompt", null);
        assertFalse(result.success());
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> responseBodies() {
        return Arbitraries.of(
                "",
                "Unauthorized",
                "{\"error\":\"invalid_token\"}",
                "Forbidden",
                "{\"message\":\"Access denied\"}"
        );
    }
}
