package org.finos.fluxnova.bpm.engine.ai.agent.el;

import org.finos.fluxnova.bpm.engine.impl.context.BpmnExecutionContext;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;

import java.util.Map;

/**
 * Custom JUEL functions under the {@code agent:} namespace for BPMN tool input mappings.
 */
public final class AgentElFunctions {

    public static final String VAR_TOOL_ARGS = "_agentToolArgs";

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_ARGS = new ThreadLocal<>();

    private AgentElFunctions() {
    }

    /**
     * Resolves the LLM-supplied tool argument {@code name} from the current tool execution context.
     * The {@code description} parameter is static metadata extracted at BPMN parse time.
     */
    public static Object arg(String name, String description) {
        if (name == null) {
            return null;
        }
        BpmnExecutionContext bpmnContext = Context.getBpmnExecutionContext();
        if (bpmnContext != null) {
            ExecutionEntity execution = bpmnContext.getExecution();
            if (execution != null) {
                Object localArgs = execution.getVariableLocal(VAR_TOOL_ARGS);
                if (localArgs instanceof Map<?, ?> map) {
                    return map.get(name);
                }
                Object scopedArgs = execution.getVariable(VAR_TOOL_ARGS);
                if (scopedArgs instanceof Map<?, ?> map) {
                    return map.get(name);
                }
            }
        }
        Map<String, Object> fallback = THREAD_LOCAL_ARGS.get();
        if (fallback != null) {
            return fallback.get(name);
        }
        return null;
    }

    public static void setCurrentArguments(Map<String, Object> args) {
        if (args == null) {
            THREAD_LOCAL_ARGS.remove();
        } else {
            THREAD_LOCAL_ARGS.set(Map.copyOf(args));
        }
    }

    public static void clearCurrentArguments() {
        THREAD_LOCAL_ARGS.remove();
    }
}
