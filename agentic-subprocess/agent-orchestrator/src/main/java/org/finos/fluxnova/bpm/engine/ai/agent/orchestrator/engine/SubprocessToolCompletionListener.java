package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.ExecutionListener;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.IoMapping;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.OutputParameter;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, Object> outputs = extractOutputs(execEntity);
        ToolResult result = new ToolResult(toolCallId, execution.getCurrentActivityId(), null, outputs);

        MessageEntity job = new MessageEntity();
        job.setExecution(scope);
        job.setJobHandlerType(AgentOrchestrationJobHandler.TYPE);
        job.setJobHandlerConfigurationRaw(
                AgentOrchestrationConfig.forToolCompletion(result).toCanonicalString());

        Context.getCommandContext().getJobManager().insertAndHintJobExecutor(job);
    }

    private Map<String, Object> extractOutputs(ExecutionEntity execution) {
        ActivityImpl activity = execution.getActivity();
        if (activity == null) {
            return Map.of();
        }
        IoMapping ioMapping = activity.getIoMapping();
        if (ioMapping == null) {
            return Map.of();
        }
        List<OutputParameter> outputParameters = ioMapping.getOutputParameters();
        if (outputParameters == null || outputParameters.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> outputs = new LinkedHashMap<>();
        for (OutputParameter outputParameter : outputParameters) {
            String name = outputParameter.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            outputs.put(name, resolveOutputValue(execution, outputParameter));
        }
        return outputs;
    }

    private Object resolveOutputValue(ExecutionEntity execution, OutputParameter outputParameter) {
        ParameterValueProvider provider = outputParameter.getValueProvider();
        if (provider != null) {
            try {
                return provider.getValue(execution);
            } catch (Exception e) {
                LOG.debug("Falling back to variable lookup for output parameter '{}'",
                        outputParameter.getName(), e);
            }
        }
        return execution.getVariable(outputParameter.getName());
    }
}
