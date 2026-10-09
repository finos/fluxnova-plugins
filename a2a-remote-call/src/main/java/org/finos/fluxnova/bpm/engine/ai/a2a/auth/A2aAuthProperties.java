package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot configuration properties for A2A remote agent authentication.
 *
 * <p>Binds to the {@code fluxnova.a2a} prefix. Per-agent auth configs are stored under
 * {@code fluxnova.a2a.agents.<agent-ref>.auth.*}.
 *
 * <p>Example configuration:
 * <pre>{@code
 * fluxnova:
 *   a2a:
 *     agents:
 *       aml-agent:
 *         auth:
 *           type: bearer
 *           token: ${AML_AGENT_TOKEN}
 *       vendor-agent:
 *         auth:
 *           type: api-key
 *           header-name: X-API-Key
 *           header-value: ${VENDOR_API_KEY}
 * }</pre>
 */
@ConfigurationProperties(prefix = "fluxnova.a2a")
public class A2aAuthProperties {

    /**
     * Per-agent authentication configurations, keyed by agent-ref name.
     */
    private Map<String, AgentEntry> agents = new HashMap<>();

    public Map<String, AgentEntry> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, AgentEntry> agents) {
        this.agents = agents;
    }

    /**
     * Wrapper for per-agent configuration containing the auth sub-properties.
     */
    public static class AgentEntry {

        private A2aAgentAuthConfig auth = new A2aAgentAuthConfig();

        public A2aAgentAuthConfig getAuth() {
            return auth;
        }

        public void setAuth(A2aAgentAuthConfig auth) {
            this.auth = auth;
        }
    }
}
