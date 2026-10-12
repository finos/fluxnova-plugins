package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import java.util.Map;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.trace.AgentTraceExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdHocSubprocessTerminator implements AgentTerminationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AdHocSubprocessTerminator.class);

  private final AgentStateManager stateManager;

  public AdHocSubprocessTerminator() {
    this(new AgentStateManager());
  }

  public AdHocSubprocessTerminator(AgentStateManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  public void complete(RuntimeService runtimeService, String scopeExecutionId) {
    LOG.debug("complete() called for scope '{}'", scopeExecutionId);
    Map<String, Object> traceVariables =
        AgentTraceExporter.exportTraceVariables(stateManager, runtimeService, scopeExecutionId);
    if (!traceVariables.isEmpty()) {
      // Write traces to the process instance first. completeAdHocSubProcess(id, vars) applies
      // variables on the ad-hoc scope that is about to be deleted, which can leave
      // ACT_RU_VARIABLE rows referencing that execution (ACT_FK_VAR_EXE).
      String processInstanceId = resolveProcessInstanceId(runtimeService, scopeExecutionId);
      runtimeService.setVariables(processInstanceId, traceVariables);
    }

    Map<String, Object> locals = runtimeService.getVariablesLocal(scopeExecutionId);
    if (locals != null && !locals.isEmpty()) {
      runtimeService.removeVariablesLocal(scopeExecutionId, locals.keySet());
    }

    runtimeService.completeAdHocSubProcess(scopeExecutionId);
  }

  private static String resolveProcessInstanceId(
      RuntimeService runtimeService, String scopeExecutionId) {
    var byScope =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceId(scopeExecutionId)
            .singleResult();
    if (byScope != null) {
      return byScope.getId();
    }

    var execution =
        runtimeService.createExecutionQuery().executionId(scopeExecutionId).singleResult();
    if (execution == null) {
      return scopeExecutionId;
    }
    return execution.getProcessInstanceId();
  }
}
