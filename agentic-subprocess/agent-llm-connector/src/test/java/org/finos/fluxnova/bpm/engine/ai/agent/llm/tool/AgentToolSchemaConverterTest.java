package org.finos.fluxnova.bpm.engine.ai.agent.llm.tool;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentToolSchemaConverterTest {

    private final AgentToolSchemaConverter converter = new AgentToolSchemaConverter();

    @Test
    void convert_whenCatalogueHasSingleTool_returnsOneCallbackWithEmptyObjectSchema() {
        AgentToolEntry tool = new AgentToolEntry(
            "creditScoreCheck", "Credit Score Check",
            "Retrieves the credit score for the customer.",
            Set.of("customerId"), Set.of("creditScore"));
        AgentToolCatalogue catalogue = new AgentToolCatalogue(
            "proc-1", "agent-1", List.of(tool));

        List<ToolCallback> callbacks = converter.convert(catalogue);

        assertThat(callbacks).hasSize(1);
        ToolDefinition def = callbacks.get(0).getToolDefinition();
        assertThat(def.name()).isEqualTo("creditScoreCheck");
        assertThat(def.inputSchema()).isEqualTo("{\"type\":\"object\",\"properties\":{}}");
    }

    @Test
    void buildDescription_whenToolHasAllFields_includesNameDescriptionReadsAndWrites() {
        AgentToolEntry tool = new AgentToolEntry(
            "creditScoreCheck", "Credit Score Check",
            "Retrieves the credit score for the customer.",
            new LinkedHashSet<>(List.of("customerId", "applicationId")),
            new LinkedHashSet<>(List.of("creditScore")));

        String description = AgentToolSchemaConverter.buildDescription(tool);

        assertThat(description)
            .contains("Credit Score Check")
            .contains("Retrieves the credit score for the customer.")
            .contains("reads: [customerId, applicationId]")
            .contains("writes: [creditScore]");
    }

    @Test
    void buildDescription_whenReadsAndWritesAreEmpty_omitsReadWriteSections() {
        AgentToolEntry tool = new AgentToolEntry(
            "noOp", "No-Op",
            "Does nothing useful.",
            Set.of(), Set.of());

        String description = AgentToolSchemaConverter.buildDescription(tool);

        assertThat(description)
            .doesNotContain("reads")
            .doesNotContain("writes");
    }

    @Test
    void convert_whenCallbackIsInvoked_throwsIllegalStateException() {
        AgentToolEntry tool = new AgentToolEntry(
            "creditScoreCheck", "Credit Score Check", "desc", Set.of(), Set.of());
        ToolCallback callback = converter.convert(
            new AgentToolCatalogue("proc-1", "agent-1", List.of(tool))).get(0);

        assertThatThrownBy(() -> callback.call("{}"))
            .isInstanceOf(IllegalStateException.class);
    }
}
