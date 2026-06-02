package org.finos.fluxnova.bpm.engine.ai.agent.llm.tool;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import reactor.util.annotation.NonNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts an {@link AgentToolCatalogue} into Spring AI {@link ToolCallback} instances
 * for inclusion in the LLM call.
 *
 * <p>Tools have <strong>no LLM-provided parameters</strong>: the input schema is the empty
 * object schema. Tool descriptions are augmented with structural pre/postcondition hints
 * derived from {@code reads} / {@code writes} so the LLM can reason about ordering. The
 * returned callbacks throw on invocation — Spring AI's internal tool execution must be
 * disabled by the caller, so the LLM's tool-call decision is returned to the caller
 * unmodified rather than executed in-process.</p>
 */
public class AgentToolSchemaConverter {

    private static final String EMPTY_OBJECT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    /**
     * Converts all tools in the catalogue to Spring AI {@link ToolCallback} instances.
     *
     * @param catalogue the catalogue whose tools should be converted; must not be
     *                  {@code null}
     * @return a list of callbacks in the same order as {@code catalogue.tools()};
     *         never {@code null}
     */
    public List<ToolCallback> convert(AgentToolCatalogue catalogue) {
        return catalogue.tools().stream()
                .map(this::toCallback)
                .collect(Collectors.toList());
    }

    private ToolCallback toCallback(AgentToolEntry tool) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(tool.elementId())
                .description(buildDescription(tool))
                .inputSchema(EMPTY_OBJECT_SCHEMA)
                .build();
        return new NonExecutingToolCallback(definition);
    }

    static String buildDescription(AgentToolEntry tool) {
        StringBuilder sb = new StringBuilder();
        if (tool.name() != null && !tool.name().isBlank()) {
            sb.append(tool.name());
            if (tool.description() != null && !tool.description().isBlank()) {
                sb.append(" — ").append(tool.description().trim());
            }
        } else if (tool.description() != null && !tool.description().isBlank()) {
            sb.append(tool.description().trim());
        }
        appendVariables(sb, "reads", tool.reads());
        appendVariables(sb, "writes", tool.writes());
        return sb.toString();
    }

    private static void appendVariables(StringBuilder sb, String label, Set<String> vars) {
        if (vars == null || vars.isEmpty()) return;
        if (!sb.isEmpty()) sb.append('\n');
        sb.append("  ").append(label).append(": [")
                .append(String.join(", ", vars))
                .append(']');
    }

    /**
     * ToolCallback that surfaces a definition to the LLM but refuses to be invoked.
     * Tool execution is the caller's responsibility — Spring AI must not execute them.
     */
    private record NonExecutingToolCallback(ToolDefinition toolDefinition) implements ToolCallback {

        @Override
        @NonNull
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        @NonNull
        public ToolMetadata getToolMetadata() {
            return ToolMetadata.builder().returnDirect(false).build();
        }

        @Override
        @NonNull
        public String call(@NonNull String toolInput) {
            throw new IllegalStateException(
                    "Agent tool '" + toolDefinition.name() + "' must not be invoked by Spring AI. " +
                            "Internal tool execution must be disabled — the caller is responsible for " +
                            "dispatching tool calls.");
        }
    }
}
