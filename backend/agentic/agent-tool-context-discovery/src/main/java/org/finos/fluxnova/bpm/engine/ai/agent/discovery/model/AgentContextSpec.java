package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.List;

public record AgentContextSpec(
    String processDefinitionId,
    String elementId,
    List<ContextVariableDeclaration> declaredVariables
) {}
