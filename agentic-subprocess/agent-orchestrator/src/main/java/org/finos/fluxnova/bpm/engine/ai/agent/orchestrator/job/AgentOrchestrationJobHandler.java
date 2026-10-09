package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AgentTerminationHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandler;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
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

    /**
     * Seed user turn for the first LLM call. Without it, the entry-turn conversation is
     * system-only (system prompt + context), which many models — local ones especially —
     * won't act on: they narrate instead of emitting tool calls. A user turn kicks off the loop.
     * TODO: make this configurable via an agent:config attribute (e.g. task/userPrompt).
     */
    private static final String INITIAL_USER_PROMPT =
            "Begin. Use the available tools to complete the task, then respond and stop.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentToolCatalogueRegistry toolCatalogueRegistry;
    private final AgentContextSpecRegistry contextSpecRegistry;
    private final AgentContextResolver contextResolver;
    private final LlmService llmService;
    private final ToolInvocationService toolInvocationService;
    private final AgentStateManager stateManager;
    private final AgentTerminationHandler agentTerminationHandler;

    public AgentOrchestrationJobHandler(AgentConfigRegistry agentConfigRegistry,
            AgentToolCatalogueRegistry toolCatalogueRegistry,
            AgentContextSpecRegistry contextSpecRegistry, AgentContextResolver contextResolver,
            LlmService llmService,
            ToolInvocationService toolInvocationService, AgentStateManager stateManager,
            AgentTerminationHandler agentTerminationHandler) {
        this.agentConfigRegistry = agentConfigRegistry;
        this.toolCatalogueRegistry = toolCatalogueRegistry;
        this.contextSpecRegistry = contextSpecRegistry;
        this.contextResolver = contextResolver;
        this.llmService = llmService;
        this.toolInvocationService = toolInvocationService;
        this.stateManager = stateManager;
        this.agentTerminationHandler = agentTerminationHandler;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(AgentOrchestrationConfig orchestratorConfig, ExecutionEntity execution,
            CommandContext commandContext, String tenantId) {
        String scopeExecutionId = execution.getId();
        LOG.debug("execute() called for scope '{}', hasToolResult={}, isActive={}, " +
                        "revision='{}', thread={}",
                scopeExecutionId,
                orchestratorConfig.hasToolResult(),
                execution.isActive(),
                execution.getRevision(),
                Thread.currentThread().getName());

        LOG.debug("execution hierarchy: id='{}', parentId='{}', superExecutionId='{}', " +
                        "isScope={}, isActive={}, activityId='{}'",
                execution.getId(),
                execution.getParentId(),
                execution.getSuperExecutionId(),
                execution.isScope(),
                execution.isActive(),
                execution.getActivityId());

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
            LOG.debug("completeToolCall() for '{}' returned allCompleted={}", result.toolCallId(), allCompleted);

            stateManager.appendToResultBuffer(runtimeService, scopeExecutionId, result);

            if (!allCompleted) {
                LOG.debug("Not all tools completed for scope '{}', parking", scopeExecutionId);
                return;
            }
            LOG.debug("All tools completed for scope '{}', proceeding to LLM", scopeExecutionId);
            // All pending tools done — fall through to next LLM call
        }

        List<ToolResult> buffer = stateManager.loadToolResultBuffer(runtimeService, scopeExecutionId);
        List<ConversationEntry> history = stateManager.loadHistory(runtimeService, scopeExecutionId);
        history = appendToolResults(history, buffer);
        stateManager.clearToolResultBuffer(runtimeService, scopeExecutionId);

        // First turn: history is empty and the mapper would send only system messages.
        // Seed a user turn so the model actually engages the tools.
        if (history.isEmpty()) {
            history.add(ConversationEntry.user(INITIAL_USER_PROMPT));
        }

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
            agentTerminationHandler.complete(runtimeService, scopeExecutionId);
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
        LOG.debug("LLM response for scope '{}': toolCalls={}", scopeExecutionId, response.toolCalls());
        stateManager.saveHistory(runtimeService, scopeExecutionId, response.updatedHistory());

        if (response.toolCalls().isEmpty()) {
            LOG.debug("No tool calls returned, triggering termination for scope '{}'", scopeExecutionId);
            // Complete the process if tool call is empty
            agentTerminationHandler.complete(runtimeService, scopeExecutionId);
            return;
        }
        LOG.debug("Dispatching scope '{}': toolCalls='{}'", scopeExecutionId, response.toolCalls());
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
            LOG.debug("dispatch() scope='{}' registering toolCallId='{}'", scopeExecutionId, tc.toolCallId());
        }
        // Save ALL pending tool call ids BEFORE any completion job is eligible
        LOG.debug("dispatch() scope='{}' saving pending set={}", scopeExecutionId, pending);
        stateManager.savePendingToolCalls(runtimeService, scopeExecutionId, pending);

        for (ToolCallRequest tc : toolCalls) {
            AgentToolEntry entry = catalogue.findById(tc.toolId()).orElse(null);

            if (entry == null) {
                LOG.warn("dispatch() scope='{}' unknown tool '{}'", scopeExecutionId, tc.toolId());
                enqueueFailure(tc.toolCallId(), "Unknown tool: " + tc.toolId(), execution, commandContext);
                continue;
            }

            if (entry.type() == AgentToolType.REMOTE_AGENT) {
                dispatchA2aJob(tc, entry, execution, commandContext);
            } else {
                ToolInvocationResult result =
                        toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, tc);
                if (!result.success()) {
                    enqueueFailure(tc.toolCallId(), result.errorMessage(), execution, commandContext);
                }
            }
        }
    }

    private void dispatchA2aJob(ToolCallRequest tc, AgentToolEntry entry,
                                ExecutionEntity execution, CommandContext commandContext) {
        String prompt = extractPromptFromArguments(tc.arguments());
        if (prompt == null) {
            LOG.warn("dispatchA2aJob() failed to extract prompt from arguments for tool '{}'", tc.toolId());
            enqueueFailure(tc.toolCallId(),
                    "Failed to extract prompt from arguments for remote agent: " + tc.toolId(),
                    execution, commandContext);
            return;
        }

        String url = getRemoteAgentUrl(entry, execution);
        if (url == null) {
            LOG.warn("dispatchA2aJob() no URL found for remote agent '{}'", entry.elementId());
            enqueueFailure(tc.toolCallId(),
                    "No URL configured for remote agent: " + entry.elementId(),
                    execution, commandContext);
            return;
        }

        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                url, prompt, tc.toolCallId(), execution.getId(), entry.elementId(),
                getRemoteAgentRef(entry, execution));

        MessageEntity job = new MessageEntity();
        job.setExecution(execution);
        job.setJobHandlerType(A2aRemoteInvokeJobHandler.TYPE);
        job.setJobHandlerConfigurationRaw(config.toCanonicalString());
        commandContext.getJobManager().insertAndHintJobExecutor(job);

        LOG.debug("dispatchA2aJob() created A2A job for agent '{}' at '{}', toolCallId='{}'",
                entry.elementId(), url, tc.toolCallId());
    }

    /**
     * Extracts the "prompt" field from a JSON arguments string.
     *
     * @param arguments the raw JSON string (e.g., {@code {"prompt":"Investigate..."}})
     * @return the extracted prompt value, or {@code null} if extraction fails
     */
    String extractPromptFromArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(arguments);
            JsonNode promptNode = node.get("prompt");
            if (promptNode == null || !promptNode.isTextual()) {
                return null;
            }
            return promptNode.asText();
        } catch (JsonProcessingException e) {
            LOG.debug("Failed to parse arguments JSON: {}", e.getMessage());
            return null;
        }
    }

    private static final String A2A_CONFIG_PROPERTY_KEY = "a2aRemoteCallConfig";

    private String getRemoteAgentUrl(AgentToolEntry entry, ExecutionEntity execution) {
        A2aRemoteCallConfig config = getA2aConfig(entry, execution);
        if (config == null || config.url() == null) {
            return null;
        }
        // Resolve EL expressions (e.g. ${remoteAgentUrl}) against the execution context
        return resolveExpression(config.url(), execution);
    }

    /**
     * Resolves a value that may contain an EL expression against the execution context.
     * Plain (non-EL) values are returned unchanged.
     */
    private String resolveExpression(String value, ExecutionEntity execution) {
        if (value.contains("${") || value.contains("#{")) {
            ExpressionManager expressionManager = Context.getProcessEngineConfiguration().getExpressionManager();
            return (String) expressionManager.createExpression(value).getValue(execution);
        }
        return value;
    }

    /**
     * Retrieves the {@link A2aRemoteCallConfig} stored as a property on the activity
     * identified by the entry's element id within the current process definition.
     */
    private A2aRemoteCallConfig getA2aConfig(AgentToolEntry entry, ExecutionEntity execution) {
        ActivityImpl activity = findActivityInScope(entry.elementId(), execution);
        if (activity == null) {
            return null;
        }
        return (A2aRemoteCallConfig) activity.getProperty(A2A_CONFIG_PROPERTY_KEY);
    }

    /**
     * Locates the {@link ActivityImpl} for the given element id within the process definition
     * associated with the execution's scope.
     *
     * <p>Uses {@link ProcessDefinitionEntity#findActivity(String)} which recursively searches
     * through nested scopes, so activities that are nested children of the ad-hoc subprocess
     * (e.g., tasks within sub-processes inside the ad-hoc scope) are found correctly.
     *
     * @param elementId the BPMN element id to locate
     * @param execution the current execution providing access to the process definition
     * @return the activity, or {@code null} if not found (with a warning logged)
     */
    private ActivityImpl findActivityInScope(String elementId, ExecutionEntity execution) {
        RepositoryService repositoryService = execution.getProcessEngineServices().getRepositoryService();
        ProcessDefinition processDefinition =
                repositoryService.getProcessDefinition(execution.getProcessDefinitionId());

        if (!(processDefinition instanceof ProcessDefinitionEntity procDefEntity)) {
            LOG.warn("Process definition '{}' is not a ProcessDefinitionEntity, cannot locate activity '{}'",
                    execution.getProcessDefinitionId(), elementId);
            return null;
        }

        ActivityImpl activity = procDefEntity.findActivity(elementId);
        if (activity == null) {
            LOG.warn("Activity '{}' not found in process definition '{}'",
                    elementId, execution.getProcessDefinitionId());
            return null;
        }

        return activity;
    }

    private String getRemoteAgentRef(AgentToolEntry entry, ExecutionEntity execution) {
        A2aRemoteCallConfig config = getA2aConfig(entry, execution);
        if (config == null || config.agentRef() == null) {
            return null;
        }

        // Resolve EL expressions (e.g. ${agentRef}) against the execution context
        return resolveExpression(config.agentRef(), execution);
    }

    private void enqueueFailure(String toolCallId, String errorMessage,
                                ExecutionEntity execution, CommandContext commandContext) {
        ToolResult failure = ToolResult.error(toolCallId, errorMessage);
        MessageEntity job = new MessageEntity();
        job.setExecution(execution);
        job.setJobHandlerType(TYPE);
        job.setJobHandlerConfigurationRaw(
                AgentOrchestrationConfig.forToolCompletion(failure).toCanonicalString());
        commandContext.getJobManager().insertAndHintJobExecutor(job);
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
