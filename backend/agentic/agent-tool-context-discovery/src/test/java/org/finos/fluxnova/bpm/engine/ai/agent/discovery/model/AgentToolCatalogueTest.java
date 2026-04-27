package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolCatalogueTest {

    private static AgentToolEntry entry(String id, String name) {
        return new AgentToolEntry(id, name, null, Set.of(), Set.of());
    }

    @Test
    void findById_whenToolExists_returnsIt() {
        AgentToolCatalogue catalogue = new AgentToolCatalogue("proc:1", "sub1",
                List.of(entry("taskA", "Task A"), entry("taskB", "Task B")));

        Optional<AgentToolEntry> result = catalogue.findById("taskB");

        assertTrue(result.isPresent());
        assertEquals("Task B", result.get().name());
    }

    @Test
    void findById_whenToolDoesNotExist_returnsEmpty() {
        AgentToolCatalogue catalogue = new AgentToolCatalogue("proc:1", "sub1",
                List.of(entry("taskA", "Task A")));

        assertTrue(catalogue.findById("nonExistent").isEmpty());
    }

    @Test
    void findById_whenCatalogueEmpty_returnsEmpty() {
        AgentToolCatalogue catalogue = new AgentToolCatalogue("proc:1", "sub1", List.of());

        assertTrue(catalogue.findById("any").isEmpty());
    }

    @Test
    void recordAccessors_returnCorrectValues() {
        AgentToolCatalogue catalogue = new AgentToolCatalogue("proc:1", "sub1", List.of());

        assertEquals("proc:1", catalogue.processDefinitionId());
        assertEquals("sub1", catalogue.elementId());
        assertTrue(catalogue.tools().isEmpty());
    }
}
