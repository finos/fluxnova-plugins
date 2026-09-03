package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Default implementation of {@link A2aAuthProvider} that resolves authentication
 * headers from {@link A2aAuthProperties}.
 *
 * <p>Registered as a Spring bean with {@code @ConditionalOnMissingBean}, allowing
 * applications to override with custom logic (e.g., OAuth2 token refresh, vault lookup).
 */
public class A2aAuthProviderImpl implements A2aAuthProvider {

    private static final Logger LOG = LoggerFactory.getLogger(A2aAuthProviderImpl.class);

    private final A2aAuthProperties properties;

    public A2aAuthProviderImpl(A2aAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, String> getAuthHeaders(String agentRef) {
        if (agentRef == null || agentRef.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, A2aAuthProperties.AgentEntry> agents = properties.getAgents();
        if (agents == null) {
            return Collections.emptyMap();
        }

        A2aAuthProperties.AgentEntry agentEntry = agents.get(agentRef);
        if (agentEntry == null || agentEntry.getAuth() == null) {
            LOG.debug("No auth config found for agent-ref '{}', proceeding without auth", agentRef);
            return Collections.emptyMap();
        }

        return agentEntry.getAuth().resolveHeaders();
    }
}
