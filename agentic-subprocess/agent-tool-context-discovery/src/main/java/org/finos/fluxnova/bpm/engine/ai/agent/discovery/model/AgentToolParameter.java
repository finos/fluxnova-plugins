package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

/**
 * Describes an LLM-supplied tool parameter declared via {@code ${agent:arg(name, description)}}
 * inside a tool activity's BPMN input mapping.
 *
 * @param name        parameter name exposed in the tool's JSON Schema
 * @param description human-readable description exposed in the tool's JSON Schema
 */
public record AgentToolParameter(String name, String description) {}
