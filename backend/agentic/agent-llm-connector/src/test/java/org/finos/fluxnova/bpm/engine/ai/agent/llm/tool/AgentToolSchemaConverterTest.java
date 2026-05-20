package org.finos.fluxnova.bpm.engine.ai.agent.llm.tool;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolSchemaConverterTest {

    private final AgentToolSchemaConverter converter = new AgentToolSchemaConverter();

    @Test
    void emitsOneCallbackPerToolWithEmptyObjectSchema() {
        AgentToolEntry tool = new AgentToolEntry(
            "creditScoreCheck", "Credit Score Check",
            "Retrieves the credit score for the customer.",
            Set.of("customerId"), Set.of("creditScore"));
        AgentToolCatalogue catalogue = new AgentToolCatalogue(
            "proc-1", "agent-1", List.of(tool));

        List<ToolCallback> callbacks = converter.convert(catalogue);

        assertEquals(1, callbacks.size());
        ToolDefinition def = callbacks.get(0).getToolDefinition();
        assertEquals("creditScoreCheck", def.name());
        assertEquals("{\"type\":\"object\",\"properties\":{}}", def.inputSchema());
    }

    @Test
    void descriptionIncludesNameDescriptionReadsAndWrites() {
        AgentToolEntry tool = new AgentToolEntry(
            "creditScoreCheck", "Credit Score Check",
            "Retrieves the credit score for the customer.",
            new LinkedHashSet<>(List.of("customerId", "applicationId")),
            new LinkedHashSet<>(List.of("creditScore")));

        String description = AgentToolSchemaConverter.buildDescription(tool);

        assertTrue(description.contains("Credit Score Check"));
        assertTrue(description.contains("Retrieves the credit score for the customer."));
        assertTrue(description.contains("reads: [customerId, applicationId]"),
            () -> "expected reads in description, got: " + description);
        assertTrue(description.contains("writes: [creditScore]"),
            () -> "expected writes in description, got: " + description);
    }

    @Test
    void descriptionOmitsEmptyReadsAndWritesSections() {
        AgentToolEntry tool = new AgentToolEntry(
            "noOp", "No-Op",
            "Does nothing useful.",
            Set.of(), Set.of());

        String description = AgentToolSchemaConverter.buildDescription(tool);

        assertFalse(description.contains("reads"));
        assertFalse(description.contains("writes"));
    }

    @Test
    void callbackRefusesInvocation() {
        AgentToolEntry tool = new AgentToolEntry(
            "creditScoreCheck", "Credit Score Check", "desc", Set.of(), Set.of());
        ToolCallback callback = converter.convert(
            new AgentToolCatalogue("proc-1", "agent-1", List.of(tool))).get(0);

        assertThrows(IllegalStateException.class, () -> callback.call("{}"));
    }
}
