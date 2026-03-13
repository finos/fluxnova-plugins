package org.finos.fluxnova.ai.mcp.query.autoconfigure;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSpecificationFilterTest {

    @Test
    void doesNotFilterWhenExcludeSetIsEmpty() {
        var filter = new ToolSpecificationFilter(Set.of());
        var specs = createToolSpecs("queryTasks", "queryJobs", "queryUsers");

        var result = filter.postProcessAfterInitialization(specs, "toolSpecs");
        var resultList = assertInstanceOf(List.class, result);

        assertEquals(3, resultList.size());
    }

    @Test
    void filtersExcludedToolsByName() {
        var filter = new ToolSpecificationFilter(Set.of("queryJobs", "queryUsers"));
        var specs = createToolSpecs("queryTasks", "queryJobs", "queryUsers", "queryBatches");

        @SuppressWarnings("unchecked")
        List<McpServerFeatures.SyncToolSpecification> result =
                (List<McpServerFeatures.SyncToolSpecification>) filter.postProcessAfterInitialization(specs, "toolSpecs");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("queryTasks", result.get(0).tool().name());
        assertEquals("queryBatches", result.get(1).tool().name());
    }

    @Test
    void ignoresNonListBeans() {
        var filter = new ToolSpecificationFilter(Set.of("queryJobs"));
        var bean = "some-string-bean";

        Object result = filter.postProcessAfterInitialization(bean, "someBean");

        assertEquals(bean, result);
    }

    @Test
    void ignoresListsOfOtherTypes() {
        var filter = new ToolSpecificationFilter(Set.of("queryJobs"));
        var list = List.of("a", "b", "c");

        Object result = filter.postProcessAfterInitialization(list, "someListBean");

        assertEquals(list, result);
    }

    @Test
    void handlesEmptyToolSpecList() {
        var filter = new ToolSpecificationFilter(Set.of("queryJobs"));
        var specs = new ArrayList<McpServerFeatures.SyncToolSpecification>();

        var result = filter.postProcessAfterInitialization(specs, "toolSpecs");
        var resultList = assertInstanceOf(List.class, result);

        assertTrue(resultList.isEmpty());
    }

    @Test
    void filtersAllToolsIfAllExcluded() {
        var filter = new ToolSpecificationFilter(Set.of("queryTasks", "queryJobs"));
        var specs = createToolSpecs("queryTasks", "queryJobs");

        @SuppressWarnings("unchecked")
        List<McpServerFeatures.SyncToolSpecification> result =
                (List<McpServerFeatures.SyncToolSpecification>) filter.postProcessAfterInitialization(specs, "toolSpecs");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void leavesUnmatchedToolsIntact() {
        var filter = new ToolSpecificationFilter(Set.of("nonExistentTool"));
        var specs = createToolSpecs("queryTasks", "queryJobs");

        @SuppressWarnings("unchecked")
        List<McpServerFeatures.SyncToolSpecification> result =
                (List<McpServerFeatures.SyncToolSpecification>) filter.postProcessAfterInitialization(specs, "toolSpecs");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    private static List<McpServerFeatures.SyncToolSpecification> createToolSpecs(String... names) {
        List<McpServerFeatures.SyncToolSpecification> specs = new ArrayList<>();
        for (String name : names) {
            var tool = new McpSchema.Tool(name, null, "description of " + name, null, null, null, null);
            specs.add(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler((exchange, request) -> null)
                    .build());
        }
        return specs;
    }
}
