package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link A2aRemoteInvokeJobHandler}.
 *
 * <p><strong>Validates: Requirements 9.3, 9.4, 9.5, 11.2, 11.4, 11.5</strong></p>
 */
class A2aRemoteInvokeJobHandlerTest {

    private A2aInvocationService invocationService;
    private RuntimeService runtimeService;
    private ExecutionEntity execution;
    private CommandContext commandContext;
    private JobManager jobManager;
    private A2aRemoteInvokeJobHandler handler;

    @BeforeTry
    void setUp() {
        invocationService = mock(A2aInvocationService.class);
        runtimeService = mock(RuntimeService.class);
        execution = mock(ExecutionEntity.class);
        commandContext = mock(CommandContext.class);
        jobManager = mock(JobManager.class);

        when(commandContext.getJobManager()).thenReturn(jobManager);

        handler = new A2aRemoteInvokeJobHandler(invocationService, runtimeService);
    }

    // ========================================================================
    // Property 15: Result Variable Naming and Storage Integrity
    //              (agentic subprocess part)
    // ========================================================================

    /**
     * Property 15: Result Variable Naming and Storage Integrity —
     * For any remoteAgentId, a successful invocation stores the response text
     * as a variable named "{remoteAgentId}Result" via runtimeService.setVariable
     * on the process instance execution (not the subprocess scope).
     *
     * <p><b>Validates: Requirements 9.3, 11.2</b></p>
     */
    @Property(tries = 100)
    void successfulInvocationStoresVariableWithCorrectName(
            @ForAll("remoteAgentIds") String remoteAgentId,
            @ForAll("responseTexts") String responseText,
            @ForAll("urls") String url,
            @ForAll("prompts") String prompt,
            @ForAll("toolCallIds") String toolCallId,
            @ForAll("scopeExecutionIds") String scopeExecutionId) {

        // Arrange
        String processInstanceId = "proc-inst-" + scopeExecutionId;
        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                url, prompt, toolCallId, scopeExecutionId, remoteAgentId);

        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(invocationService.invoke(eq(url), eq(prompt), any()))
                .thenReturn(A2aInvocationResult.success(responseText));

        // Act
        handler.execute(config, execution, commandContext, null);

