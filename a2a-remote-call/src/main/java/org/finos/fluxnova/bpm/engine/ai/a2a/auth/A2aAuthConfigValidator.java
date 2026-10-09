package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates {@link A2aAuthProperties} at application startup.
 *
 * <p>Rejects configurations that would silently fail at runtime:
 * <ul>
 *   <li>Bearer type with blank/null token</li>
 *   <li>API-key type with blank/null header-name or header-value</li>
 * </ul>
 *
 * <p>Implements {@link SmartInitializingSingleton} so validation runs after all beans
 * are initialized but before the application accepts traffic.
 */
public class A2aAuthConfigValidator implements SmartInitializingSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(A2aAuthConfigValidator.class);

    private final A2aAuthProperties properties;

    public A2aAuthConfigValidator(A2aAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    /**
     * Validates all configured agent auth entries.
     *
     * @throws IllegalStateException if any agent has an invalid auth configuration
     */
    public void validate() {
        Map<String, A2aAuthProperties.AgentEntry> agents = properties.getAgents();
        if (agents == null || agents.isEmpty()) {
            LOG.debug("No A2A agent auth configurations found — skipping validation");
            return;
        }

        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, A2aAuthProperties.AgentEntry> entry : agents.entrySet()) {
            String agentRef = entry.getKey();
            A2aAuthProperties.AgentEntry agentEntry = entry.getValue();

            if (agentEntry == null || agentEntry.getAuth() == null) {
                continue;
            }

            A2aAgentAuthConfig auth = agentEntry.getAuth();
            A2aAgentAuthConfig.AuthType type = auth.getType();

            if (type == null) {
                errors.add("Agent '" + agentRef + "': auth.type must not be null");
                continue;
            }

            switch (type) {
                case BEARER -> {
                    if (auth.getToken() == null || auth.getToken().isBlank()) {
                        errors.add("Agent '" + agentRef
                                + "': auth.type is 'bearer' but auth.token is missing or blank");
                    }
                }
                case API_KEY -> {
                    if (auth.getHeaderName() == null || auth.getHeaderName().isBlank()) {
                        errors.add("Agent '" + agentRef
                                + "': auth.type is 'api-key' but auth.header-name is missing or blank");
                    }
                    if (auth.getHeaderValue() == null || auth.getHeaderValue().isBlank()) {
                        errors.add("Agent '" + agentRef
                                + "': auth.type is 'api-key' but auth.header-value is missing or blank");
                    }
                }
                case NONE -> {
                    // No validation needed
                }
            }
        }

        if (!errors.isEmpty()) {
            String message = "Invalid A2A auth configuration:\n  - " + String.join("\n  - ", errors);
            throw new IllegalStateException(message);
        }

        LOG.debug("A2A auth configuration validated for {} agent(s)", agents.size());
    }
}
