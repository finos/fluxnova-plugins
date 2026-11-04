package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.finos.fluxnova.bpm.engine.ProcessEngineServices;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolType;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AgentTerminationHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for orchestration routing in {@link AgentOrchestrationJobHandler}.
 *
 * <p><strong>Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.6</strong></p>
 */
class AgentOrchestrationJobHandlerRoutingTest {

    private static final String PROC_DEF_ID = "procDef:1:abc";
    private static final String SCOPE_EXECUTION_ID = "scope-exec-001";
    private static final String ELEMENT_ID = "agentSubprocess";

    private AgentConfigRegistry agentConfigRegistry;
    private AgentToolCatalogueRegistry toolCatalogueRegistry;
    private AgentContextSpecRegistry contextSpecRegistry;
    private AgentContextResolver contextResolver;
    private LlmService llmService;
    private ToolInvocationService toolInvocationService;
    private AgentStateManager stateManager;
    private AgentTerminationHandler terminationHandler;
    private ExecutionEntity execution;
    private CommandContext commandContext;
    private JobManager jobManager;
    private RuntimeService runtimeService;
    private RepositoryService repositoryService;

    private AgentOrchestrationJobHandler handler;

    @BeforeTry
    void setUp() {
        agentConfigRegistry = mock(AgentConfigRegistry.class);
        toolCatalogueRegistry = mock(AgentToolCatalogueRegistry.class);
        contextSpecRegistry = mock(AgentContextSpecRegistry.class);
        contextResolver = mock(AgentContextResolver.class);
        llmService = mock(LlmService.class);
        toolInvocationService = mock(ToolInvocationService.class);
        stateManager = mock(AgentStateManager.class);
        terminationHandler = mock(AgentTerminationHandler.class);
        execution = mock(ExecutionEntity.class);
        commandContext = mock(CommandContext.class);
        jobManager = mock(JobManager.class);
        runtimeService = mock(RuntimeService.class);
        repositoryService = mock(RepositoryService.class);

        handler = new AgentOrchestrationJobHandler(
                agentConfigRegistry, toolCatalogueRegistry, contextSpecRegistry,
                contextResolver, llmService, toolInvocationService, stateManager,
                terminationHandler);

        // Common stubs
        when(commandContext.getJobManager()).thenReturn(jobManager);

        ProcessEngineServices services = mock(ProcessEngineServices.class);
        when(services.getRuntimeService()).thenReturn(runtimeService);
        when(services.getRepositoryService()).thenReturn(repositoryService);
        when(execution.getProcessEngineServices()).thenReturn(services);
        when(execution.isActive()).thenReturn(true);
        when(execution.getId()).thenReturn(SCOPE_EXECUTION_ID);
        when(execution.getProcessDefinitionId()).thenReturn(PROC_DEF_ID);
        when(execution.getActivityId()).thenReturn(ELEMENT_ID);
    }

    // ========================================================================
    // Property 19: Routing Correctness By Tool Type
    // ========================================================================

