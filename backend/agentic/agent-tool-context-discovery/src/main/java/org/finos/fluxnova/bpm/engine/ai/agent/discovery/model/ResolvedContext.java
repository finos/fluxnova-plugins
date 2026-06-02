package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.Map;

/**
 * A snapshot of process variables resolved for a specific scope execution.
 *
 * <p>The variable map contains only those variables that were both present on the
 * execution and included by the context specification used to produce this snapshot.
 * An empty map indicates that no variables were in scope — it does not mean all
 * variables are available.
 *
 * @param variables the resolved variable name-to-value pairs; never {@code null},
 *                  but may be empty
 */
public record ResolvedContext(
    Map<String, Object> variables
) {}
