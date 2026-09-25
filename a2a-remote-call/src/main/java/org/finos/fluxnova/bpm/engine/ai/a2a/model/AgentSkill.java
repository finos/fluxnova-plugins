package org.finos.fluxnova.bpm.engine.ai.a2a.model;

/**
 * A skill declared by a remote A2A agent in its agent card.
 *
 * @param name        the skill name
 * @param description a human-readable description of the skill
 */
public record AgentSkill(
    String name,
    String description
) {}
