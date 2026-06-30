package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.ExecutionListener;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;

public class AgentSubprocessEntryListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        MessageEntity job = new MessageEntity();
        job.setExecution((ExecutionEntity) execution);
        job.setJobHandlerType(AgentOrchestrationJobHandler.TYPE);
        job.setJobHandlerConfigurationRaw(AgentOrchestrationConfig.forEntry().toCanonicalString());

        Context.getCommandContext().getJobManager().insertAndHintJobExecutor(job);
    } 
}
