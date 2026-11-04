package org.finos.fluxnova.bpm.engine.ai.agent.llm.tool;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolType;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link AgentToolSchemaConverter}.
 *
 * <p><strong>Validates: Requirements 7.7, 7.8, 16.4</strong></p>
 */
class AgentToolSchemaConverterPropertyTest {

    private static final String EMPTY_OBJECT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";
    private static final String REMOTE_AGENT_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\"}},\"required\":[\"prompt\"]}";

    private final AgentToolSchemaConverter converter = new AgentToolSchemaConverter();

    /**
     * Property 10: Type-Aware Schema Generation —
     * For any AgentToolEntry with type REMOTE_AGENT, the generated schema contains
     * "prompt" property and "required"; for type BPMN_ACTIVITY, the schema is the
     * empty object schema.
     */
    @Property
    void remoteAgentToolAlwaysGeneratesPromptSchema(
            @ForAll("remoteAgentEntries") AgentToolEntry entry) {

        AgentToolCatalogue catalogue = new AgentToolCatalogue("proc-1", "scope-1", List.of(entry));
        List<ToolCallback> callbacks = converter.convert(catalogue);

        assertThat(callbacks).hasSize(1);
        ToolDefinition def = callbacks.get(0).getToolDefinition();
        assertThat(def.inputSchema()).isEqualTo(REMOTE_AGENT_SCHEMA);
        assertThat(def.inputSchema()).contains("\"prompt\"");
        assertThat(def.inputSchema()).contains("\"required\"");
    }

    @Property
    void bpmnActivityToolAlwaysGeneratesEmptyObjectSchema(
            @ForAll("bpmnActivityEntries") AgentToolEntry entry) {

        AgentToolCatalogue catalogue = new AgentToolCatalogue("proc-1", "scope-1", List.of(entry));
        List<ToolCallback> callbacks = converter.convert(catalogue);

        assertThat(callbacks).hasSize(1);
        ToolDefinition def = callbacks.get(0).getToolDefinition();
        assertThat(def.inputSchema()).isEqualTo(EMPTY_OBJECT_SCHEMA);
        assertThat(def.inputSchema()).doesNotContain("\"prompt\"");
        assertThat(def.inputSchema()).doesNotContain("\"required\"");
    }

    @Provide
    Arbitrary<AgentToolEntry> remoteAgentEntries() {
        Arbitrary<String> elementIds = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(30);
        Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(0).ofMaxLength(50).injectNull(0.1);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha().ofMinLength(0).ofMaxLength(100).injectNull(0.1);

        return Combinators.combine(elementIds, names, descriptions)
                .as((id, name, desc) -> new AgentToolEntry(
                        id, name, desc, Set.of(), Set.of(), AgentToolType.REMOTE_AGENT));
    }

    @Provide
    Arbitrary<AgentToolEntry> bpmnActivityEntries() {
        Arbitrary<String> elementIds = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(30);
        Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(0).ofMaxLength(50).injectNull(0.1);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha().ofMinLength(0).ofMaxLength(100).injectNull(0.1);
        Arbitrary<Set<String>> varSets = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(15)
                .set().ofMaxSize(5);

        return Combinators.combine(elementIds, names, descriptions, varSets, varSets)
                .as((id, name, desc, reads, writes) -> new AgentToolEntry(
                        id, name, desc, reads, writes, AgentToolType.BPMN_ACTIVITY));
    }
}
