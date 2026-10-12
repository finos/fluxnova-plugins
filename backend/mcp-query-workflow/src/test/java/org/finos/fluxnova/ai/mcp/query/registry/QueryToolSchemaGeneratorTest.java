package org.finos.fluxnova.ai.mcp.query.registry;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import org.finos.fluxnova.ai.mcp.query.model.query.FilterQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.TaskQueryDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryToolSchemaGeneratorTest {

    @Test
    void shouldGenerateQueryToolSchemaWithQueryDtoAndMaxResults() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(FilterQueryDto.class);

        assertEquals("object", schema.type());
        assertNotNull(schema.properties());
        assertTrue(schema.properties().containsKey("queryDto"));
        assertTrue(schema.properties().containsKey("maxResults"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateQueryDtoSchemaFromRecordComponents() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(FilterQueryDto.class);

        Map<String, Object> queryDtoSchema = (Map<String, Object>) schema.properties().get("queryDto");
        assertNotNull(queryDtoSchema);
        assertEquals("object", queryDtoSchema.get("type"));

        Map<String, Object> properties = (Map<String, Object>) queryDtoSchema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("filterId"));
        assertTrue(properties.containsKey("resourceType"));
        assertTrue(properties.containsKey("name"));
        assertTrue(properties.containsKey("nameLike"));
        assertTrue(properties.containsKey("owner"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeSchemaDescriptions() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(FilterQueryDto.class);

        Map<String, Object> queryDtoSchema = (Map<String, Object>) schema.properties().get("queryDto");
        Map<String, Object> properties = (Map<String, Object>) queryDtoSchema.get("properties");
        Map<String, Object> filterIdProp = (Map<String, Object>) properties.get("filterId");

        assertEquals("string", filterIdProp.get("type"));
        assertEquals("Filter by the id of the filter.", filterIdProp.get("description"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeClassLevelSchemaDescription() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(FilterQueryDto.class);

        Map<String, Object> queryDtoSchema = (Map<String, Object>) schema.properties().get("queryDto");
        assertEquals("Query parameters for filtering saved query filters.",
                queryDtoSchema.get("description"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateMaxResultsProperty() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(FilterQueryDto.class);

        Map<String, Object> maxResultsProp = (Map<String, Object>) schema.properties().get("maxResults");
        assertNotNull(maxResultsProp);
        assertEquals("integer", maxResultsProp.get("type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleComplexRecordWithManyFields() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(TaskQueryDto.class);

        Map<String, Object> queryDtoSchema = (Map<String, Object>) schema.properties().get("queryDto");
        Map<String, Object> properties = (Map<String, Object>) queryDtoSchema.get("properties");

        // TaskQueryDto has 90+ fields
        assertTrue(properties.size() > 30);
        assertTrue(properties.containsKey("taskId"));
        assertTrue(properties.containsKey("assignee"));
        assertTrue(properties.containsKey("processInstanceId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapListFieldsAsArray() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(TaskQueryDto.class);

        Map<String, Object> queryDtoSchema = (Map<String, Object>) schema.properties().get("queryDto");
        Map<String, Object> properties = (Map<String, Object>) queryDtoSchema.get("properties");
        Map<String, Object> taskIdInProp = (Map<String, Object>) properties.get("taskIdIn");

        assertEquals("array", taskIdInProp.get("type"));
        assertNotNull(taskIdInProp.get("items"));
    }

    @Test
    void shouldGenerateStringParamSchema() {
        JsonSchema schema = QueryToolSchemaGenerator.generateStringParamSchema(
                "processDefinitionId",
                "The ID of the process definition.");

        assertEquals("object", schema.type());
        assertTrue(schema.properties().containsKey("processDefinitionId"));
        assertEquals(List.of("processDefinitionId"), schema.required());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateStringParamSchemaWithDescription() {
        JsonSchema schema = QueryToolSchemaGenerator.generateStringParamSchema(
                "myParam", "My description");

        Map<String, Object> paramProp = (Map<String, Object>) schema.properties().get("myParam");
        assertEquals("string", paramProp.get("type"));
        assertEquals("My description", paramProp.get("description"));
    }

    @Test
    void shouldNotRequireQueryDtoOrMaxResults() {
        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(FilterQueryDto.class);

        // Both queryDto and maxResults should be optional (no required list)
        assertNull(schema.required());
    }
}
