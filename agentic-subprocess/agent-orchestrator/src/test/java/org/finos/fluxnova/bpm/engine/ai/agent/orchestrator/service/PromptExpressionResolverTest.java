package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.delegate.VariableScope;
import org.finos.fluxnova.bpm.engine.impl.el.Expression;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptExpressionResolverTest {

    @Mock
    private ExpressionManager expressionManager;
    @Mock
    private Expression expression;
    @Mock
    private VariableScope scope;

    private PromptExpressionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PromptExpressionResolver(() -> expressionManager);
    }

    @Test
    void resolve_whenTemplateNull_returnsEmpty() {
        assertTrue(resolver.resolve(null, scope, "userPrompt").isEmpty());
        verifyNoInteractions(expressionManager);
    }

    @Test
    void resolve_whenTemplateEmpty_returnsEmpty() {
        assertTrue(resolver.resolve("", scope, "userPrompt").isEmpty());
        verifyNoInteractions(expressionManager);
    }

    @Test
    void resolve_whenTemplateBlank_returnsEmpty() {
        assertTrue(resolver.resolve("  \n\t ", scope, "userPrompt").isEmpty());
        verifyNoInteractions(expressionManager);
    }

    @Test
    void resolve_whenPlainText_returnsTextUnchangedWithoutEvaluation() {
        String text = "Line one\nLine two with <special> & \"quoted\" chars — 100% ✓";

        assertEquals(Optional.of(text), resolver.resolve(text, scope, "userPrompt"));
        verifyNoInteractions(expressionManager);
    }

    @Test
    void resolve_whenSingleExpression_returnsEvaluatedValue() {
        when(expressionManager.createExpression("${taskPrompt}")).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn("Review loan 42");

        assertEquals(Optional.of("Review loan 42"), resolver.resolve("${taskPrompt}", scope, "userPrompt"));
    }

    @Test
    void resolve_whenExpressionMixedWithText_returnsEvaluatedValue() {
        String template = "Investigate alert ${alertId} for ${customerName}";
        when(expressionManager.createExpression(template)).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn("Investigate alert A-101 for Acme");

        assertEquals(Optional.of("Investigate alert A-101 for Acme"),
                resolver.resolve(template, scope, "userPrompt"));
    }

    @Test
    void resolve_whenHashExpression_isEvaluated() {
        when(expressionManager.createExpression("#{name}")).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn("Acme");

        assertEquals(Optional.of("Acme"), resolver.resolve("#{name}", scope, "userPrompt"));
    }

    @Test
    void resolve_whenExpressionEvaluatesToNonString_returnsStringValue() {
        when(expressionManager.createExpression("${amount}")).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn(5000L);

        assertEquals(Optional.of("5000"), resolver.resolve("${amount}", scope, "userPrompt"));
    }

    @Test
    void resolve_whenExpressionEvaluatesToNull_returnsEmpty() {
        when(expressionManager.createExpression("${taskPrompt}")).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn(null);

        assertTrue(resolver.resolve("${taskPrompt}", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_whenExpressionEvaluatesToBlank_returnsEmpty() {
        when(expressionManager.createExpression("${taskPrompt}")).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn("   ");

        assertTrue(resolver.resolve("${taskPrompt}", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_whenVariableUndefined_returnsEmpty() {
        when(expressionManager.createExpression("Hello ${missing}")).thenReturn(expression);
        when(expression.getValue(scope)).thenThrow(
                new ProcessEngineException("Unknown property used in expression: Hello ${missing}"));

        assertTrue(resolver.resolve("Hello ${missing}", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_whenExpressionSyntaxInvalid_returnsEmpty() {
        when(expressionManager.createExpression("${unclosed")).thenThrow(
                new ProcessEngineException("Error parsing '${unclosed'"));

        assertTrue(resolver.resolve("${unclosed", scope, "userPrompt").isEmpty());
    }

    @Test
    void resolve_whenEvaluatedValueHasLineBreaksAndSpecialCharacters_preservesThem() {
        String evaluated = "Customer: O'Brien & Sons\n<Priority> \"high\"";
        when(expressionManager.createExpression("${prompt}")).thenReturn(expression);
        when(expression.getValue(scope)).thenReturn(evaluated);

        assertEquals(Optional.of(evaluated), resolver.resolve("${prompt}", scope, "userPrompt"));
    }
}
