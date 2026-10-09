package org.finos.fluxnova.bpm.engine.ai.a2a.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthProvider;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthenticationException;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends synchronous A2A message/send JSON-RPC requests using Spring RestClient.
 * Clean boundary: invoke(url, prompt) → A2aInvocationResult.
 */
public class A2aInvocationService {

    private static final Logger LOG = LoggerFactory.getLogger(A2aInvocationService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final A2aAuthProvider authProvider;

    public A2aInvocationService(RestClient restClient, A2aAuthProvider authProvider) {
        this.restClient = restClient;
        this.authProvider = authProvider;
    }

    /** Backward-compatible constructor (no auth provider — no auth applied). */
    public A2aInvocationService(RestClient restClient) {
        this(restClient, agentRef -> Map.of());
    }

    /**
     * Invokes a remote A2A agent with a prompt instruction (no auth).
     *
     * @param url    the remote agent's base URL (HTTP POST target)
     * @param prompt the instruction text to send
     * @return A2aInvocationResult — success with responseText, or failure with errorMessage
     */
    public A2aInvocationResult invoke(String url, String prompt) {
        return invoke(url, prompt, null);
    }

    /**
     * Invokes a remote A2A agent with a prompt instruction and authentication.
     *
     * @param url      the remote agent's base URL (HTTP POST target)
     * @param prompt   the instruction text to send
     * @param agentRef the agent reference for auth lookup (may be null)
     * @return A2aInvocationResult — success with responseText, or failure with errorMessage
     * @throws A2aAuthenticationException if the remote agent returns HTTP 401 or 403
     */
    public A2aInvocationResult invoke(String url, String prompt, String agentRef) {
        String requestId = UUID.randomUUID().toString();
        String requestBody = buildJsonRpcRequest(requestId, prompt);

        // A2A v0.3.0: if the URL has no path (just host:port), the agent should accept
        // POST at the root. But some agents use a dedicated path. We POST to the URL as-is.
        String responseBody;
        try {
            Map<String, String> authHeaders = authProvider.getAuthHeaders(agentRef);
            var requestSpec = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);

            for (Map.Entry<String, String> header : authHeaders.entrySet()) {
                requestSpec = requestSpec.header(header.getKey(), header.getValue());
            }

            responseBody = requestSpec
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            // Detect 401/403 for auth-specific error handling
            if (isAuthError(e)) {
                int statusCode = extractStatusCode(e);
                String body = extractResponseBody(e);
                throw new A2aAuthenticationException(statusCode, body);
            }
            LOG.error("A2A invocation failed for URL '{}': {}", url, e.getMessage(), e);
            return A2aInvocationResult.failure("HTTP error invoking A2A agent at " + url + ": " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Unexpected error during A2A invocation for URL '{}': {}", url, e.getMessage(), e);
            return A2aInvocationResult.failure("Unexpected error invoking A2A agent at " + url + ": " + e.getMessage());
        }

        return parseResponse(url, responseBody);
    }

    private String buildJsonRpcRequest(String requestId, String prompt) {
        try {
            var root = OBJECT_MAPPER.createObjectNode();
            root.put("jsonrpc", "2.0");
            root.put("id", requestId);
            root.put("method", "message/send");

            var params = root.putObject("params");
            var message = params.putObject("message");
            message.put("role", "user");
            var parts = message.putArray("parts");
            var part = parts.addObject();
            part.put("kind", "text");
            part.put("text", prompt);

            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            // This should never happen with programmatic construction
            throw new IllegalStateException("Failed to construct JSON-RPC request", e);
        }
    }

    private A2aInvocationResult parseResponse(String url, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            LOG.error("A2A agent at '{}' returned empty response body", url);
            return A2aInvocationResult.failure("A2A agent at " + url + " returned empty response body");
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(responseBody);
        } catch (Exception e) {
            LOG.error("Failed to parse JSON response from A2A agent at '{}': {}", url, e.getMessage(), e);
            return A2aInvocationResult.failure("Malformed JSON response from A2A agent at " + url + ": " + e.getMessage());
        }

        // Check for JSON-RPC error
        JsonNode errorNode = root.get("error");
        if (errorNode != null && !errorNode.isNull()) {
            String errorMessage = extractJsonRpcError(errorNode);
            LOG.error("A2A agent at '{}' returned JSON-RPC error: {}", url, errorMessage);
            return A2aInvocationResult.failure("JSON-RPC error from A2A agent at " + url + ": " + errorMessage);
        }

        // Extract result
        JsonNode resultNode = root.get("result");
        if (resultNode == null || resultNode.isNull()) {
            LOG.error("A2A agent at '{}' returned response with no 'result' field", url);
            return A2aInvocationResult.failure("Malformed response from A2A agent at " + url + ": missing 'result' field");
        }

        // Check status.state
        JsonNode statusNode = resultNode.get("status");
        if (statusNode == null || statusNode.isNull()) {
            LOG.error("A2A agent at '{}' returned response with no 'result.status' field", url);
            return A2aInvocationResult.failure("Malformed response from A2A agent at " + url + ": missing 'result.status' field");
        }

        JsonNode stateNode = statusNode.get("state");
        if (stateNode == null || stateNode.isNull() || !stateNode.isTextual()) {
            LOG.error("A2A agent at '{}' returned response with no 'result.status.state' field", url);
            return A2aInvocationResult.failure("Malformed response from A2A agent at " + url + ": missing 'result.status.state' field");
        }

        String state = stateNode.asText();

        return switch (state) {
            case "completed" -> extractCompletedResult(url, resultNode);
            case "failed" -> extractFailedResult(url, statusNode);
            default -> {
                LOG.error("A2A agent at '{}' returned unsupported task state: {}", url, state);
                yield A2aInvocationResult.failure("Unsupported task state: " + state);
            }
        };
    }

