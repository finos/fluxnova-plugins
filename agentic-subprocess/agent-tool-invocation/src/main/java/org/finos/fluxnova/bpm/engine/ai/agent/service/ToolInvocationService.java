package org.finos.fluxnova.bpm.engine.ai.agent.service;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;

/**
 * Invokes a tool activity within an ad-hoc subprocess scope.
 *
 * <p>Implementations translate a {@link ToolCallRequest} into a process engine operation
 * and return a {@link ToolInvocationResult} indicating whether the activity was
 * successfully started. The call is <em>non-blocking</em>: the activity is triggered and
 * the method returns immediately without waiting for the activity to complete.
 *
 * <p>Failures that prevent the activity from starting (e.g. unknown tool id, engine
 * rejection) are returned as a failed {@link ToolInvocationResult} rather than thrown,
 * allowing callers to handle errors uniformly.
 */
public interface ToolInvocationService {

    /**
     * Attempts to invoke the tool identified by {@code request.toolId()} within the given
     * ad-hoc subprocess scope.
     *
     * @param runtimeService    the runtime service to execute the tool against
     * @param adHocSubprocessId the execution id of the ad-hoc subprocess scope that owns
     *                          the tool activities
     * @param catalogue         the tool catalogue for the scope; used to validate that the
     *                          requested tool is known before dispatching
     * @param request           the tool call to dispatch, carrying a stable
     *                          {@code toolCallId} for correlation and the {@code toolId}
     *                          that must match an entry in the catalogue
     * @return a successful result if the activity was started, or a failure result if the
     *         tool id was not found in the catalogue or the engine rejected the invocation
     */
    ToolInvocationResult invoke(RuntimeService runtimeService, String adHocSubprocessId, AgentToolCatalogue catalogue, ToolCallRequest request);
}
