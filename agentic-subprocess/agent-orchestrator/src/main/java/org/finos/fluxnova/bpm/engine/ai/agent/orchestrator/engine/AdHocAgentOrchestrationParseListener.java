package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ActivityTypes;
import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.VariableScope;
import org.finos.fluxnova.bpm.engine.impl.Condition;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessValidationHelper;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.finos.fluxnova.bpm.engine.impl.pvm.PvmEvent;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;

/*
 * For AdHoc Subprocesses, the engine treats it as a more general subprocess at parsetime, so we
 * override parseSubProcess instead of the presumed parseAdHocSubProcess. This is different to
 * termination of the process, where a specific "completeAdHocSubProcess" method was implemented.
 */
public class AdHocAgentOrchestrationParseListener extends AbstractBpmnParseListener {

    /**
     * Keeps agentic ad-hoc scopes open until {@code RuntimeService#completeAdHocSubProcess}
     * is called. Without this, the engine's default (no-condition) behaviour completes the
     * ad-hoc as soon as a synchronous tool activity ends, which deletes the scope while
     * agent state variables still reference it ({@code ACT_FK_VAR_EXE}).
     */
    static final Condition NEVER_COMPLETE = new Condition() {
        @Override
        public boolean evaluate(DelegateExecution execution) {
            return false;
        }

        @Override
        public boolean evaluate(VariableScope scope, DelegateExecution execution) {
            return false;
        }

        @Override
        public boolean tryEvaluate(VariableScope scope, DelegateExecution execution) {
            return false;
        }
    };

    private final AgentSubprocessEntryListener subprocessEntryListener;
    private final SubprocessToolCompletionListener subprocessToolCompletionListener;

    public AdHocAgentOrchestrationParseListener(
            AgentSubprocessEntryListener subprocessEntryListener,
            SubprocessToolCompletionListener subprocessToolCompletionListener) {
        this.subprocessEntryListener = subprocessEntryListener;
        this.subprocessToolCompletionListener = subprocessToolCompletionListener;
    }

    @Override
    public void parseSubProcess(Element element, ScopeImpl scope, ActivityImpl activity) {
        if (!ActivityTypes.SUB_PROCESS_AD_HOC.equals(element.getTagName())) {
            return;
        }
        Element ext = element.element("extensionElements");
        if (ext == null) {
            return;
        }
        if (ext.elementNS(AgentModelConstants.AGENT_NS, "config") == null) {
            return;
        }

        // Orchestrator owns completion; suppress engine auto-complete after tool activities.
        activity.setProperty(BpmnParse.PROPERTYNAME_AD_HOC_COMPLETION_CONDITION, NEVER_COMPLETE);
        activity.setProperty(BpmnParse.PROPERTYNAME_AD_HOC_COMPLETION_CONDITION_TEXT, "${false}");

        activity.addBuiltInListener(PvmEvent.EVENTNAME_START, subprocessEntryListener);

        for (ActivityImpl child : activity.getActivities()) {
            if (!AdHocSubProcessValidationHelper.isStartableActivityInAdHocScope(activity, child)) {
                continue;
            }
            child.addBuiltInListener(PvmEvent.EVENTNAME_END, subprocessToolCompletionListener);
        }
    }
}
