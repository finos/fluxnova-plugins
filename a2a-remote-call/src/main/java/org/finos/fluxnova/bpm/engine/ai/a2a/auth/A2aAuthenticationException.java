package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

/**
 * Thrown when a remote A2A agent returns HTTP 401 (Unauthorized) or 403 (Forbidden).
 *
 * <p>Carries the HTTP status code and response body so that callers can construct
 * meaningful BPMN error messages. The {@code A2aRemoteCallActivityBehaviour} translates
 * this exception into a {@code BpmnError} with code {@code "a2a-auth-error"}.
 */
public class A2aAuthenticationException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    /**
     * Creates an authentication exception for a rejected A2A request.
     *
     * @param statusCode   the HTTP status code (401 or 403)
     * @param responseBody the response body from the remote agent (may be null or empty)
     */
    public A2aAuthenticationException(int statusCode, String responseBody) {
        super("A2A authentication failed: HTTP " + statusCode
                + (responseBody != null && !responseBody.isBlank() ? " — " + responseBody : ""));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * @return the HTTP status code (401 or 403)
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the response body from the remote agent, or null if empty
     */
    public String getResponseBody() {
        return responseBody;
    }
}
