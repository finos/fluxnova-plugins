package org.finos.fluxnova.bpm.engine.ai.a2a.auth;

import java.util.Collections;
import java.util.Map;

/**
 * Per-agent authentication configuration model.
 * Bound from {@code fluxnova.a2a.agents.<ref>.auth.*} application properties.
 *
 * <p>Supports three authentication types:
 * <ul>
 *   <li>{@link AuthType#BEARER} — injects {@code Authorization: Bearer <token>}</li>
 *   <li>{@link AuthType#API_KEY} — injects a custom header with configurable name and value</li>
 *   <li>{@link AuthType#NONE} — no authentication headers applied</li>
 * </ul>
 */
public class A2aAgentAuthConfig {

    /**
     * Supported authentication types for A2A remote agent communication.
     */
    public enum AuthType {
        /** OAuth2/JWT Bearer token: {@code Authorization: Bearer <token>} */
        BEARER,
        /** Custom API key header: {@code <header-name>: <header-value>} */
        API_KEY,
        /** No authentication */
        NONE
    }

    private AuthType type = AuthType.NONE;
    private String token;
    private String headerName;
    private String headerValue;

    public AuthType getType() {
        return type;
    }

    public void setType(AuthType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    /**
     * Resolves this configuration into HTTP headers to apply on outbound requests.
     *
     * @return map of header-name → header-value; empty if auth type is NONE
     */
    public Map<String, String> resolveHeaders() {
        return switch (type) {
            case BEARER -> Map.of("Authorization", "Bearer " + token);
            case API_KEY -> Map.of(headerName, headerValue);
            case NONE -> Collections.emptyMap();
        };
    }
}
