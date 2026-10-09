package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthenticationException;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandler;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job handler for asynchronous A2A remote agent invocation.
 *
 * <p>Picks up {@link MessageEntity} jobs created by the orchestration dispatcher,
 * executes the HTTP call via {@link A2aInvocationService}, stores the result as a
 * process-instance-level variable on success, and creates a completion signal job
 * so the orchestration loop can proceed.
 */
public class A2aRemoteInvokeJobHandler implements JobHandler<A2aRemoteInvokeConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(A2aRemoteInvokeJobHandler.class);

    public static final String TYPE = "a2a-remote-invoke";

    private final A2aInvocationService invocationService;
    private final RuntimeService runtimeService;

    public A2aRemoteInvokeJobHandler(A2aInvocationService invocationService,
                                     RuntimeService runtimeService) {
        this.invocationService = invocationService;
        this.runtimeService = runtimeService;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(A2aRemoteInvokeConfig config, ExecutionEntity execution,
                        CommandContext commandContext, String tenantId) {
        String url = config.url();
        String prompt = config.prompt();
        String toolCallId = config.toolCallId();
        String scopeExecutionId = config.scopeExecutionId();
        String remoteAgentId = config.remoteAgentId();
        String agentRef = config.agentRef();

        LOG.debug("Invoking remote A2A agent '{}' at '{}' for toolCallId='{}', scope='{}', agentRef='{}'",
                remoteAgentId, url, toolCallId, scopeExecutionId, agentRef);

        A2aInvocationResult result;
        try {
            result = invocationService.invoke(url, prompt, agentRef);
        } catch (A2aAuthenticationException e) {
            LOG.warn("A2A authentication failed for '{}' at '{}': {}",
                    remoteAgentId, url, e.getMessage());
            ToolResult toolResult = ToolResult.error(toolCallId,
                    "Authentication failed for remote agent '" + remoteAgentId + "': " + e.getMessage());
            createCompletionSignal(execution, toolResult, commandContext);
            return;
        }

        if (result.success()) {
            String varName = remoteAgentId + "Result";
            // Store on process instance scope so the variable survives subprocess completion.
            // Using setVariableLocal on the subprocess execution would lose it when the scope ends.
            String processInstanceId = execution.getProcessInstanceId();
            runtimeService.setVariable(processInstanceId, varName, result.responseText());
            LOG.debug("A2A invocation succeeded for '{}', stored variable '{}' on process instance '{}'",
                    remoteAgentId, varName, processInstanceId);

            ToolResult toolResult = new ToolResult(toolCallId, remoteAgentId, null);
            createCompletionSignal(execution, toolResult, commandContext);
        } else {
            LOG.warn("A2A invocation failed for '{}' at '{}': {}",
                    remoteAgentId, url, result.errorMessage());

            ToolResult toolResult = ToolResult.error(toolCallId, result.errorMessage());
            createCompletionSignal(execution, toolResult, commandContext);
        }
    }

    @Override
    public A2aRemoteInvokeConfig newConfiguration(String canonicalString) {
        return A2aRemoteInvokeConfig.fromCanonicalString(canonicalString);
    }

    @Override
    public void onDelete(A2aRemoteInvokeConfig configuration, JobEntity jobEntity) {
        // No cleanup needed
    }

    private void createCompletionSignal(ExecutionEntity execution, ToolResult toolResult,
                                        CommandContext commandContext) {
        MessageEntity job = new MessageEntity();
        job.setExecution(execution);
        job.setJobHandlerType(AgentOrchestrationJobHandler.TYPE);
        job.setJobHandlerConfigurationRaw(
                AgentOrchestrationConfig.forToolCompletion(toolResult).toCanonicalString());
        commandContext.getJobManager().insertAndHintJobExecutor(job);
    }
}
