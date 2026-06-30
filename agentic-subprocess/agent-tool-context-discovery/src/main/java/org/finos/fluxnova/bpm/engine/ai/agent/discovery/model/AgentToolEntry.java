package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.Set;

/**
 * Describes a single tool available within a BPMN scope.
 *
 * <p>{@code reads} and {@code writes} are sets of process variable names that the
 * tool's activity is expected to consume and produce respectively, derived from the
 * activity's input/output mappings. They are informational hints and do not enforce
 * any runtime behaviour.
 *
 * @param elementId   the id of the BPMN activity that backs this tool
 * @param name        the human-readable name of the activity; may be {@code null} if
 *                    no name is set on the BPMN element
 * @param description documentation text for the activity; may be {@code null}
 * @param reads       process variable names read as inputs by this tool; never
 *                    {@code null}, but may be empty
 * @param writes      process variable names written as outputs by this tool; never
 *                    {@code null}, but may be empty
 */
public record AgentToolEntry(
    String elementId,
    String name,
    String description,
    Set<String> reads,
    Set<String> writes
) {}
