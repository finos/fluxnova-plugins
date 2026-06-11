package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
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
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandler;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Job handler that drives a single step of the scope execution loop.
 *
 * <p>Each execution of this handler represents one turn: it reads the current
 * conversation history and any buffered tool results from scope-local variables,
 * resolves the agent configuration and tool catalogue, calls the LLM, and then
 * either dispatches the tool activities the LLM requested or signals completion
 * of the scope if the LLM returned no tool calls.
 *
 * <p>Two entry paths are handled by a single handler type:
 * <ul>
 *   <li><b>Entry</b> — a fresh turn triggered on scope entry
 *       ({@link AgentOrchestrationConfig#forEntry()}).</li>
 *   <li><b>Tool completion</b> — a continuation triggered when a dispatched tool
 *       activity finishes ({@link AgentOrchestrationConfig#forToolCompletion(ToolResult)}).
 *       The handler accumulates results until all pending tools are complete, then
 *       proceeds to the next LLM call.</li>
 * </ul>
 */
public class AgentOrchestrationJobHandler implements JobHandler<AgentOrchestrationConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(AgentOrchestrationJobHandler.class);

    public static final String TYPE = "agent-orchestration-step";

    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentToolCatalogueRegistry toolCatalogueRegistry;
    private final AgentContextSpecRegistry contextSpecRegistry;
    private final AgentContextResolver contextResolver;
    private final LlmService llmService;
    private final ToolInvocationService toolInvocationService;
    private final AgentStateManager stateManager;
    private final AgentTerminationHandler AgentTerminationHandler;

    public AgentOrchestrationJobHandler(AgentConfigRegistry agentConfigRegistry,
            AgentToolCatalogueRegistry toolCatalogueRegistry,
            AgentContextSpecRegistry contextSpecRegistry, AgentContextResolver contextResolver,
            LlmService llmService,
            ToolInvocationService toolInvocationService, AgentStateManager stateManager,
            AgentTerminationHandler AgentTerminationHandler) {
        this.agentConfigRegistry = agentConfigRegistry;
        this.toolCatalogueRegistry = toolCatalogueRegistry;
        this.contextSpecRegistry = contextSpecRegistry;
        this.contextResolver = contextResolver;
        this.llmService = llmService;
        this.toolInvocationService = toolInvocationService;
        this.stateManager = stateManager;
        this.AgentTerminationHandler = AgentTerminationHandler;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(AgentOrchestrationConfig orchestratorConfig, ExecutionEntity execution,
            CommandContext commandContext, String tenantId) {
        String scopeExecutionId = execution.getId();
        RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
        RepositoryService repositoryService = execution.getProcessEngineServices().getRepositoryService();

        if (!execution.isActive()) {
            LOG.debug("Scope execution '{}' is no longer active, skipping orchestration step",
                    scopeExecutionId);
            return;
        }

        if (orchestratorConfig.hasToolResult()) {
            ToolResult result = orchestratorConfig.toolResult();

            if (!stateManager.isPendingToolCall(runtimeService, scopeExecutionId, result.toolCallId())) {
                LOG.debug(
                        "ToolResult '{}' not in pending set, discarding (duplicate or late arrival)",
                        result.toolCallId());
                return;
            }

            boolean allCompleted =
                    stateManager.completeToolCall(runtimeService, scopeExecutionId, result.toolCallId());
            stateManager.appendToResultBuffer(runtimeService, scopeExecutionId, result);

            if (!allCompleted) {
                return;
            }
            // All pending tools done — fall through to next LLM call
        }

        List<ToolResult> buffer = stateManager.loadToolResultBuffer(runtimeService, scopeExecutionId);
        List<ConversationEntry> history = stateManager.loadHistory(runtimeService, scopeExecutionId);
        history = appendToolResults(history, buffer);
        stateManager.clearToolResultBuffer(runtimeService, scopeExecutionId);

        AgentConfig agentConfig = agentConfigRegistry
                .resolve(repositoryService, execution.getProcessDefinitionId(), execution.getActivityId())
                .orElseThrow(() -> new IllegalStateException("No AgentConfig found for "
                        + execution.getProcessDefinitionId() + "/" + execution.getActivityId()));
        AgentToolCatalogue catalogue = toolCatalogueRegistry
                .resolve(repositoryService, execution.getProcessDefinitionId(), execution.getActivityId())
                .orElseThrow(() -> new IllegalStateException("No AgentToolCatalogue found for "
                        + execution.getProcessDefinitionId() + "/" + execution.getActivityId()));

        if (catalogue.tools().isEmpty()) {
            LOG.warn(
                    "Tool catalogue is empty for activity '{}' in process '{}', terminating execution '{}'",
                    execution.getActivityId(), execution.getProcessDefinitionId(),
                    scopeExecutionId);
            AgentTerminationHandler.complete(runtimeService, scopeExecutionId);
            return;
        }

        // Fallback to empty spec if no context is declared — resolver will include all process
        // variables
        AgentContextSpec contextSpec = contextSpecRegistry
                .resolve(repositoryService, execution.getProcessDefinitionId(), execution.getActivityId())
                .orElse(new AgentContextSpec(execution.getProcessDefinitionId(),
                        execution.getActivityId(), List.of()));
        ResolvedContext context = contextResolver.resolve(runtimeService, scopeExecutionId, contextSpec);


        LlmResponse response =
                llmService.call(agentConfig, catalogue, context, history);
        stateManager.saveHistory(runtimeService, scopeExecutionId, response.updatedHistory());

        if (response.toolCalls().isEmpty()) {
            // Complete the process if tool call is empty
            AgentTerminationHandler.complete(runtimeService, scopeExecutionId);
            return;
        }

        dispatch(runtimeService, scopeExecutionId, catalogue, response.toolCalls(), execution, commandContext);
    }

    @Override
    public AgentOrchestrationConfig newConfiguration(String canonicalString) {
        return AgentOrchestrationConfig.fromCanonicalString(canonicalString);
    }

    @Override
    public void onDelete(AgentOrchestrationConfig configuration, JobEntity jobEntity) {
        // No cleanup needed
    }

    private void dispatch(RuntimeService runtimeService, String scopeExecutionId, AgentToolCatalogue catalogue,
            List<ToolCallRequest> toolCalls, ExecutionEntity execution,
            CommandContext commandContext) {
        Set<String> pending = new HashSet<>();

        for (ToolCallRequest tc : toolCalls) {
            pending.add(tc.toolCallId());
            ToolInvocationResult result =
                    toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, tc);
            if (!result.success()) {
                // Synchronous failure — no BPMN activity will complete, so no listener will fire.
                // Instantiate an equivalent completion job so the failure travels through the same
                // tool-completion path as listener-driven results, keeping the pending set
                // consistent.
                ToolResult failure = ToolResult.error(tc.toolCallId(), result.errorMessage());
                MessageEntity job = new MessageEntity();
                job.setExecution(execution);
                job.setJobHandlerType(TYPE);
                job.setJobHandlerConfigurationRaw(
                        AgentOrchestrationConfig.forToolCompletion(failure).toCanonicalString());
                commandContext.getJobManager().insertAndHintJobExecutor(job);
            }
        }
        stateManager.savePendingToolCalls(runtimeService, scopeExecutionId, pending);
    }


    private List<ConversationEntry> appendToolResults(List<ConversationEntry> history,
            List<ToolResult> results) {
        List<ConversationEntry> updated = new ArrayList<>(history);
        for (ToolResult result : results) {
            Map<String, Object> resultContent =
                    result.isError() ? Map.of("error", result.errorMessage())
                            : Map.of("status", "ok");
            updated.add(ConversationEntry.tool(result.toolCallId(), resultContent));
        }
        return updated;
    }
}
