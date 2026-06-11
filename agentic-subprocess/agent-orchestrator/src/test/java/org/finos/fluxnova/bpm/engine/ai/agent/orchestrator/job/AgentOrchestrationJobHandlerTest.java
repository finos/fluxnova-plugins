package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import org.finos.fluxnova.bpm.engine.ProcessEngineServices;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AgentTerminationHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestrationJobHandlerTest {

        private static final String PROC_DEF_ID = "procDef:1:abc";
        private static final String SCOPE_EXECUTION_ID = "scope-exec-001";
        private static final String ELEMENT_ID = "agentSubprocess";

        @Mock
        private RepositoryService repositoryService;
        @Mock
        private RuntimeService runtimeService;
        @Mock
        private AgentConfigRegistry agentConfigRegistry;
        @Mock
        private AgentToolCatalogueRegistry toolCatalogueRegistry;
        @Mock
        private AgentContextSpecRegistry contextSpecRegistry;
        @Mock
        private AgentContextResolver contextResolver;
        @Mock
        private LlmService llmService;
        @Mock
        private ToolInvocationService toolInvocationService;
        @Mock
        private AgentStateManager stateManager;
        @Mock
        private AgentTerminationHandler terminationHandler;
        @Mock
        private ExecutionEntity execution;
        @Mock
        private CommandContext commandContext;
        @Mock
        private JobManager jobManager;

        private AgentOrchestrationJobHandler handler;

        private AgentConfig agentConfig;
        private AgentToolCatalogue toolCatalogue;
        private AgentContextSpec contextSpec;

        @BeforeEach
        void setUp() {
                handler = new AgentOrchestrationJobHandler(agentConfigRegistry,
                                toolCatalogueRegistry, contextSpecRegistry, contextResolver,
                                llmService, toolInvocationService, stateManager,
                                terminationHandler);

                agentConfig = new AgentConfig(PROC_DEF_ID, ELEMENT_ID, "ollama", "llama3",
                                "You are an agent.", ELEMENT_ID);
                toolCatalogue = new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID,
                                List.of(new AgentToolEntry("taskA", "Task A", "Does A", Set.of(),
                                                Set.of("resultA"))));
                contextSpec = new AgentContextSpec(PROC_DEF_ID, ELEMENT_ID, List.of());
        }

        private void stubActiveServices() {
                ProcessEngineServices services = mock(ProcessEngineServices.class);
                when(services.getRepositoryService()).thenReturn(repositoryService);
                when(services.getRuntimeService()).thenReturn(runtimeService);
                when(execution.getProcessEngineServices()).thenReturn(services);
        }

        private void stubActiveExecution() {
                when(execution.isActive()).thenReturn(true);
                when(execution.getId()).thenReturn(SCOPE_EXECUTION_ID);
                when(execution.getProcessDefinitionId()).thenReturn(PROC_DEF_ID);
                when(execution.getActivityId()).thenReturn(ELEMENT_ID);
        }

        private void stubRegistries() {
                when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                .thenReturn(Optional.of(agentConfig));
                when(toolCatalogueRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                .thenReturn(Optional.of(toolCatalogue));
                when(contextSpecRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                .thenReturn(Optional.of(contextSpec));
        }

        private void stubEmptyState() {
                when(stateManager.loadToolResultBuffer(runtimeService, SCOPE_EXECUTION_ID))
                                .thenReturn(new ArrayList<>());
                when(stateManager.loadHistory(runtimeService, SCOPE_EXECUTION_ID)).thenReturn(new ArrayList<>());
        }

        @Test
        void getType_returnsCorrectType() {
                assertEquals("agent-orchestration-step", handler.getType());
        }

        @Nested
        class InactiveScope {

                @Test
                void execute_whenScopeInactive_exitsImmediately() {
                        stubActiveServices();
                        when(execution.isActive()).thenReturn(false);

                        handler.execute(AgentOrchestrationConfig.forEntry(), execution,
                                        commandContext, null);

                        verifyNoInteractions(agentConfigRegistry);
                        verifyNoInteractions(llmService);
                }
        }

        @Nested
        class EntryPath {

                @BeforeEach
                void setUpActiveExecution() {
                        stubActiveExecution();
                        stubActiveServices();
                }

                @Test
                void execute_callsLlmAndDispatchesTools() {
                        stubRegistries();
                        stubEmptyState();
                        ResolvedContext resolvedContext =
                                        new ResolvedContext(Map.of("customerId", "C123"));
                        when(contextResolver.resolve(runtimeService, SCOPE_EXECUTION_ID, contextSpec))
                                        .thenReturn(resolvedContext);

                        List<ToolCallRequest> toolCalls =
                                        List.of(new ToolCallRequest("tc1", "taskA"));
                        List<ConversationEntry> updatedHistory = List.of(ConversationEntry
                                        .assistant("I'll check task A", toolCalls));
                        LlmResponse response = new LlmResponse("I'll check task A", toolCalls,
                                        updatedHistory);
                        when(llmService.call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList())).thenReturn(response);
                        when(toolInvocationService.invoke(eq(runtimeService), eq(SCOPE_EXECUTION_ID), eq(toolCatalogue),
                                        any())).thenReturn(ToolInvocationResult.success("tc1"));

                        handler.execute(AgentOrchestrationConfig.forEntry(), execution,
                                        commandContext, null);

                        verify(llmService).call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList());
                        verify(toolInvocationService).invoke(runtimeService, SCOPE_EXECUTION_ID, toolCatalogue,
                                        toolCalls.get(0));
                        verify(stateManager).saveHistory(runtimeService, SCOPE_EXECUTION_ID, updatedHistory);
                        verify(stateManager).savePendingToolCalls(eq(runtimeService), eq(SCOPE_EXECUTION_ID),
                                        eq(Set.of("tc1")));
                }

                @Test
                void execute_whenLlmReturnsNoToolCalls_completesScope() {
                        stubRegistries();
                        stubEmptyState();
                        ResolvedContext resolvedContext = new ResolvedContext(Map.of());
                        when(contextResolver.resolve(runtimeService, SCOPE_EXECUTION_ID, contextSpec))
                                        .thenReturn(resolvedContext);

                        List<ConversationEntry> updatedHistory = List
                                        .of(ConversationEntry.assistant("All done!", List.of()));
                        LlmResponse response =
                                        new LlmResponse("All done!", List.of(), updatedHistory);
                        when(llmService.call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList())).thenReturn(response);

                        handler.execute(AgentOrchestrationConfig.forEntry(), execution,
                                        commandContext, null);

                        verify(terminationHandler).complete(runtimeService, SCOPE_EXECUTION_ID);
                        verifyNoInteractions(toolInvocationService);
                }

                @Test
                void execute_whenToolCatalogueEmpty_completesScope() {
                        AgentToolCatalogue emptyCatalogue =
                                        new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID, List.of());
                        when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.of(agentConfig));
                        when(toolCatalogueRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.of(emptyCatalogue));
                        stubEmptyState();

                        handler.execute(AgentOrchestrationConfig.forEntry(), execution,
                                        commandContext, null);

                        verify(terminationHandler).complete(runtimeService, SCOPE_EXECUTION_ID);
                        verifyNoInteractions(llmService);
                }

                @Test
                void execute_whenNoContextSpec_usesEmptyFallback() {
                        when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.of(agentConfig));
                        when(toolCatalogueRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.of(toolCatalogue));
                        when(contextSpecRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.empty());
                        stubEmptyState();

                        ResolvedContext resolvedContext = new ResolvedContext(Map.of());
                        when(contextResolver.resolve(eq(runtimeService), eq(SCOPE_EXECUTION_ID),
                                        any(AgentContextSpec.class))).thenReturn(resolvedContext);

                        LlmResponse response = new LlmResponse("Done", List.of(),
                                        List.of(ConversationEntry.assistant("Done", List.of())));
                        when(llmService.call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList())).thenReturn(response);

                        handler.execute(AgentOrchestrationConfig.forEntry(), execution,
                                        commandContext, null);

                        verify(contextResolver).resolve(eq(runtimeService), eq(SCOPE_EXECUTION_ID),
                                        any(AgentContextSpec.class));
                        verify(terminationHandler).complete(runtimeService, SCOPE_EXECUTION_ID);
                }

                @Test
                void execute_whenAgentConfigMissing_throwsIllegalState() {
                        when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.empty());
                        stubEmptyState();

                        assertThrows(IllegalStateException.class,
                                        () -> handler.execute(AgentOrchestrationConfig.forEntry(),
                                                        execution, commandContext, null));
                }

                @Test
                void execute_whenToolCatalogueMissing_throwsIllegalState() {
                        when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.of(agentConfig));
                        when(toolCatalogueRegistry.resolve(repositoryService, PROC_DEF_ID, ELEMENT_ID))
                                        .thenReturn(Optional.empty());
                        stubEmptyState();

                        assertThrows(IllegalStateException.class,
                                        () -> handler.execute(AgentOrchestrationConfig.forEntry(),
                                                        execution, commandContext, null));
                }
        }

        @Nested
        class ToolCompletionPath {

                @BeforeEach
                void setUpActiveExecution() {
                        when(execution.isActive()).thenReturn(true);
                        when(execution.getId()).thenReturn(SCOPE_EXECUTION_ID);
                        stubActiveServices();
                }

                @Test
                void execute_absorbsResultAndWaitsForMore() {
                        ToolResult toolResult = new ToolResult("tc1", "taskA", null);
                        when(stateManager.isPendingToolCall(runtimeService, SCOPE_EXECUTION_ID, "tc1"))
                                        .thenReturn(true);
                        when(stateManager.completeToolCall(runtimeService, SCOPE_EXECUTION_ID, "tc1"))
                                        .thenReturn(false);

                        handler.execute(AgentOrchestrationConfig.forToolCompletion(toolResult),
                                        execution, commandContext, null);

                        verify(stateManager).appendToResultBuffer(runtimeService, SCOPE_EXECUTION_ID, toolResult);
                        verify(stateManager).completeToolCall(runtimeService, SCOPE_EXECUTION_ID, "tc1");
                        verifyNoInteractions(llmService);
                }

                @Test
                void execute_allDone_callsLlm() {
                        when(execution.getProcessDefinitionId()).thenReturn(PROC_DEF_ID);
                        when(execution.getActivityId()).thenReturn(ELEMENT_ID);
                        stubRegistries();
                        ToolResult toolResult = new ToolResult("tc1", "taskA", null);
                        when(stateManager.isPendingToolCall(runtimeService, SCOPE_EXECUTION_ID, "tc1"))
                                        .thenReturn(true);
                        when(stateManager.completeToolCall(runtimeService, SCOPE_EXECUTION_ID, "tc1"))
                                        .thenReturn(true);

                        ResolvedContext resolvedContext =
                                        new ResolvedContext(Map.of("resultA", "value"));
                        when(contextResolver.resolve(runtimeService, SCOPE_EXECUTION_ID, contextSpec))
                                        .thenReturn(resolvedContext);

                        when(stateManager.loadToolResultBuffer(runtimeService, SCOPE_EXECUTION_ID))
                                        .thenReturn(new ArrayList<>(List.of(toolResult)));

                        List<ConversationEntry> existingHistory = new ArrayList<>(
                                        List.of(ConversationEntry.assistant("Checking", List
                                                        .of(new ToolCallRequest("tc1", "taskA")))));
                        when(stateManager.loadHistory(runtimeService, SCOPE_EXECUTION_ID))
                                        .thenReturn(existingHistory);

                        LlmResponse response = new LlmResponse("Done!", List.of(),
                                        List.of(ConversationEntry.assistant("Done!", List.of())));
                        when(llmService.call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList())).thenReturn(response);

                        handler.execute(AgentOrchestrationConfig.forToolCompletion(toolResult),
                                        execution, commandContext, null);

                        verify(stateManager).appendToResultBuffer(runtimeService, SCOPE_EXECUTION_ID, toolResult);
                        verify(llmService).call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList());
                        verify(terminationHandler).complete(runtimeService, SCOPE_EXECUTION_ID);
                }

                @Test
                void execute_duplicateToolCallId_discarded() {
                        ToolResult toolResult = new ToolResult("tc-unknown", "taskA", null);
                        when(stateManager.isPendingToolCall(runtimeService, SCOPE_EXECUTION_ID, "tc-unknown"))
                                        .thenReturn(false);

                        handler.execute(AgentOrchestrationConfig.forToolCompletion(toolResult),
                                        execution, commandContext, null);

                        verify(stateManager, never()).appendToResultBuffer(any(), any(), any());
                        verifyNoInteractions(llmService);
                }
        }

        @Nested
        class DispatchFailures {

                @BeforeEach
                void setUpActiveExecution() {
                        stubActiveExecution();
                        stubActiveServices();
                }

                @Test
                void execute_allToolsFail_synthesisesCompletionJobPerFailure() {
                        stubRegistries();
                        stubEmptyState();
                        ResolvedContext resolvedContext = new ResolvedContext(Map.of());
                        when(contextResolver.resolve(runtimeService, SCOPE_EXECUTION_ID, contextSpec))
                                        .thenReturn(resolvedContext);

                        List<ToolCallRequest> toolCalls =
                                        List.of(new ToolCallRequest("tc1", "taskA"),
                                                        new ToolCallRequest("tc2", "unknownTask"));
                        LlmResponse response = new LlmResponse("Checking", toolCalls, List
                                        .of(ConversationEntry.assistant("Checking", toolCalls)));
                        when(llmService.call(eq(agentConfig), eq(toolCatalogue),
                                        eq(resolvedContext), anyList())).thenReturn(response);
                        when(toolInvocationService
                                        .invoke(eq(runtimeService), eq(SCOPE_EXECUTION_ID), eq(toolCatalogue), any()))
                                                        .thenReturn(ToolInvocationResult
                                                                        .failure("tc1", "Failed"))
                                                        .thenReturn(ToolInvocationResult.failure(
                                                                        "tc2", "Unknown tool"));
                        when(commandContext.getJobManager()).thenReturn(jobManager);

                        handler.execute(AgentOrchestrationConfig.forEntry(), execution,
                                        commandContext, null);

                        verify(stateManager).savePendingToolCalls(eq(runtimeService), eq(SCOPE_EXECUTION_ID),
                                        eq(Set.of("tc1", "tc2")));
                        verify(jobManager, times(2))
                                        .insertAndHintJobExecutor(any(MessageEntity.class));
                        verify(stateManager, never()).appendAllToResultBuffer(any(), any(), any());
                }
        }

        @Nested
        class ConfigDeserialization {

                @Test
                void newConfiguration_deserializesFromCanonicalString() {
                        AgentOrchestrationConfig original = AgentOrchestrationConfig.forEntry();
                        String canonical = original.toCanonicalString();

                        AgentOrchestrationConfig restored = handler.newConfiguration(canonical);

                        assertFalse(restored.hasToolResult());
                }
        }
}
