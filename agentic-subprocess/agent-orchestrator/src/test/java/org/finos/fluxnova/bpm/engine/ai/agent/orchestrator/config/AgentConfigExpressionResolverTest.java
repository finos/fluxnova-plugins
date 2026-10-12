package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.config;

import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.delegate.VariableScope;
import org.finos.fluxnova.bpm.engine.impl.el.Expression;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentConfigExpressionResolverTest {

    private static final String PROC_DEF_ID = "proc:1";
    private static final String ELEMENT_ID = "agentSubprocess";

    @Mock
    private VariableScope variableScope;

    @Mock
    private ExpressionManager expressionManager;

    @Test
    void resolve_whenExpressionManagerNull_returnsSameInstance() {
        AgentConfig config = literalConfig("openai", "gpt-5.4", "Be careful.");

        assertSame(config, AgentConfigExpressionResolver.resolve(config, variableScope, null));
    }

    @Test
    void resolve_evaluatesProviderModelAndSystemPrompt() {
        AgentConfig config =
                literalConfig("${llmProvider}", "${llmModel}", "Decide for ${applicantName}");
        stubExpression("${llmProvider}", "anthropic");
        stubExpression("${llmModel}", "claude-sonnet-4-6");
        stubExpression("Decide for ${applicantName}", "Decide for Ada");

        AgentConfig resolved =
                AgentConfigExpressionResolver.resolve(config, variableScope, expressionManager);

        assertEquals("anthropic", resolved.provider());
        assertEquals("claude-sonnet-4-6", resolved.model());
        assertEquals("Decide for Ada", resolved.systemPrompt());
        assertEquals(PROC_DEF_ID, resolved.processDefinitionId());
        assertEquals(ELEMENT_ID, resolved.elementId());
        assertEquals(ELEMENT_ID, resolved.toolScopeElementId());
    }

    @Test
    void resolve_evaluatesCacheStrategyAndPromptCacheKey() {
        AgentConfig config =
                new AgentConfig(
                        PROC_DEF_ID,
                        ELEMENT_ID,
                        "anthropic",
                        "claude-sonnet-5",
                        null,
                        ELEMENT_ID,
                        "${cacheStrategy}",
                        "${promptCacheKey}");
        stubExpression("anthropic", "anthropic");
        stubExpression("claude-sonnet-5", "claude-sonnet-5");
        stubExpression("${cacheStrategy}", "SYSTEM_AND_TOOLS");
        stubExpression("${promptCacheKey}", "loan-approval");

        AgentConfig resolved =
                AgentConfigExpressionResolver.resolve(config, variableScope, expressionManager);

        assertEquals("SYSTEM_AND_TOOLS", resolved.cacheStrategy());
        assertEquals("loan-approval", resolved.promptCacheKey());
    }

    @Test
    void resolve_blankCacheFieldsBecomeNull() {
        AgentConfig config =
                new AgentConfig(
                        PROC_DEF_ID,
                        ELEMENT_ID,
                        "openai",
                        "gpt-5.4",
                        null,
                        ELEMENT_ID,
                        "${cacheStrategy}",
                        "${promptCacheKey}");
        stubExpression("openai", "openai");
        stubExpression("gpt-5.4", "gpt-5.4");
        stubExpression("${cacheStrategy}", "  ");
        stubExpression("${promptCacheKey}", null);

        AgentConfig resolved =
                AgentConfigExpressionResolver.resolve(config, variableScope, expressionManager);

        assertEquals(null, resolved.cacheStrategy());
        assertEquals(null, resolved.promptCacheKey());
    }

    @Test
    void resolve_whenAllLiteralsUnchanged_returnsSameInstance() {
        AgentConfig config = literalConfig("openai", "gpt-5.4", "Be careful.");
        stubExpression("openai", "openai");
        stubExpression("gpt-5.4", "gpt-5.4");
        stubExpression("Be careful.", "Be careful.");

        AgentConfig resolved =
                AgentConfigExpressionResolver.resolve(config, variableScope, expressionManager);

        assertSame(config, resolved);
    }

    @Test
    void resolve_whenProviderResolvesBlank_throws() {
        AgentConfig config = literalConfig("${missingProvider}", "gpt-5.4", null);
        stubExpression("${missingProvider}", "  ");

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> AgentConfigExpressionResolver.resolve(
                                config, variableScope, expressionManager));

        assertTrue(ex.getMessage().contains("provider"));
    }

    @Test
    void resolve_whenModelResolvesNull_throws() {
        AgentConfig config = literalConfig("openai", "${missingModel}", null);
        stubExpression("openai", "openai");
        stubExpression("${missingModel}", null);

        assertThrows(
                IllegalStateException.class,
                () -> AgentConfigExpressionResolver.resolve(
                        config, variableScope, expressionManager));
    }

    @Test
    void resolve_allowsNullSystemPrompt() {
        AgentConfig config = literalConfig("openai", "gpt-5.4", null);
        stubExpression("openai", "openai");
        stubExpression("gpt-5.4", "gpt-5.4");

        AgentConfig resolved =
                AgentConfigExpressionResolver.resolve(config, variableScope, expressionManager);

        assertSame(config, resolved);
        assertEquals(null, resolved.systemPrompt());
    }

    @Test
    void resolve_stringifiesNonStringExpressionResults() {
        AgentConfig config = literalConfig("${providerObj}", "${modelObj}", null);
        stubExpression("${providerObj}", 42);
        stubExpression("${modelObj}", true);

        AgentConfig resolved =
                AgentConfigExpressionResolver.resolve(config, variableScope, expressionManager);

        assertEquals("42", resolved.provider());
        assertEquals("true", resolved.model());
    }

    private AgentConfig literalConfig(String provider, String model, String systemPrompt) {
        return new AgentConfig(
                PROC_DEF_ID, ELEMENT_ID, provider, model, systemPrompt, ELEMENT_ID);
    }

    private void stubExpression(String expressionText, Object value) {
        Expression expression = mock(Expression.class);
        when(expressionManager.createExpression(expressionText)).thenReturn(expression);
        when(expression.getValue(variableScope)).thenReturn(value);
    }
}
