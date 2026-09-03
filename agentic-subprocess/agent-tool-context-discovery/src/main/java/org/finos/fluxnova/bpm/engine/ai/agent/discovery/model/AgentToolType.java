package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

/**
 * Discriminator for the type of tool represented by an {@link AgentToolEntry}.
 * Used by the orchestration routing logic to determine the invocation mechanism.
 */
public enum AgentToolType {

    /** A standard BPMN activity tool backed by a local activity in the ad-hoc subprocess. */
    BPMN_ACTIVITY,

    /** A remote A2A-compliant agent invoked via HTTP JSON-RPC. */
    REMOTE_AGENT
}
