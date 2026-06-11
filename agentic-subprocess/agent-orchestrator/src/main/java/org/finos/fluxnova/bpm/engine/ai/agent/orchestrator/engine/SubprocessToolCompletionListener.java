package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.ExecutionListener;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubprocessToolCompletionListener implements ExecutionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(SubprocessToolCompletionListener.class);

    @Override
    public void notify(DelegateExecution execution) {
        String toolCallId = (String) execution.getVariable("_agentToolCallId");
        if (toolCallId == null) {
            LOG.warn("Skipping tool result processing for execution '{}' - no toolCallId found",
                    execution.getId());
            return;
        }

        ExecutionEntity execEntity = (ExecutionEntity) execution;
        ExecutionEntity scope = execEntity.getParent();
        while (scope != null && !scope.isScope()) {
            scope = scope.getParent();
        }

        ToolResult result = new ToolResult(toolCallId, execution.getCurrentActivityId(), null);

        MessageEntity job = new MessageEntity();
        job.setExecution(scope);
        job.setJobHandlerType(AgentOrchestrationJobHandler.TYPE);
        job.setJobHandlerConfigurationRaw(
                AgentOrchestrationConfig.forToolCompletion(result).toCanonicalString());

        Context.getCommandContext().getJobManager().insertAndHintJobExecutor(job);
    }
}
