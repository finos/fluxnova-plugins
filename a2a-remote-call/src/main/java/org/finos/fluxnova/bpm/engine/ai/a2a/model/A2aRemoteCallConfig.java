package org.finos.fluxnova.bpm.engine.ai.a2a.model;

/**
 * Configuration for a standalone A2A remote call service task.
 * Extracted from {@code <a2a-remote:config>} extension element.
 *
 * @param url         the remote agent URL (may be an EL expression)
 * @param prompt      the prompt instruction (may be an EL expression); nullable for agentic mode
 * @param name        a human-readable name for the remote call
 * @param description a description of what the remote call does
 * @param agentRef    optional reference to a named agent auth config (nullable)
 * @param timeout     optional call timeout in milliseconds (nullable, uses global default when null)
 */
public record A2aRemoteCallConfig(
    String url,
    String prompt,
    String name,
    String description,
    String agentRef,
    Long timeout
) {
    /** Backward-compatible 4-arg constructor (agentRef and timeout default to null). */
    public A2aRemoteCallConfig(String url, String prompt, String name, String description) {
        this(url, prompt, name, description, null, null);
    }

    /** Backward-compatible 5-arg constructor (timeout defaults to null). */
    public A2aRemoteCallConfig(String url, String prompt, String name, String description, String agentRef) {
        this(url, prompt, name, description, agentRef, null);
    }
}
