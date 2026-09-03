package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for routing logic in {@link AgentOrchestrationJobHandler} that uses
 * the activity config property path (no registry fallback).
 *
 * <p>Tests verify that URL and agentRef are resolved from the {@code A2aRemoteCallConfig}
 * stored on the activity's property, and that no registry fallback occurs.</p>
 *
 * <p><strong>Validates: Requirements REQ-2</strong></p>
 */
@ExtendWith(MockitoExtension.class)
class AgentOrchestrationJobHandlerConfigRoutingTest {

    private static final String PROC_DEF_ID = "procDef:1:abc";
    private static final String SCOPE_EXECUTION_ID = "scope-exec-001";
    private static final String ELEMENT_ID = "agentSubprocess";
    private static final String REMOTE_AGENT_ID = "amlAgent";

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
    @Mock
    private ProcessDefinitionEntity procDefEntity;

    private AgentOrchestrationJobHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AgentOrchestrationJobHandler(
                agentConfigRegistry, toolCatalogueRegistry, contextSpecRegistry,
                contextResolver, llmService, toolInvocationService, stateManager,
                terminationHandler);

        // Common stubs
        ProcessEngineServices services = mock(ProcessEngineServices.class);
        when(services.getRepositoryService()).thenReturn(repositoryService);
        when(services.getRuntimeService()).thenReturn(runtimeService);
        when(execution.getProcessEngineServices()).thenReturn(services);
        when(execution.isActive()).thenReturn(true);
        when(execution.getId()).thenReturn(SCOPE_EXECUTION_ID);
        when(execution.getProcessDefinitionId()).thenReturn(PROC_DEF_ID);
        when(execution.getActivityId()).thenReturn(ELEMENT_ID);
        when(commandContext.getJobManager()).thenReturn(jobManager);
    }

    private void stubRegistriesAndState(AgentToolCatalogue catalogue) {
        AgentConfig agentConfig = new AgentConfig(
                PROC_DEF_ID, ELEMENT_ID, "openai", "gpt-4", "System prompt", ELEMENT_ID);
        AgentContextSpec contextSpec = new AgentContextSpec(PROC_DEF_ID, ELEMENT_ID, List.of());

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
    }

    private void stubLlmReturnsRemoteAgentCall(AgentToolCatalogue catalogue) {
        String arguments = "{\"prompt\":\"Investigate AML alerts\"}";
        List<ToolCallRequest> toolCalls = List.of(
                new ToolCallRequest("tc1", REMOTE_AGENT_ID, arguments));
        LlmResponse response = new LlmResponse("Delegating", toolCalls,
                List.of(ConversationEntry.assistant("Delegating", toolCalls)));
        when(llmService.call(any(AgentConfig.class), eq(catalogue), any(), anyList()))
                .thenReturn(response);
    }

    @Nested
    @DisplayName("URL resolved from activity config property")
    class UrlFromActivityConfig {

        @Test
        @DisplayName("dispatches A2A job with URL from activity config")
        void urlResolvedFromActivityConfig() {
            // Arrange: catalogue with a REMOTE_AGENT entry
            AgentToolEntry remoteEntry = new AgentToolEntry(
                    REMOTE_AGENT_ID, "AML Agent", "AML investigation",
                    Set.of(), Set.of(), AgentToolType.REMOTE_AGENT);
            AgentToolCatalogue catalogue = new AgentToolCatalogue(
                    PROC_DEF_ID, ELEMENT_ID, List.of(remoteEntry));

            stubRegistriesAndState(catalogue);
            stubLlmReturnsRemoteAgentCall(catalogue);

            // Mock activity lookup - config on the activity
            ActivityImpl activity = mock(ActivityImpl.class);
            A2aRemoteCallConfig config = new A2aRemoteCallConfig(
                    "http://localhost:8085/tasks/send", null, "AML Agent",
                    "AML investigation", "aml-agent");
            when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDefEntity);
            when(procDefEntity.findActivity(REMOTE_AGENT_ID)).thenReturn(activity);

            // Act
            handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

            // Assert: A2A job dispatched with correct URL
            ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
            verify(jobManager).insertAndHintJobExecutor(captor.capture());

            MessageEntity job = captor.getValue();
            assertEquals(A2aRemoteInvokeJobHandler.TYPE, job.getJobHandlerType());
            // The config string should contain the URL from the activity config
            assertTrue(job.getJobHandlerConfigurationRaw().contains("http://localhost:8085/tasks/send"),
                    "Job config must contain the URL from activity config property");
        }
    }

    @Nested
    @DisplayName("Activity config has URL but agentRef is null")
    class NullAgentRef {

        @Test
        @DisplayName("dispatches A2A job successfully when agentRef is null")
        void urlPresentButAgentRefNull() {
            // Arrange: config with URL but null agentRef
            AgentToolEntry remoteEntry = new AgentToolEntry(
                    REMOTE_AGENT_ID, "AML Agent", "AML investigation",
                    Set.of(), Set.of(), AgentToolType.REMOTE_AGENT);
            AgentToolCatalogue catalogue = new AgentToolCatalogue(
                    PROC_DEF_ID, ELEMENT_ID, List.of(remoteEntry));

            stubRegistriesAndState(catalogue);
            stubLlmReturnsRemoteAgentCall(catalogue);

            // Activity config with null agentRef
            ActivityImpl activity = mock(ActivityImpl.class);
            A2aRemoteCallConfig config = new A2aRemoteCallConfig(
                    "http://localhost:9090/agent", null, "AML Agent",
                    "AML investigation");
            when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDefEntity);
            when(procDefEntity.findActivity(REMOTE_AGENT_ID)).thenReturn(activity);

            // Act
            handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

            // Assert: job still dispatched (agentRef is optional)
            ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
            verify(jobManager).insertAndHintJobExecutor(captor.capture());

            MessageEntity job = captor.getValue();
            assertEquals(A2aRemoteInvokeJobHandler.TYPE, job.getJobHandlerType());
            assertTrue(job.getJobHandlerConfigurationRaw().contains("http://localhost:9090/agent"),
                    "Job config must contain the URL even when agentRef is null");
        }
    }

    @Nested
    @DisplayName("Activity not found in process definition")
    class ActivityNotFound {

        @Test
        @DisplayName("enqueues failure when activity is not found in process definition")
        void activityNotFoundEnqueuesFailure() {
            // Arrange
            AgentToolEntry remoteEntry = new AgentToolEntry(
                    REMOTE_AGENT_ID, "AML Agent", "AML investigation",
                    Set.of(), Set.of(), AgentToolType.REMOTE_AGENT);
            AgentToolCatalogue catalogue = new AgentToolCatalogue(
                    PROC_DEF_ID, ELEMENT_ID, List.of(remoteEntry));

            stubRegistriesAndState(catalogue);
            stubLlmReturnsRemoteAgentCall(catalogue);

            // Activity not found
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDefEntity);
            when(procDefEntity.findActivity(REMOTE_AGENT_ID)).thenReturn(null);

            // Act
            handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

            // Assert: a failure completion job is enqueued (not a successful A2A job)
            ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
            verify(jobManager).insertAndHintJobExecutor(captor.capture());

            MessageEntity job = captor.getValue();
            // The failure is dispatched as a tool-completion job (not A2aRemoteInvokeJobHandler)
            assertEquals(AgentOrchestrationJobHandler.TYPE, job.getJobHandlerType(),
                    "When activity not found, failure should be routed as tool completion");
        }
    }

    @Nested
    @DisplayName("No registry fallback")
    class NoRegistryFallback {

        @Test
        @DisplayName("No registry exists - URL is read from activity config only")
        void urlFromConfigPropertyOnly() {
            // Arrange: successful dispatch to prove config property is the only source
            AgentToolEntry remoteEntry = new AgentToolEntry(
                    REMOTE_AGENT_ID, "AML Agent", "AML investigation",
                    Set.of(), Set.of(), AgentToolType.REMOTE_AGENT);
            AgentToolCatalogue catalogue = new AgentToolCatalogue(
                    PROC_DEF_ID, ELEMENT_ID, List.of(remoteEntry));

            stubRegistriesAndState(catalogue);
            stubLlmReturnsRemoteAgentCall(catalogue);

            // Activity has valid config
            ActivityImpl activity = mock(ActivityImpl.class);
            A2aRemoteCallConfig config = new A2aRemoteCallConfig(
                    "http://agent.local:8080/tasks/send", null, "AML Agent",
                    "AML investigation", "aml-ref");
            when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDefEntity);
            when(procDefEntity.findActivity(REMOTE_AGENT_ID)).thenReturn(activity);

            // Act
            handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

            // Assert: A2A job dispatched with correct URL from config
            ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
            verify(jobManager).insertAndHintJobExecutor(captor.capture());

            MessageEntity job = captor.getValue();
            assertEquals(A2aRemoteInvokeJobHandler.TYPE, job.getJobHandlerType());
            assertTrue(job.getJobHandlerConfigurationRaw().contains("http://agent.local:8080/tasks/send"));
        }

        @Test
        @DisplayName("when activity config is null, failure is enqueued")
        void noFallbackWhenConfigMissing() {
            // Arrange
            AgentToolEntry remoteEntry = new AgentToolEntry(
                    REMOTE_AGENT_ID, "AML Agent", "AML investigation",
                    Set.of(), Set.of(), AgentToolType.REMOTE_AGENT);
            AgentToolCatalogue catalogue = new AgentToolCatalogue(
                    PROC_DEF_ID, ELEMENT_ID, List.of(remoteEntry));

            stubRegistriesAndState(catalogue);
            stubLlmReturnsRemoteAgentCall(catalogue);

            // Activity exists but has no config property
            ActivityImpl activity = mock(ActivityImpl.class);
            when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(null);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDefEntity);
            when(procDefEntity.findActivity(REMOTE_AGENT_ID)).thenReturn(activity);

            // Act
            handler.execute(AgentOrchestrationConfig.forEntry(), execution, commandContext, null);

            // Failure should be enqueued instead
            ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
            verify(jobManager).insertAndHintJobExecutor(captor.capture());

            MessageEntity job = captor.getValue();
            assertEquals(AgentOrchestrationJobHandler.TYPE, job.getJobHandlerType(),
                    "When config is null, a failure completion job should be dispatched");
        }
    }
}
