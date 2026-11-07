package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ActivityTypes;
import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.delegate.ExecutionListener;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessValidationHelper;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.PvmActivity;
import org.finos.fluxnova.bpm.engine.impl.pvm.PvmEvent;
import org.finos.fluxnova.bpm.engine.impl.pvm.PvmTransition;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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

        if (isAutoCompleteEnabled(element, activity)) {
            throw new BpmnParseException(
                    "Agent ad-hoc subprocess '" + element.attribute("id")
                            + "' must disable autoComplete (set autoComplete=\"false\")",
                    element);
        }

        activity.addBuiltInListener(PvmEvent.EVENTNAME_START, subprocessEntryListener);

        Map<String, Set<String>> fragmentsByStarterId = new HashMap<>();
        Map<String, String> starterIdByActivityId = new HashMap<>();

        for (ActivityImpl child : activity.getActivities()) {
            if (!AdHocSubProcessValidationHelper.isStartableActivityInAdHocScope(activity, child)) {
                continue;
            }
            String starterId = child.getId();
            Set<String> reachableIds = collectReachableActivityIds(child);
            if (starterId != null) {
                fragmentsByStarterId.put(starterId, reachableIds);
                for (String actId : reachableIds) {
                    starterIdByActivityId.putIfAbsent(actId, starterId);
                }
                child.addBuiltInListener(PvmEvent.EVENTNAME_START, createStarterTrackingListener(starterId));
            }
        }

        activity.setActivityBehavior(
                new AgentAdHocSubProcessActivityBehavior(
                        subprocessToolCompletionListener, fragmentsByStarterId, starterIdByActivityId));
    }

    private boolean isAutoCompleteEnabled(Element element, ActivityImpl activity) {
        Object prop = activity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_AUTO_COMPLETE);
        if (prop instanceof Boolean autoComplete) {
            return autoComplete;
        }
        String attr = element.attributeNS(BpmnParse.FLUXNOVA_BPMN_EXTENSIONS_NS, "autoComplete");
        if (attr == null) {
            attr = element.attributeNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "autoComplete");
        }
        if (attr == null) {
            attr = element.attribute("autoComplete");
        }
        if (attr != null) {
            return !"false".equalsIgnoreCase(attr.trim());
        }
        return false;
    }

    private Set<String> collectReachableActivityIds(ActivityImpl starter) {
        Set<String> visited = new LinkedHashSet<>();
        Queue<PvmActivity> queue = new ArrayDeque<>();
        queue.add(starter);
        while (!queue.isEmpty()) {
            PvmActivity current = queue.poll();
            if (current == null || current.getId() == null || !visited.add(current.getId())) {
                continue;
            }
            if (current.getOutgoingTransitions() != null) {
                for (PvmTransition transition : current.getOutgoingTransitions()) {
                    if (transition != null && transition.getDestination() != null) {
                        queue.add(transition.getDestination());
                    }
                }
            }
        }
        return visited;
    }

    @SuppressWarnings("unchecked")
    private ExecutionListener createStarterTrackingListener(String starterId) {
        return execution -> {
            String toolCallId = (String) execution.getVariableLocal(
                    AgentAdHocSubProcessActivityBehavior.VAR_TOOL_CALL_ID);
            if (toolCallId == null || !(execution instanceof ExecutionEntity execEntity)) {
                return;
            }
            ExecutionEntity scope = execEntity.getParent();
            while (scope != null && !scope.isScope()) {
                scope = scope.getParent();
            }
            if (scope != null) {
                Map<String, String> activeMap;
                Object existing = scope.getVariableLocal(
                        AgentAdHocSubProcessActivityBehavior.VAR_ACTIVE_FRAGMENT_TOOL_CALLS);
                if (existing instanceof Map<?, ?> map) {
                    activeMap = new HashMap<>((Map<String, String>) map);
                } else {
                    activeMap = new HashMap<>();
                }
                activeMap.put(starterId, toolCallId);
                scope.setVariableLocal(
                        AgentAdHocSubProcessActivityBehavior.VAR_ACTIVE_FRAGMENT_TOOL_CALLS, activeMap);
            }
        };
    }
}
