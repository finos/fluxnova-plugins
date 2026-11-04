package org.finos.fluxnova.bpm.engine.ai.a2a.model;

/**
 * Result of an A2A JSON-RPC invocation.
 *
 * @param success      true if remote agent completed successfully
 * @param responseText concatenated text parts on success; null on failure
 * @param errorMessage descriptive error on failure; null on success
 */
public record A2aInvocationResult(
    boolean success,
    String responseText,
    String errorMessage
) {

    /**
     * Creates a successful invocation result.
     *
     * @param responseText the concatenated response text from the remote agent
     * @return a successful result
     */
    public static A2aInvocationResult success(String responseText) {
        return new A2aInvocationResult(true, responseText, null);
    }

    /**
     * Creates a failed invocation result.
     *
     * @param errorMessage descriptive error message
     * @return a failure result
     */
    public static A2aInvocationResult failure(String errorMessage) {
        return new A2aInvocationResult(false, null, errorMessage);
    }
}
