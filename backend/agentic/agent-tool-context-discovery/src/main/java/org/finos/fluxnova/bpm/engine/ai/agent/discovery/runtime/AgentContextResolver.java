package org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class AgentContextResolver {

    private static final String AGENT_VAR_PREFIX = "_agent";

    private final ObjectProvider<RuntimeService> runtimeService;

    public AgentContextResolver(ObjectProvider<RuntimeService> runtimeService) {
        this.runtimeService = runtimeService;
    }

    public ResolvedContext resolve(String executionId, AgentContextSpec spec) {
        Map<String, Object> processVariables = runtimeService.getObject().getVariables(executionId);

        Set<String> declared = spec.declaredVariables().stream()
                .map(ContextVariableDeclaration::name)
                .collect(toSet());

        Map<String, Object> filtered = processVariables.entrySet().stream()
                .filter(e -> !e.getKey().startsWith(AGENT_VAR_PREFIX)
                        && (declared.isEmpty() || declared.contains(e.getKey())))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ResolvedContext(filtered);
    }
}
