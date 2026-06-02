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

/**
 * Resolves the runtime variable snapshot for a scope execution given a context
 * specification.
 *
 * <p>Reads all variables visible on the given execution and filters them down to
 * only those declared in the {@link AgentContextSpec}. This gives callers a
 * controlled, minimal view of process state rather than exposing every variable
 * on the execution.
 *
 * <p>When the spec declares no variables, the returned {@link ResolvedContext}
 * will contain an empty variable map.
 *
 * @see AgentContextSpec
 * @see ResolvedContext
 */
public class AgentContextResolver {

    private final ObjectProvider<RuntimeService> runtimeService;

    public AgentContextResolver(ObjectProvider<RuntimeService> runtimeService) {
        this.runtimeService = runtimeService;
    }

    /**
     * Resolves the context variables for the given execution.
     *
     * @param executionId the id of the scope execution whose variables should be read
     * @param spec        the context specification declaring which variable names to
     *                    include; must not be {@code null}
     * @return a {@link ResolvedContext} containing only the variables whose names appear
     *         in {@code spec.declaredVariables()}; never {@code null}
     */
    public ResolvedContext resolve(String executionId, AgentContextSpec spec) {
        Map<String, Object> processVariables = runtimeService.getObject().getVariables(executionId);

        Set<String> declared = spec.declaredVariables().stream()
                .map(ContextVariableDeclaration::name)
                .collect(toSet());

        Map<String, Object> filtered = processVariables.entrySet().stream()
                .filter(e -> declared.contains(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ResolvedContext(filtered);
    }
}