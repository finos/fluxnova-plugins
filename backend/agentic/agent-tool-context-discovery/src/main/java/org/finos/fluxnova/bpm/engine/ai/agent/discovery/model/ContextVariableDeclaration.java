package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

/**
 * Declares a single named process variable that should be included in a context snapshot.
 *
 * <p>The {@code name} must be non-blank and correspond to a process variable name on
 * the execution at runtime. Declarations with blank names are silently ignored during
 * extraction.
 *
 * @param name the process variable name; must not be blank
 */
public record ContextVariableDeclaration(
    String name
) {}
