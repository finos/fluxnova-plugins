package org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;

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

    private static final String AGENT_VAR_PREFIX = "_agent";

    /**
     * Resolves the process variables visible to an agent scope into a {@link ResolvedContext}.
     *
     * <p>Reads the variables of the given execution and returns only those explicitly named
     * in the spec's {@code declaredVariables}. Variables whose names begin with the internal
     * {@code _agent} prefix are always excluded, and an empty declaration list yields an empty
     * context (nothing is exposed unless explicitly declared).
     *
     * @param runtimeService the runtime service used to read the execution's variables
     * @param executionId    the execution (agent scope) whose variables are read
     * @param spec           the context specification declaring which variables to expose
     * @return a resolved context containing only the declared, non-internal variables
     */
    public ResolvedContext resolve(RuntimeService runtimeService, String executionId, AgentContextSpec spec) {
        Map<String, Object> processVariables = runtimeService.getVariables(executionId);

        Set<String> declared = spec.declaredVariables().stream()
                .map(ContextVariableDeclaration::name)
                .collect(toSet());

        Map<String, Object> filtered = processVariables.entrySet().stream()
                .filter(e -> !e.getKey().startsWith(AGENT_VAR_PREFIX)
                        && declared.contains(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ResolvedContext(filtered);
    }
}