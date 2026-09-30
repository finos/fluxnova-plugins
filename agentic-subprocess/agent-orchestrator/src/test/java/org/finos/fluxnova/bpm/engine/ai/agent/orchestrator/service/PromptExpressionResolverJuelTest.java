package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.delegate.VariableScope;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.delegate.DelegateInvocation;
import org.finos.fluxnova.bpm.engine.impl.el.JuelExpressionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Evaluates prompt expressions with the engine's real JUEL {@link JuelExpressionManager} (no mocked
 * expressions), to verify expression semantics end to end: mixed text, non-string values and
 * undefined variables.
 */
class PromptExpressionResolverJuelTest {

    private final JuelExpressionManager expressionManager = new JuelExpressionManager();
    private final PromptExpressionResolver resolver = new PromptExpressionResolver(() -> expressionManager);

    @BeforeEach
    void pushEngineConfiguration() {
        ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
        // Pass-through interceptor: the default one needs a live CommandContext for auth checks.
        configuration.setDelegateInterceptor(DelegateInvocation::proceed);
        Context.setProcessEngineConfiguration(configuration);
    }

    @AfterEach
    void popEngineConfiguration() {
        Context.removeProcessEngineConfiguration();
    }

    private static VariableScope scopeWith(Map<String, Object> variables) {
        VariableScope scope = mock(VariableScope.class);
        lenient().when(scope.hasVariable(anyString()))
                .thenAnswer(inv -> variables.containsKey(inv.<String>getArgument(0)));
        lenient().when(scope.getVariable(anyString()))
                .thenAnswer(inv -> variables.get(inv.<String>getArgument(0)));
        return scope;
    }

    @Test
    void resolve_expressionMixedWithText_substitutesVariables() {
        VariableScope scope = scopeWith(Map.of("alertId", "A-101", "customerName", "Acme"));

        assertEquals(Optional.of("Investigate alert A-101 for Acme."),
                resolver.resolve("Investigate alert ${alertId} for ${customerName}.", scope, "userPrompt"));
    }

    @Test
    void resolve_singleExpressionWithNumber_returnsStringValue() {
        VariableScope scope = scopeWith(Map.of("amount", 5000L));

        assertEquals(Optional.of("5000"), resolver.resolve("${amount}", scope, "userPrompt"));
    }

    @Test
    void resolve_expressionWithOperators_isEvaluated() {
        VariableScope scope = scopeWith(Map.of("amount", 90000L));

        assertEquals(Optional.of("Priority: high"),
                resolver.resolve("Priority: ${amount > 50000 ? 'high' : 'normal'}", scope, "userPrompt"));
    }

    @Test
    void resolve_undefinedVariable_returnsEmptyForFallback() {
        VariableScope scope = scopeWith(Map.of("alertId", "A-101"));

        assertTrue(resolver.resolve("Alert ${alertId} for ${customerName}", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_variableEvaluatingToEmptyString_returnsEmptyForFallback() {
        VariableScope scope = scopeWith(Map.of("taskPrompt", ""));

        assertTrue(resolver.resolve("${taskPrompt}", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_invalidExpressionSyntax_returnsEmptyForFallback() {
        VariableScope scope = scopeWith(Map.of());

        assertTrue(resolver.resolve("Broken ${alertId", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_variableWithLineBreaksAndSpecialCharacters_preservesThem() {
        String value = "Customer: O'Brien & Sons\n<Priority> \"high\" — 100% ✓";
        VariableScope scope = scopeWith(Map.of("details", value));

        assertEquals(Optional.of("Details:\n" + value),
                resolver.resolve("Details:\n${details}", scope, "userPrompt"));
    }
}
