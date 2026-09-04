package org.finos.fluxnova.ai.mcp.query.registry;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * Generates {@link JsonSchema} objects from Java record classes (QueryDtos).
 * <p>
 * Uses reflection to introspect record components and their {@code @Schema} annotations
 * to produce JSON Schema definitions that expose the full structure of QueryDto types
 * through MCP tool parameters.
 */
class QueryToolSchemaGenerator {

    private QueryToolSchemaGenerator() {
    }

    /**
     * Generates a top-level JSON schema for a query tool that takes a QueryDto and optional maxResults.
     *
     * @param queryDtoClass the record class representing the query DTO
     * @return a JsonSchema for the tool's input parameters
     */
    static JsonSchema generateQueryToolSchema(Class<? extends Record> queryDtoClass) {
        Map<String, Object> queryDtoSchema = generateRecordSchema(queryDtoClass);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("queryDto", queryDtoSchema);
        properties.put("maxResults", Map.of(
                "type", "integer",
                "description", "Maximum number of results to return. "
                        + "If not specified, defaults to the configured maximum. "
                        + "Cannot exceed the configured maximum."
        ));

        return new JsonSchema("object", properties, null, null, null, null);
    }

    /**
     * Generates a top-level JSON schema for a simple string-parameter tool (e.g. XMLMcpTools).
     *
     * @param paramName   the parameter name
     * @param description the parameter description
     * @return a JsonSchema for the tool's input parameters
     */
    static JsonSchema generateStringParamSchema(String paramName, String description) {
        Map<String, Object> properties = Map.of(
                paramName, Map.of(
                        "type", "string",
                        "description", description
                )
        );
        return new JsonSchema("object", properties, List.of(paramName), null, null, null);
    }

    /**
     * Generates a JSON Schema-style map for a Java record class by introspecting its components.
     */
    private static Map<String, Object> generateRecordSchema(Class<? extends Record> recordClass) {
        RecordComponent[] components = recordClass.getRecordComponents();
        Map<String, Object> properties = new LinkedHashMap<>();

        for (RecordComponent component : components) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("type", mapJavaTypeToJsonSchemaType(component.getType()));

            // @Schema annotation may be on the field (not the record component)
            String description = getSchemaDescription(recordClass, component);
            prop.put("description", description != null ? description : "Parameter: " + component.getName());

            if (List.class.isAssignableFrom(component.getType())
                    || Set.class.isAssignableFrom(component.getType())
                    || component.getType().isArray()) {
                prop.put("items", buildItemsSchema(component));
            }

            properties.put(component.getName(), prop);
        }

        // Wrap as an object schema
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);

        // Read class-level @Schema description
        Schema classSchema = recordClass.getAnnotation(Schema.class);
        if (classSchema != null && !classSchema.description().isEmpty()) {
            schema.put("description", classSchema.description());
        }

        return schema;
    }

    /**
     * Gets the @Schema description for a record component, checking the component itself,
     * its accessor method, and its corresponding field.
     */
    private static String getSchemaDescription(Class<? extends Record> recordClass, RecordComponent component) {
        // Try record component annotation first
        Schema annotation = component.getAnnotation(Schema.class);
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }

        // Try accessor method annotation
        annotation = component.getAccessor().getAnnotation(Schema.class);
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }

        // Try field annotation (most reliable for @Schema on records)
        try {
            java.lang.reflect.Field field = recordClass.getDeclaredField(component.getName());
            annotation = field.getAnnotation(Schema.class);
            if (annotation != null && !annotation.description().isEmpty()) {
                return annotation.description();
            }
        } catch (NoSuchFieldException e) {
            // Shouldn't happen for records, but handle gracefully
        }

        return null;
    }

    private static String mapJavaTypeToJsonSchemaType(Class<?> type) {
        if (type == String.class || type.isEnum()) {
            return "string";
        } else if (type == Integer.class || type == int.class
                || type == Long.class || type == long.class) {
            return "integer";
        } else if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        } else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class) {
            return "number";
        } else if (List.class.isAssignableFrom(type)
                || Set.class.isAssignableFrom(type)
                || type.isArray()) {
            return "array";
        } else if (java.util.Date.class.isAssignableFrom(type)) {
            return "string";
        }
        return "string";
    }

    private static Map<String, Object> buildItemsSchema(RecordComponent component) {
        Class<?> itemType = String.class;

        if (component.getType().isArray()) {
            itemType = component.getType().getComponentType();
        } else {
            Type genericType = component.getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                if (actualTypes.length > 0 && actualTypes[0] instanceof Class<?> clazz) {
                    itemType = clazz;
                }
            }
        }

        return Map.of("type", mapJavaTypeToJsonSchemaType(itemType));
    }
}