    private A2aInvocationResult extractCompletedResult(String url, JsonNode resultNode) {
        JsonNode artifactsNode = resultNode.get("artifacts");
        if (artifactsNode == null || artifactsNode.isNull() || !artifactsNode.isArray() || artifactsNode.isEmpty()) {
            return A2aInvocationResult.success("");
        }

        List<String> textParts = new ArrayList<>();
        for (JsonNode artifact : artifactsNode) {
            JsonNode partsNode = artifact.get("parts");
            if (partsNode == null || !partsNode.isArray()) {
                continue;
            }
            for (JsonNode part : partsNode) {
                JsonNode kindNode = part.get("kind");
                JsonNode textNode = part.get("text");
                if (kindNode != null && "text".equals(kindNode.asText()) && textNode != null) {
                    textParts.add(textNode.asText());
                }
            }
        }

        if (textParts.isEmpty()) {
            return A2aInvocationResult.success("");
        }

        return A2aInvocationResult.success(String.join("\n", textParts));
    }

    private A2aInvocationResult extractFailedResult(String url, JsonNode statusNode) {
        JsonNode messageNode = statusNode.get("message");
        String errorDetails;
        if (messageNode != null && messageNode.isTextual() && !messageNode.asText().isBlank()) {
            errorDetails = messageNode.asText();
        } else {
            errorDetails = "Remote agent task failed (no details provided)";
        }
        LOG.error("A2A agent at '{}' reported task failure: {}", url, errorDetails);
        return A2aInvocationResult.failure(errorDetails);
    }

    private String extractJsonRpcError(JsonNode errorNode) {
        StringBuilder sb = new StringBuilder();
        JsonNode codeNode = errorNode.get("code");
        if (codeNode != null) {
            sb.append("code=").append(codeNode.asText());
        }
        JsonNode messageNode = errorNode.get("message");
        if (messageNode != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("message=").append(messageNode.asText());
        }
        if (sb.isEmpty()) {
            return errorNode.toString();
        }
        return sb.toString();
    }

    private boolean isAuthError(RestClientException e) {
        if (e instanceof org.springframework.web.client.HttpClientErrorException httpErr) {
            int code = httpErr.getStatusCode().value();
            return code == 401 || code == 403;
        }
        return false;
    }

    private int extractStatusCode(RestClientException e) {
        if (e instanceof org.springframework.web.client.HttpClientErrorException httpErr) {
            return httpErr.getStatusCode().value();
        }
        return 0;
    }

    private String extractResponseBody(RestClientException e) {
        if (e instanceof org.springframework.web.client.HttpClientErrorException httpErr) {
            return httpErr.getResponseBodyAsString();
        }
        return null;
    }
}
