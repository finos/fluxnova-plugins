package org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class ContextResolver {

    private static final String AGENT_VAR_PREFIX = "_agent";

    private final RuntimeService runtimeService;

    public ContextResolver(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public ResolvedContext resolve(String executionId, AgentContextSpec spec) {
        Map<String, Object> all = runtimeService.getVariables(executionId);

        Stream<Map.Entry<String, Object>> filtered = all.entrySet().stream()
                .filter(e -> !e.getKey().startsWith(AGENT_VAR_PREFIX));

        if (!spec.declaredVariables().isEmpty()) {
            Set<String> declared = spec.declaredVariables().stream()
                    .map(ContextVariableDeclaration::name)
                    .collect(toSet());
            filtered = filtered.filter(e -> declared.contains(e.getKey()));
        }

        return new ResolvedContext(
                filtered.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }
}
