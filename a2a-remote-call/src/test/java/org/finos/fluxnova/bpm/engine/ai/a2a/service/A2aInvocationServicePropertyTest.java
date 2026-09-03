package org.finos.fluxnova.bpm.engine.ai.a2a.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link A2aInvocationService}.
 *
 * <p>Validates: Requirements 5.1, 5.2, 5.4, 5.7, 5.9</p>
 */
class A2aInvocationServicePropertyTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WireMockServer wireMockServer;

    @BeforeProperty
    void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterProperty
    void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    private A2aInvocationService createService() {
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMockServer.baseUrl())
                .build();
        return new A2aInvocationService(restClient);
    }

    // ========================================================================
    // Property 11: JSON-RPC Request Structure Correctness
    // ========================================================================

    /**
     * Property 11: JSON-RPC Request Structure Correctness
     *
     * <p>For any prompt string, the service constructs a valid JSON-RPC 2.0 request with
     * method "message/send", a UUID id, and the prompt in params.message.parts[0].text</p>
     *
     * <p><b>Validates: Requirements 5.1, 5.2</b></p>
     */
    @Property(tries = 50)
    void jsonRpcRequestStructureIsCorrect(@ForAll("promptStrings") String prompt) throws Exception {
        // Reset WireMock state for each trial
        wireMockServer.resetAll();

        // Stub WireMock to accept any POST and return a valid completed response
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(buildCompletedResponse("ok"))));

        A2aInvocationService service = createService();
        service.invoke(wireMockServer.baseUrl() + "/", prompt);

        // Capture the request
        List<LoggedRequest> requests = wireMockServer.findAll(postRequestedFor(urlEqualTo("/")));
        assertEquals(1, requests.size(), "Expected exactly one request to be sent");

        LoggedRequest request = requests.get(0);

        // Verify Content-Type
        assertTrue(request.getHeader("Content-Type").contains("application/json"),
                "Content-Type should be application/json");

        // Parse the request body
        JsonNode root = OBJECT_MAPPER.readTree(request.getBodyAsString());

        // Verify JSON-RPC 2.0 structure
        assertEquals("2.0", root.get("jsonrpc").asText(), "jsonrpc field must be 2.0");
        assertEquals("message/send", root.get("method").asText(), "method must be message/send");

        // Verify id is a valid UUID
        String id = root.get("id").asText();
        assertNotNull(id, "id field must not be null");
        assertFalse(id.isBlank(), "id field must not be blank");
        assertDoesNotThrow(() -> UUID.fromString(id), "id must be a valid UUID");

        // Verify params.message structure
        JsonNode params = root.get("params");
        assertNotNull(params, "params field must not be null");
        JsonNode message = params.get("message");
        assertNotNull(message, "params.message must not be null");
        assertEquals("user", message.get("role").asText(), "message.role must be 'user'");

        JsonNode parts = message.get("parts");
        assertNotNull(parts, "message.parts must not be null");
        assertTrue(parts.isArray(), "message.parts must be an array");
        assertEquals(1, parts.size(), "message.parts must have exactly one element");
        assertEquals("text", parts.get(0).get("kind").asText(), "parts[0].kind must be 'text'");
        assertEquals(prompt, parts.get(0).get("text").asText(), "parts[0].text must equal the prompt");
    }

    // ========================================================================
    // Property 12: Response Text Extraction from Artifacts
    // ========================================================================

    /**
     * Property 12: Response Text Extraction from Artifacts
     *
     * <p>For any list of text parts in artifacts, the service correctly extracts and
     * concatenates them with newlines.</p>
     *
     * <p><b>Validates: Requirements 5.4</b></p>
     */
    @Property(tries = 50)
    void responseTextExtractionFromArtifacts(@ForAll("textPartLists") List<String> textParts) throws Exception {
        // Reset WireMock state for each trial
        wireMockServer.resetAll();

        // Build an A2A response with the given text parts as artifact parts
        String responseBody = buildCompletedResponseWithParts(textParts);

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(responseBody)));

        A2aInvocationService service = createService();
        A2aInvocationResult result = service.invoke(wireMockServer.baseUrl() + "/", "any prompt");

        assertTrue(result.success(), "Result should be successful for completed state");

        if (textParts.isEmpty()) {
            assertEquals("", result.responseText(),
                    "Empty text parts should produce empty response text");
        } else {
            String expected = String.join("\n", textParts);
            assertEquals(expected, result.responseText(),
                    "Response text should be text parts joined by newlines");
        }
    }

    // ========================================================================
    // Property 13: Unsupported and Malformed Responses Are Always Failures
    // ========================================================================

    /**
     * Property 13: Unsupported and Malformed Responses Are Always Failures
     *
     * <p>For any state other than "completed" or "failed", the service always returns
     * a failure result indicating unsupported task state.</p>
     *
     * <p><b>Validates: Requirements 5.7</b></p>
     */
    @Property(tries = 50)
    void unsupportedStatesAreAlwaysFailures(@ForAll("unsupportedStates") String state) throws Exception {
        // Reset WireMock state for each trial
        wireMockServer.resetAll();

        String responseBody = buildResponseWithState(state);

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(responseBody)));

        A2aInvocationService service = createService();
        A2aInvocationResult result = service.invoke(wireMockServer.baseUrl() + "/", "any prompt");

        assertFalse(result.success(), "Unsupported state '" + state + "' should yield failure");
        assertNotNull(result.errorMessage(), "Error message must not be null");
        assertTrue(result.errorMessage().contains("Unsupported task state"),
                "Error message should mention unsupported task state, got: " + result.errorMessage());
    }

    /**
     * Property 13 (continued): Malformed response bodies always produce failures.
     *
     * <p><b>Validates: Requirements 5.9</b></p>
     */
    @Property(tries = 50)
    void malformedResponseBodiesAreAlwaysFailures(@ForAll("malformedBodies") String body) throws Exception {
        // Reset WireMock state for each trial
        wireMockServer.resetAll();

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(body)));

        A2aInvocationService service = createService();
        A2aInvocationResult result = service.invoke(wireMockServer.baseUrl() + "/", "any prompt");

        assertFalse(result.success(), "Malformed body should yield failure");
        assertNotNull(result.errorMessage(), "Error message must not be null for malformed response");
        assertFalse(result.errorMessage().isBlank(),
                "Error message must not be blank for malformed response");
    }

    // ========================================================================
    // Generators / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> promptStrings() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(500)
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '!', '?', '-', '_', ':', '/', '(', ')');
    }

    @Provide
    Arbitrary<List<String>> textPartLists() {
        Arbitrary<String> textPart = Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200)
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '!', '?', '-', '_');
        return textPart.list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<String> unsupportedStates() {
        return Arbitraries.of(
                "working", "input-required", "pending", "submitted",
                "running", "queued", "paused", "canceled", "unknown"
        ).flatMap(base -> Arbitraries.of(base, base.toUpperCase(), base + "-extra"));
    }

    @Provide
    Arbitrary<String> malformedBodies() {
        return Arbitraries.oneOf(
                // Not valid JSON at all
                Arbitraries.of(
                        "not json at all",
                        "{invalid",
                        "[]",
                        "123"
                ),
                // Valid JSON but missing required fields
                Arbitraries.of(
                        "{}",
                        "{\"jsonrpc\":\"2.0\"}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\"}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"status\":{}}}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"status\":{\"state\":null}}}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"artifacts\":[]}}"
                ),
                // JSON-RPC error responses
                Arbitraries.of(
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32600,\"message\":\"Invalid Request\"}}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}",
                        "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}"
                )
        );
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String buildCompletedResponse(String text) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": "resp-1",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [
                          { "kind": "text", "text": "%s" }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(text);
    }

    private String buildCompletedResponseWithParts(List<String> textParts) {
        if (textParts.isEmpty()) {
            return """
                    {
                      "jsonrpc": "2.0",
                      "id": "resp-1",
                      "result": {
                        "status": { "state": "completed" },
                        "artifacts": []
                      }
                    }
                    """;
        }

        String partsJson = textParts.stream()
                .map(text -> {
                    // Escape for JSON using Jackson to ensure correctness
                    try {
                        String escaped = OBJECT_MAPPER.writeValueAsString(text);
                        return "{\"kind\":\"text\",\"text\":" + escaped + "}";
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining(","));

        return """
                {
                  "jsonrpc": "2.0",
                  "id": "resp-1",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [%s]
                      }
                    ]
                  }
                }
                """.formatted(partsJson);
    }

    private String buildResponseWithState(String state) {
        // Use Jackson to properly escape the state string in case it has special chars
        try {
            String escapedState = OBJECT_MAPPER.writeValueAsString(state);
            // escapedState includes surrounding quotes, remove them for inline use
            String stateValue = escapedState.substring(1, escapedState.length() - 1);
            return """
                    {
                      "jsonrpc": "2.0",
                      "id": "resp-1",
                      "result": {
                        "status": { "state": "%s" }
                      }
                    }
                    """.formatted(stateValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
