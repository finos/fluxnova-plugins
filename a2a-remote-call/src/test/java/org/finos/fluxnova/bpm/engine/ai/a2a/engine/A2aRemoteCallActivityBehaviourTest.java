package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import net.jqwik.api.*;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.delegate.BpmnError;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.el.Expression;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link A2aRemoteCallActivityBehaviour} standalone execution.
 *
 * <p>Validates: Requirements 6.1, 6.2, 6.4, 6.6, 11.1, 13.4</p>
 */
class A2aRemoteCallActivityBehaviourTest {

    // ========================================================================
    // Property 14: EL Expression Resolution in Standalone Mode
    // ========================================================================

    /**
     * Property 14: EL Expression Resolution in Standalone Mode
     *
     * <p>For any url and prompt EL expressions, the behaviour resolves them correctly
     * via the expression manager and invokes the service with the resolved values.</p>
     *
     * <p><b>Validates: Requirements 6.1, 6.2</b></p>
     */
    @Property(tries = 100)
    void elExpressionResolutionInStandaloneMode(
            @ForAll("urlExpressions") String urlExpression,
            @ForAll("promptExpressions") String promptExpression,
            @ForAll("resolvedValues") String resolvedUrl,
            @ForAll("resolvedValues") String resolvedPrompt) {

        // Ensure url and prompt expressions are always different
        Assume.that(!urlExpression.equals(promptExpression));

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallActivityBehaviour behaviour = new A2aRemoteCallActivityBehaviour(invocationService);

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig(urlExpression, promptExpression, "test", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY)).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression(urlExpression)).thenReturn(urlExpr);
        when(expressionManager.createExpression(promptExpression)).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(resolvedUrl);
        when(promptExpr.getValue(execution)).thenReturn(resolvedPrompt);
        when(invocationService.invoke(eq(resolvedUrl), eq(resolvedPrompt), any()))
                .thenReturn(A2aInvocationResult.success("response"));

        // Execute with mocked static Context
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            assertDoesNotThrow(() -> behaviour.execute(execution));
        }

        // Verify: expression manager was called with the raw EL expressions
        verify(expressionManager).createExpression(urlExpression);
        verify(expressionManager).createExpression(promptExpression);

        // Verify: expressions were evaluated against the execution
        verify(urlExpr).getValue(execution);
        verify(promptExpr).getValue(execution);

        // Verify: invocation service was called with the resolved values (not the raw EL expressions)
        verify(invocationService).invoke(eq(resolvedUrl), eq(resolvedPrompt), any());
    }

    // ========================================================================
    // Property 15: Result Variable Naming and Storage Integrity (standalone part)
    // ========================================================================

    /**
     * Property 15: Result Variable Naming and Storage Integrity (standalone part)
     *
     * <p>For any activity id, the result is stored as {@code {activityId}Result}.</p>
     *
     * <p><b>Validates: Requirements 6.4, 11.1</b></p>
     */
    @Property(tries = 100)
    void resultVariableNamingAndStorageIntegrity(
            @ForAll("activityIds") String activityId,
            @ForAll("responseTexts") String responseText) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallActivityBehaviour behaviour = new A2aRemoteCallActivityBehaviour(invocationService);

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "test", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn(activityId);
        when(activity.getProperty(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY)).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn("http://example.com");
        when(promptExpr.getValue(execution)).thenReturn("do something");
        when(invocationService.invoke(eq("http://example.com"), eq("do something"), any()))
                .thenReturn(A2aInvocationResult.success(responseText));

        // Execute with mocked static Context
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            assertDoesNotThrow(() -> behaviour.execute(execution));
        }

        // Verify: result stored with correct variable name pattern: {activityId}Result
        String expectedVariableName = activityId + "Result";
        verify(execution).setVariable(expectedVariableName, responseText);
    }

    // ========================================================================
    // Property 16: Standalone BpmnError on Failure
    // ========================================================================

    /**
     * Property 16: Standalone BpmnError on Failure
     *
     * <p>For any failure result, the behaviour throws BpmnError with error code
     * "a2a-remote-call-error".</p>
     *
     * <p><b>Validates: Requirements 6.6, 13.4</b></p>
     */
    @Property(tries = 100)
    void standaloneBpmnErrorOnFailure(
            @ForAll("nonBlankErrorMessages") String errorMessage) {

        // Setup
        A2aInvocationService invocationService = mock(A2aInvocationService.class);
        A2aRemoteCallActivityBehaviour behaviour = new A2aRemoteCallActivityBehaviour(invocationService);

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "test", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty(A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY)).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn("http://example.com");
        when(promptExpr.getValue(execution)).thenReturn("do something");
        when(invocationService.invoke(eq("http://example.com"), eq("do something"), any()))
                .thenReturn(A2aInvocationResult.failure(errorMessage));

        // Execute with mocked static Context and verify BpmnError is thrown
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            // Verify: error code is "a2a-remote-call-error"
            assertEquals("a2a-remote-call-error", thrown.getErrorCode(),
                    "BpmnError error code must be 'a2a-remote-call-error'");

            // Verify: error message matches the failure message
            assertEquals(errorMessage, thrown.getMessage(),
                    "BpmnError message must match the failure error message");
        }

        // Verify: no variable was stored on failure
        verify(execution, never()).setVariable(any(), any());
    }

    // ========================================================================
    // Generators / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> urlExpressions() {
        return Arbitraries.oneOf(
                // Simple EL expressions with "url" prefix to ensure distinctness
                Arbitraries.strings().ofMinLength(1).ofMaxLength(30)
                        .alpha().numeric().withChars('_')
                        .map(s -> "${url_" + s + "}"),
                // Literal URL values
                Arbitraries.strings().ofMinLength(3).ofMaxLength(50)
                        .withCharRange('a', 'z').withCharRange('0', '9')
                        .withChars(':', '/', '.', '-')
                        .map(s -> "http://" + s)
        );
    }

    @Provide
    Arbitrary<String> promptExpressions() {
        return Arbitraries.oneOf(
                // Simple EL expressions with "prompt" prefix to ensure distinctness
                Arbitraries.strings().ofMinLength(1).ofMaxLength(30)
                        .alpha().numeric().withChars('_')
                        .map(s -> "${prompt_" + s + "}"),
                // Literal prompt values
                Arbitraries.strings().ofMinLength(3).ofMaxLength(100)
                        .withCharRange('a', 'z').withCharRange('A', 'Z')
                        .withCharRange('0', '9').withChars(' ', '.', ',', '-')
                        .map(s -> "Prompt: " + s)
        );
    }

    @Provide
    Arbitrary<String> resolvedValues() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200)
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(':', '/', '.', '-', '_', ' ');
    }

    @Provide
    Arbitrary<String> activityIds() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(50)
                .alpha()
                .numeric()
                .withChars('-', '_');
    }

    @Provide
    Arbitrary<String> responseTexts() {
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(500)
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?', '\n', '-', '_');
    }

    @Provide
    Arbitrary<String> nonBlankErrorMessages() {
        // BpmnError requires non-empty, non-blank messages
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(300)
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', ':', '-', '_', '(', ')')
                .filter(s -> !s.isBlank());
    }
}
