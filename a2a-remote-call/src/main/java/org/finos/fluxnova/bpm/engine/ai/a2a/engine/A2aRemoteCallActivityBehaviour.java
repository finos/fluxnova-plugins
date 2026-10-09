package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthenticationException;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aInvocationResult;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.delegate.BpmnError;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.FlowNodeActivityBehavior;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.finos.fluxnova.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a synchronous A2A remote call for a service task.
 * <p>
 * Resolves {@code url} and {@code prompt} as EL expressions against the current execution,
 * invokes the remote agent via {@link A2aInvocationService}, stores the result as a process
 * variable ({@code {activityId}Result}), or throws {@link BpmnError} on failure.
 */
public class A2aRemoteCallActivityBehaviour extends FlowNodeActivityBehavior {

    private static final Logger LOG = LoggerFactory.getLogger(A2aRemoteCallActivityBehaviour.class);

    private static final String ERROR_CODE = "a2a-remote-call-error";
    private static final String AUTH_ERROR_CODE = "a2a-auth-error";

    private final A2aInvocationService invocationService;

    public A2aRemoteCallActivityBehaviour(A2aInvocationService invocationService) {
        this.invocationService = invocationService;
    }

    @Override
    public void execute(ActivityExecution execution) throws Exception {
        ActivityImpl activity = (ActivityImpl) execution.getActivity();
        A2aRemoteCallConfig config = (A2aRemoteCallConfig) activity.getProperty(
                A2aRemoteCallParseListener.CONFIG_PROPERTY_KEY);

        if (config == null) {
            throw new BpmnError(ERROR_CODE,
                    "No A2A remote call configuration found on activity '" + activity.getId() + "'");
        }

        // Resolve url and prompt as EL expressions against the current execution
        ExpressionManager expressionManager = Context.getProcessEngineConfiguration().getExpressionManager();

        String resolvedUrl = (String) expressionManager.createExpression(config.url()).getValue(execution);
        String resolvedPrompt = (String) expressionManager.createExpression(config.prompt()).getValue(execution);

        LOG.debug("A2A - Invoking remote agent at '{}' with prompt for activity '{}'",
                resolvedUrl, activity.getId());

        // Invoke the remote A2A agent with auth
        A2aInvocationResult result;
        try {
            result = invocationService.invoke(resolvedUrl, resolvedPrompt, config.agentRef());
        } catch (A2aAuthenticationException e) {
            LOG.error("A2A - Authentication failed for activity '{}': {}",
                    activity.getId(), e.getMessage());
            throw new BpmnError(AUTH_ERROR_CODE, e.getMessage());
        }

        if (result.success()) {
            // Store result as process variable: {activityId}Result
            String resultVariableName = activity.getId() + "Result";
            execution.setVariable(resultVariableName, result.responseText());
            LOG.debug("A2A - Stored result in variable '{}' for activity '{}'",
                    resultVariableName, activity.getId());

            // Complete the activity — take outgoing sequence flow
            leave(execution);
        } else {
            // Throw BpmnError on failure — catchable by error boundary events
            LOG.error("A2A - Remote call failed for activity '{}': {}",
                    activity.getId(), result.errorMessage());
            throw new BpmnError(ERROR_CODE, result.errorMessage());
        }
    }
}
