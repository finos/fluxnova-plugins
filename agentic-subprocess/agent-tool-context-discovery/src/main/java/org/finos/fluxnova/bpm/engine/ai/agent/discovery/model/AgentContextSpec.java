package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.List;

/**
 * Declares the process variables that should be visible within a BPMN scope element.
 *
 * <p>A spec with an empty {@code declaredVariables} list indicates that no variables
 * have been explicitly declared; callers may treat this as "no context" rather than
 * "all variables".
 *
 * @param processDefinitionId the process definition that owns the scope element
 * @param elementId           the id of the BPMN scope element this spec was extracted from
 * @param declaredVariables   the ordered list of variable declarations; never {@code null},
 *                            but may be empty
 */
public record AgentContextSpec(
    String processDefinitionId,
    String elementId,
    List<ContextVariableDeclaration> declaredVariables
) {}