    /**
     * Property 19: Routing Correctness By Tool Type —
     * For any tool call whose toolId maps to a REMOTE_AGENT entry, dispatch creates
     * a MessageEntity with A2aRemoteInvokeJobHandler.TYPE; for BPMN_ACTIVITY entries,
     * it routes to toolInvocationService.invoke().
     *
     * <p><b>Validates: Requirements 10.1, 10.2, 10.3, 10.4</b></p>
     */
    @Property(tries = 100)
    void remoteAgentToolCallRoutesToA2aJobDispatch(
            @ForAll("toolIds") String remoteAgentId,
            @ForAll("toolCallIds") String toolCallId,
            @ForAll("prompts") String prompt,
            @ForAll("urls") String url) {

        // Arrange: a catalogue with a REMOTE_AGENT entry
        AgentToolEntry remoteEntry = new AgentToolEntry(
                remoteAgentId, "Remote Agent", "A remote agent",
                Set.of(), Set.of(), AgentToolType.REMOTE_AGENT);
        AgentToolCatalogue catalogue = new AgentToolCatalogue(
                PROC_DEF_ID, ELEMENT_ID, List.of(remoteEntry));
        AgentConfig agentConfig = new AgentConfig(
                PROC_DEF_ID, ELEMENT_ID, "openai", "gpt-4", "System prompt", ELEMENT_ID);
        AgentContextSpec contextSpec = new AgentContextSpec(PROC_DEF_ID, ELEMENT_ID, List.of());

        // Stub registries for the full execute() flow
        when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                .thenReturn(Optional.of(agentConfig));
        when(toolCatalogueRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                .thenReturn(Optional.of(catalogue));
        when(contextSpecRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                .thenReturn(Optional.of(contextSpec));
        when(contextResolver.resolve(runtimeService, SCOPE_EXECUTION_ID, contextSpec))
                .thenReturn(new ResolvedContext(Map.of()));
        when(stateManager.loadToolResultBuffer(runtimeService, SCOPE_EXECUTION_ID))
                .thenReturn(new ArrayList<>());
        when(stateManager.loadHistory(runtimeService, SCOPE_EXECUTION_ID))
                .thenReturn(new ArrayList<>());

        // LLM returns a tool call targeting the remote agent
        String arguments = "{\"prompt\":\"" + escapeJson(prompt) + "\"}";
        List<ToolCallRequest> toolCalls = List.of(
                new ToolCallRequest(toolCallId, remoteAgentId, arguments));
        LlmResponse response = new LlmResponse("Delegating", toolCalls,
                List.of(ConversationEntry.assistant("Delegating", toolCalls)));
        when(llmService.call(eq(agentConfig), eq(catalogue), any(), anyList()))
                .thenReturn(response);

        // Stub remote agent config: activity has A2aRemoteCallConfig property
        ProcessDefinitionEntity procDefEntity = mock(ProcessDefinitionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDefEntity);
        when(procDefEntity.findActivity(remoteAgentId)).thenReturn(activity);
        A2aRemoteCallConfig a2aConfig = new A2aRemoteCallConfig(url, null, "Agent", "desc", "agent-ref");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(a2aConfig);

        // Act
        handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

        // Assert: a MessageEntity with A2aRemoteInvokeJobHandler.TYPE was created
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        MessageEntity createdJob = captor.getValue();
        assertEquals(A2aRemoteInvokeJobHandler.TYPE, createdJob.getJobHandlerType(),
                "Remote agent tool calls must be dispatched via A2A job handler type");

        // Verify toolInvocationService was NOT called for this remote agent
        verify(toolInvocationService, never()).invoke(any(), any(), any(), any());
    }

    /**
     * Property 19 (BPMN_ACTIVITY path): For any tool call whose toolId maps to a
     * BPMN_ACTIVITY entry, dispatch routes to toolInvocationService.invoke().
     *
     * <p><b>Validates: Requirements 10.2, 10.3</b></p>
     */
    @Property(tries = 100)
    void bpmnActivityToolCallRoutesToToolInvocationService(
            @ForAll("toolIds") String bpmnActivityId,
            @ForAll("toolCallIds") String toolCallId) {

        // Arrange: a catalogue with a BPMN_ACTIVITY entry
        AgentToolEntry bpmnEntry = new AgentToolEntry(
                bpmnActivityId, "BPMN Task", "A local BPMN activity",
                Set.of(), Set.of("resultVar"), AgentToolType.BPMN_ACTIVITY);
        AgentToolCatalogue catalogue = new AgentToolCatalogue(
                PROC_DEF_ID, ELEMENT_ID, List.of(bpmnEntry));
        AgentConfig agentConfig = new AgentConfig(
                PROC_DEF_ID, ELEMENT_ID, "openai", "gpt-4", "System prompt", ELEMENT_ID);
        AgentContextSpec contextSpec = new AgentContextSpec(PROC_DEF_ID, ELEMENT_ID, List.of());

        // Stub registries
        when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                .thenReturn(Optional.of(agentConfig));
        when(toolCatalogueRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                .thenReturn(Optional.of(catalogue));
        when(contextSpecRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                .thenReturn(Optional.of(contextSpec));
        when(contextResolver.resolve(runtimeService, SCOPE_EXECUTION_ID, contextSpec))
                .thenReturn(new ResolvedContext(Map.of()));
        when(stateManager.loadToolResultBuffer(runtimeService, SCOPE_EXECUTION_ID))
                .thenReturn(new ArrayList<>());
        when(stateManager.loadHistory(runtimeService, SCOPE_EXECUTION_ID))
                .thenReturn(new ArrayList<>());

        // LLM returns a tool call targeting the BPMN activity
        List<ToolCallRequest> toolCalls = List.of(
                new ToolCallRequest(toolCallId, bpmnActivityId));
        LlmResponse response = new LlmResponse("Invoking task", toolCalls,
                List.of(ConversationEntry.assistant("Invoking task", toolCalls)));
        when(llmService.call(eq(agentConfig), eq(catalogue), any(), anyList()))
                .thenReturn(response);
        when(toolInvocationService.invoke(eq(runtimeService), eq(SCOPE_EXECUTION_ID),
                eq(catalogue), any(ToolCallRequest.class)))
                .thenReturn(ToolInvocationResult.success(toolCallId));

        // Act
        handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

        // Assert: toolInvocationService.invoke() was called
        verify(toolInvocationService).invoke(eq(runtimeService), eq(SCOPE_EXECUTION_ID),
                eq(catalogue), any(ToolCallRequest.class));

        // Verify NO A2A job was created (jobManager should not be called for successful BPMN tools)
        verify(jobManager, never()).insertAndHintJobExecutor(any(MessageEntity.class));
    }

    // ========================================================================
    // Property 20: Prompt Extraction from Arguments
    // ========================================================================

    /**
     * Property 20: Prompt Extraction from Arguments —
     * For any valid JSON arguments containing a "prompt" field,
     * extractPromptFromArguments correctly extracts the value.
     *
     * <p><b>Validates: Requirements 10.6</b></p>
     */
    @Property(tries = 200)
    void extractPromptFromArguments_validJson_extractsPrompt(
            @ForAll("prompts") String prompt) {

        // Arrange: construct valid JSON with a prompt field
        String arguments = "{\"prompt\":\"" + escapeJson(prompt) + "\"}";

        // Act
        String result = handler.extractPromptFromArguments(arguments);

        // Assert
        assertEquals(prompt, result,
                "extractPromptFromArguments must extract the prompt value from valid JSON");
    }

    /**
     * Property 20 (invalid path): For invalid/missing prompt,
     * extractPromptFromArguments returns null.
     *
     * <p><b>Validates: Requirements 10.6</b></p>
     */
    @Property(tries = 100)
    void extractPromptFromArguments_invalidInput_returnsNull(
            @ForAll("invalidArguments") String invalidArgs) {

        // Act
        String result = handler.extractPromptFromArguments(invalidArgs);

        // Assert
        assertNull(result,
                "extractPromptFromArguments must return null for invalid/missing prompt input");
    }

    /**
     * Property 20 (null/blank path): For null or blank input,
     * extractPromptFromArguments returns null.
     */
    @Property(tries = 50)
    void extractPromptFromArguments_nullOrBlank_returnsNull(
            @ForAll("blankInputs") String blankInput) {

        // Act
        String result = handler.extractPromptFromArguments(blankInput);

        // Assert
        assertNull(result,
                "extractPromptFromArguments must return null for null or blank input");
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> toolIds() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> toolCallIds() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(5).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> prompts() {
        // Generate prompts that are safe for JSON embedding (no unescaped quotes/backslashes)
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?', '-', '_')
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> urls() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                .map(s -> "http://" + s + ":8080");
    }

    @Provide
    Arbitrary<String> invalidArguments() {
        return Arbitraries.oneOf(
                // Missing prompt field
                Arbitraries.of("{\"task\":\"do something\"}"),
                Arbitraries.of("{\"message\":\"hello\"}"),
                Arbitraries.of("{}"),
                // Prompt field is not a string
                Arbitraries.of("{\"prompt\":123}"),
                Arbitraries.of("{\"prompt\":true}"),
                Arbitraries.of("{\"prompt\":null}"),
                Arbitraries.of("{\"prompt\":[\"array\"]}"),
                Arbitraries.of("{\"prompt\":{\"nested\":\"object\"}}"),
                // Malformed JSON
                Arbitraries.of("{not valid json"),
                Arbitraries.of("just a string"),
                Arbitraries.of(""),
                Arbitraries.of("[1,2,3]")
        );
    }

    @Provide
    Arbitrary<String> blankInputs() {
        return Arbitraries.oneOf(
                Arbitraries.of(""),
                Arbitraries.of("   "),
                Arbitraries.of("\t"),
                Arbitraries.of("\n"),
                Arbitraries.of("  \t\n  ")
        );
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Escapes special JSON characters in a string for safe embedding in a JSON value.
     */
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
