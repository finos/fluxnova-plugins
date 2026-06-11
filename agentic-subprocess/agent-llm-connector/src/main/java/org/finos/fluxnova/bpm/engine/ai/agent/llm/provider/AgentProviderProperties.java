package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Explicit provider-id → bean-name overrides. Used for non-standard ChatModel beans
 * whose names do not follow Spring AI's {@code <provider>ChatModel} convention, or to
 * remap a standard provider to a different bean instance.
 */
@ConfigurationProperties(prefix = "fluxnova.ai.agent")
public class AgentProviderProperties {

    /** providerId → ChatModel bean name */
    private Map<String, String> providerOverrides = new HashMap<>();

    public Map<String, String> getProviderOverrides() {
        return providerOverrides;
    }

    public void setProviderOverrides(Map<String, String> providerOverrides) {
        this.providerOverrides = providerOverrides == null ? new HashMap<>() : providerOverrides;
    }
}