        // Assert: variable stored on process instance with correct name pattern and value
        String expectedVarName = remoteAgentId + "Result";
        verify(runtimeService).setVariable(processInstanceId, expectedVarName, responseText);
    }

    // ========================================================================
    // Property 17: Result Variable Overwrite on Repeated Invocation
    // ========================================================================

    /**
     * Property 17: Result Variable Overwrite on Repeated Invocation —
     * For any remoteAgentId invoked multiple times, the most recent successful
     * response overwrites the previous result variable via setVariable
     * with the same variable name on the process instance.
     *
     * <p><b>Validates: Requirements 11.4</b></p>
     */
    @Property(tries = 100)
    void repeatedInvocationOverwritesResultVariable(
            @ForAll("remoteAgentIds") String remoteAgentId,
            @ForAll("responseTexts") String firstResponse,
            @ForAll("responseTexts") String secondResponse,
            @ForAll("urls") String url,
            @ForAll("prompts") String prompt,
            @ForAll("toolCallIds") String toolCallId1,
            @ForAll("toolCallIds") String toolCallId2,
            @ForAll("scopeExecutionIds") String scopeExecutionId) {

        // Arrange: two configs with the same remoteAgentId but potentially different responses
        String processInstanceId = "proc-inst-" + scopeExecutionId;
        A2aRemoteInvokeConfig config1 = new A2aRemoteInvokeConfig(
                url, prompt, toolCallId1, scopeExecutionId, remoteAgentId);
        A2aRemoteInvokeConfig config2 = new A2aRemoteInvokeConfig(
                url, prompt, toolCallId2, scopeExecutionId, remoteAgentId);

        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(invocationService.invoke(eq(url), eq(prompt), any()))
                .thenReturn(A2aInvocationResult.success(firstResponse))
                .thenReturn(A2aInvocationResult.success(secondResponse));

        // Act: execute handler twice
        handler.execute(config1, execution, commandContext, null);
        handler.execute(config2, execution, commandContext, null);

        // Assert: setVariable called twice with the same variable name but different values
        String expectedVarName = remoteAgentId + "Result";
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(runtimeService, times(2)).setVariable(
                eq(processInstanceId), eq(expectedVarName), valueCaptor.capture());

        // The two calls used the same variable name (overwrite semantics)
        assertEquals(firstResponse, valueCaptor.getAllValues().get(0),
                "First invocation should store first response");
        assertEquals(secondResponse, valueCaptor.getAllValues().get(1),
                "Second invocation should overwrite with second response");
    }

    // ========================================================================
    // Property 18: ToolCallId Correlation Preservation
    // ========================================================================

    /**
     * Property 18: ToolCallId Correlation Preservation —
     * For any toolCallId, the completion signal ToolResult uses the exact same
     * toolCallId for correlation on success.
     *
     * <p><b>Validates: Requirements 9.5, 11.5</b></p>
     */
    @Property(tries = 100)
    void completionSignalPreservesToolCallIdOnSuccess(
            @ForAll("toolCallIds") String toolCallId,
            @ForAll("remoteAgentIds") String remoteAgentId,
            @ForAll("responseTexts") String responseText,
            @ForAll("urls") String url,
            @ForAll("prompts") String prompt,
            @ForAll("scopeExecutionIds") String scopeExecutionId) {

        // Arrange
        String processInstanceId = "proc-inst-" + scopeExecutionId;
        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                url, prompt, toolCallId, scopeExecutionId, remoteAgentId);

        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(invocationService.invoke(eq(url), eq(prompt), any()))
                .thenReturn(A2aInvocationResult.success(responseText));

        // Act
        handler.execute(config, execution, commandContext, null);

        // Assert: capture the MessageEntity and verify toolCallId correlation
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        MessageEntity createdJob = captor.getValue();
        assertNotNull(createdJob.getJobHandlerConfigurationRaw(),
                "Completion signal job must have configuration");

        AgentOrchestrationConfig orchConfig = AgentOrchestrationConfig.fromCanonicalString(
                createdJob.getJobHandlerConfigurationRaw());
        assertTrue(orchConfig.hasToolResult(), "Completion signal must carry a ToolResult");

        ToolResult toolResult = orchConfig.toolResult();
        assertEquals(toolCallId, toolResult.toolCallId(),
                "ToolResult toolCallId must match the original toolCallId");
    }

    /**
     * Property 18 (failure path): ToolCallId Correlation Preservation —
     * On failure, the completion signal ToolResult also uses the exact same
     * toolCallId for correlation.
     *
     * <p><b>Validates: Requirements 9.5, 11.5</b></p>
     */
    @Property(tries = 100)
    void completionSignalPreservesToolCallIdOnFailure(
            @ForAll("toolCallIds") String toolCallId,
            @ForAll("remoteAgentIds") String remoteAgentId,
            @ForAll("urls") String url,
            @ForAll("prompts") String prompt,
            @ForAll("scopeExecutionIds") String scopeExecutionId) {

        // Arrange
        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                url, prompt, toolCallId, scopeExecutionId, remoteAgentId);

        when(invocationService.invoke(eq(url), eq(prompt), any()))
                .thenReturn(A2aInvocationResult.failure("Connection timed out"));

        // Act
        handler.execute(config, execution, commandContext, null);

        // Assert: capture the MessageEntity and verify toolCallId correlation
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        MessageEntity createdJob = captor.getValue();
        assertNotNull(createdJob.getJobHandlerConfigurationRaw(),
                "Completion signal job must have configuration");

        AgentOrchestrationConfig orchConfig = AgentOrchestrationConfig.fromCanonicalString(
                createdJob.getJobHandlerConfigurationRaw());
        assertTrue(orchConfig.hasToolResult(), "Completion signal must carry a ToolResult");

        ToolResult toolResult = orchConfig.toolResult();
        assertEquals(toolCallId, toolResult.toolCallId(),
                "ToolResult toolCallId must match the original toolCallId on failure");
        assertTrue(toolResult.isError(), "ToolResult must be an error on failure");
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> remoteAgentIds() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> responseTexts() {
        return Arbitraries.strings().ofMinLength(0).ofMaxLength(200);
    }

    @Provide
    Arbitrary<String> urls() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                .map(s -> "http://" + s + ":8080");
    }

    @Provide
    Arbitrary<String> prompts() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> toolCallIds() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(5).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> scopeExecutionIds() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(5).ofMaxLength(30);
    }
}
