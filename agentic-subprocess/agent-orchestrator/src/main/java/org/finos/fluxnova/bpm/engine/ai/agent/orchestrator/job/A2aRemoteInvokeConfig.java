package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;

/**
 * Job handler configuration for an asynchronous A2A remote agent invocation.
 *
 * <p>Carries all data needed by {@link A2aRemoteInvokeJobHandler} to invoke a remote
 * A2A agent and correlate the result back into the orchestration loop.
 *
 * @param url              the remote agent's base URL (HTTP POST target)
 * @param prompt           the instruction text to send to the remote agent
 * @param toolCallId       the LLM-assigned correlation id for this tool call
 * @param scopeExecutionId the execution id of the orchestrating scope (for variable storage)
 * @param remoteAgentId    the declared id of the remote agent (used for result variable naming)
 * @param agentRef         optional reference to a named agent auth config (nullable)
 */
public record A2aRemoteInvokeConfig(
        String url,
        String prompt,
        String toolCallId,
        String scopeExecutionId,
        String remoteAgentId,
        String agentRef
) implements JobHandlerConfiguration {

    /** Backward-compatible constructor (agentRef defaults to null). */
    public A2aRemoteInvokeConfig(String url, String prompt, String toolCallId,
                                  String scopeExecutionId, String remoteAgentId) {
        this(url, prompt, toolCallId, scopeExecutionId, remoteAgentId, null);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String toCanonicalString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize A2aRemoteInvokeConfig", e);
        }
    }

    public static A2aRemoteInvokeConfig fromCanonicalString(String canonicalString) {
        try {
            return MAPPER.readValue(canonicalString, A2aRemoteInvokeConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize A2aRemoteInvokeConfig", e);
        }
    }
}
