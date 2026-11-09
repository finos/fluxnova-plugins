package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.config;

import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.delegate.VariableScope;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;

/**
 * Resolves {@link AgentConfig} string fields that may contain process-engine expressions
 * (e.g. {@code ${llmProvider}}, {@code #{llmModel}}, or composite text) against a
 * {@link VariableScope} at orchestration time.
 *
 * <p>Definition-time extraction keeps the raw attribute values (including expression text).
 * This resolver produces a per-instance {@link AgentConfig} for the LLM call.
 */
public final class AgentConfigExpressionResolver {

    private AgentConfigExpressionResolver() {}

    /**
     * Returns a copy of {@code config} with expression-bearing string fields evaluated against
     * {@code variableScope}.
     *
     * <p>When {@code expressionManager} is {@code null}, the input config is returned unchanged
     * (useful in unit tests outside a process-engine command context).
     *
     * @throws IllegalStateException if provider or model resolve to blank after evaluation
     */
    public static AgentConfig resolve(
            AgentConfig config, VariableScope variableScope, ExpressionManager expressionManager) {
        if (config == null || expressionManager == null) {
            return config;
        }

        String provider = requireResolved(
                "provider", config.provider(), variableScope, expressionManager);
        String model =
                requireResolved("model", config.model(), variableScope, expressionManager);
        String systemPrompt =
                resolveString(config.systemPrompt(), variableScope, expressionManager);
        String cacheStrategy =
                blankToNull(resolveString(config.cacheStrategy(), variableScope, expressionManager));
        String promptCacheKey =
                blankToNull(resolveString(config.promptCacheKey(), variableScope, expressionManager));

        if (unchanged(config.provider(), provider)
                && unchanged(config.model(), model)
                && unchanged(config.systemPrompt(), systemPrompt)
                && unchanged(config.cacheStrategy(), cacheStrategy)
                && unchanged(config.promptCacheKey(), promptCacheKey)) {
            return config;
        }

        return new AgentConfig(
                config.processDefinitionId(),
                config.elementId(),
                provider,
                model,
                systemPrompt,
                config.toolScopeElementId(),
                cacheStrategy,
                promptCacheKey);
    }

    private static String requireResolved(
            String field,
            String raw,
            VariableScope variableScope,
            ExpressionManager expressionManager) {
        String resolved = resolveString(raw, variableScope, expressionManager);
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException(
                    "AgentConfig."
                            + field
                            + " resolved to a blank value (raw='"
                            + raw
                            + "'). Set a literal or a process variable expression that evaluates "
                            + "to a non-blank string.");
        }
        return resolved;
    }

    static String resolveString(
            String raw, VariableScope variableScope, ExpressionManager expressionManager) {
        if (raw == null) {
            return null;
        }
        Object value = expressionManager.createExpression(raw).getValue(variableScope);
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean unchanged(String original, String resolved) {
        return original == null ? resolved == null : original.equals(resolved);
    }
}
