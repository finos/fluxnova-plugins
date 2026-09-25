package org.finos.fluxnova.bpm.engine.ai.a2a.model;

import java.util.List;

/**
 * Parsed representation of a remote agent's card metadata.
 * Fetched from {@code {baseUrl}/.well-known/agent-card.json}.
 *
 * @param name            the agent name
 * @param description     a human-readable description of the agent
 * @param url             the agent's task endpoint URL (where to POST JSON-RPC requests);
 *                        may be null if not specified in the card
 * @param protocolVersion the A2A protocol version supported by the agent
 * @param skills          the list of skills the agent provides
 */
public record AgentCard(
    String name,
    String description,
    String url,
    String protocolVersion,
    List<AgentSkill> skills
) {}
