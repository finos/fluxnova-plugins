package org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.el.AgentElFunctions;
import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.el.JuelExpressionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Process engine plugin that registers the {@code agent:arg(name, description)} EL function
 * on the engine's {@link JuelExpressionManager}.
 */
public class AgentToolInvocationEnginePlugin extends AbstractProcessEnginePlugin {

    private static final Logger log = LoggerFactory.getLogger(AgentToolInvocationEnginePlugin.class);
    public static final String AGENT_ARG_FUNCTION_NAME = "agent:arg";

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getExpressionManager() instanceof JuelExpressionManager expressionManager) {
            try {
                Method argMethod = AgentElFunctions.class.getMethod("arg", String.class, String.class);
                expressionManager.addFunction(AGENT_ARG_FUNCTION_NAME, argMethod);
                log.info("Registered EL function '{}' -> {}.arg(String, String)",
                        AGENT_ARG_FUNCTION_NAME, AgentElFunctions.class.getSimpleName());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to register EL function " + AGENT_ARG_FUNCTION_NAME, e);
            }
        }
    }
}
