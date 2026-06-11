package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ActivityTypes;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessValidationHelper;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
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

        activity.addBuiltInListener(PvmEvent.EVENTNAME_START, subprocessEntryListener);

        for (ActivityImpl child : activity.getActivities()) {
            if (!AdHocSubProcessValidationHelper.isStartableActivityInAdHocScope(activity, child)) {
                continue;
            }
            child.addBuiltInListener(PvmEvent.EVENTNAME_END, subprocessToolCompletionListener);
        }
    }
}
