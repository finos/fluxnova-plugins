package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.finos.fluxnova.bpm.engine.impl.pvm.delegate.ActivityExecution;

import java.util.Map;
import java.util.Set;

/**
 * Custom {@link AdHocSubProcessActivityBehavior} for agentic ad-hoc subprocesses.
 *
 * <p>Instead of firing tool completion when a starter activity ends, this behavior hooks into
 * {@link #concurrentChildExecutionEnded(ActivityExecution, ActivityExecution)} so tool completion
 * is recorded only after the entire triggered activity fragment (sequential chains, conditional
 * paths, parallel branches, and loops) has finished.
 */
public class AgentAdHocSubProcessActivityBehavior extends AdHocSubProcessActivityBehavior {

    static final String VAR_TOOL_CALL_ID = "_agentToolCallId";
    static final String VAR_ACTIVE_FRAGMENT_TOOL_CALLS = "_agentActiveFragmentToolCalls";

    private final SubprocessToolCompletionListener subprocessToolCompletionListener;
    private final Map<String, Set<String>> fragmentsByStarterId;
    private final Map<String, String> starterIdByActivityId;

    public AgentAdHocSubProcessActivityBehavior(
            SubprocessToolCompletionListener subprocessToolCompletionListener) {
        this(subprocessToolCompletionListener, Map.of(), Map.of());
    }

    public AgentAdHocSubProcessActivityBehavior(
            SubprocessToolCompletionListener subprocessToolCompletionListener,
            Map<String, Set<String>> fragmentsByStarterId,
            Map<String, String> starterIdByActivityId) {
        this.subprocessToolCompletionListener = subprocessToolCompletionListener;
        this.fragmentsByStarterId = fragmentsByStarterId;
        this.starterIdByActivityId = starterIdByActivityId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void concurrentChildExecutionEnded(
            ActivityExecution scopeExecution, ActivityExecution endedExecution) {
        String endedActivityId =
                endedExecution.getActivity() != null ? endedExecution.getActivity().getId() : null;
        String starterId =
                endedActivityId != null ? starterIdByActivityId.get(endedActivityId) : null;
        Set<String> fragmentActivityIds =
                starterId != null ? fragmentsByStarterId.getOrDefault(starterId, Set.of()) : Set.of();

        String toolCallId = (String) endedExecution.getVariableLocal(VAR_TOOL_CALL_ID);
        Map<String, String> activeFragmentToolCalls = null;
        Object rawMap = scopeExecution.getVariableLocal(VAR_ACTIVE_FRAGMENT_TOOL_CALLS);
        if (rawMap instanceof Map<?, ?> map) {
            activeFragmentToolCalls = (Map<String, String>) map;
        }
        if (toolCallId == null && starterId != null && activeFragmentToolCalls != null) {
            toolCallId = activeFragmentToolCalls.get(starterId);
            if (toolCallId != null) {
                endedExecution.setVariableLocal(VAR_TOOL_CALL_ID, toolCallId);
            }
        }
        if (toolCallId == null) {
            toolCallId = (String) endedExecution.getVariable(VAR_TOOL_CALL_ID);
        }

        boolean siblingStillActive = hasOtherActiveFragmentExecution(
                scopeExecution, endedExecution, toolCallId, fragmentActivityIds);

        if (!siblingStillActive && endedExecution instanceof DelegateExecution delegateExecution) {
            if (starterId != null && activeFragmentToolCalls != null) {
                activeFragmentToolCalls.remove(starterId);
                scopeExecution.setVariableLocal(VAR_ACTIVE_FRAGMENT_TOOL_CALLS, activeFragmentToolCalls);
            }
            subprocessToolCompletionListener.notify(delegateExecution);
        }

        super.concurrentChildExecutionEnded(scopeExecution, endedExecution);
    }

    private boolean hasOtherActiveFragmentExecution(
            ActivityExecution scopeExecution,
            ActivityExecution endedExecution,
            String toolCallId,
            Set<String> fragmentActivityIds) {
        for (ActivityExecution child : scopeExecution.getExecutions()) {
            if (child == endedExecution || !child.isActive() || child.isEnded()) {
                continue;
            }
            if (toolCallId != null && toolCallId.equals(child.getVariableLocal(VAR_TOOL_CALL_ID))) {
                return true;
            }
            if (!fragmentActivityIds.isEmpty()
                    && child.getActivity() != null
                    && fragmentActivityIds.contains(child.getActivity().getId())) {
                return true;
            }
        }
        return false;
    }
}
