package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import java.util.Map;

/**
 * Resolves authentication headers for a given agent reference.
 *
 * <p>Implementations look up the agent-ref in the configured properties and return
 * the appropriate HTTP headers (e.g., {@code Authorization: Bearer <token>} or a
 * custom API key header).
 *
 * <p>Returns an empty map when:
 * <ul>
 *   <li>agentRef is null or blank</li>
 *   <li>agentRef has no matching configuration</li>
 *   <li>The configured auth type is {@code none}</li>
 * </ul>
 *
 * <p>Registered as a Spring bean with {@code @ConditionalOnMissingBean} to allow
 * applications to override with custom authentication logic (e.g., OAuth2 token refresh).
 */
public interface A2aAuthProvider {

    /**
     * Returns authentication headers for the given agent reference.
     *
     * @param agentRef the agent reference from BPMN config, may be null or blank
     * @return map of header-name → header-value; empty if no auth configured
     */
    Map<String, String> getAuthHeaders(String agentRef);
}
