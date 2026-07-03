package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VariableSerializerTest {

    private VariableSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new VariableSerializer();
    }

    // ===================== Tests for serializeVariable method =====================

    @DisplayName("serializeVariable: null value should return null")
    @Test
    void serializeVariableNullValue() throws Exception {
        Object result = invokeSerializeVariable(null, null);
        assertNull(result);
    }

    @DisplayName("serializeVariable: String should be returned as-is")
    @Test
    void serializeVariableString() throws Exception {
        String value = "test string";
        Object result = invokeSerializeVariable(value, null);
        assertEquals("test string", result);
    }

    @DisplayName("serializeVariable: Integer should be returned as-is")
    @Test
    void serializeVariableInteger() throws Exception {
        Integer value = 42;
        Object result = invokeSerializeVariable(value, null);
        assertEquals(42, result);
    }

    @DisplayName("serializeVariable: Long should be returned as-is")
    @Test
    void serializeVariableLong() throws Exception {
        Long value = 999L;
        Object result = invokeSerializeVariable(value, null);
        assertEquals(999L, result);
    }

    @DisplayName("serializeVariable: Boolean should be returned as-is")
    @Test
    void serializeVariableBoolean() throws Exception {
        Boolean value = true;
        Object result = invokeSerializeVariable(value, null);
        assertEquals(true, result);
    }

    @DisplayName("serializeVariable: Date should be returned as timestamp in milliseconds")
    @Test
    void serializeVariableDate() throws Exception {
        Date date = new Date(1000000000000L);
        Object result = invokeSerializeVariable(date, null);
        assertEquals(1000000000000L, result);
    }

    @DisplayName("serializeVariable: Map should be returned as structured JSON (not escaped string)")
    @Test
    void serializeVariableMap() throws Exception {
        Map<String, Object> value = Map.of("id", "123", "name", "John");
        Object result = invokeSerializeVariable(value, null);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals("123", ((Map<?, ?>) result).get("id"));
        assertEquals("John", ((Map<?, ?>) result).get("name"));
    }

    @DisplayName("serializeVariable: Complex nested Map structures")
    @Test
    void serializeVariableComplexNestedMap() throws Exception {
        Map<String, Object> nested = Map.of("level", 2, "data", "nested");
        Map<String, Object> value = Map.of(
            "id", "123",
            "nested", nested,
            "list", List.of(1, 2, 3)
        );
        Object result = invokeSerializeVariable(value, null);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertEquals("123", resultMap.get("id"));
        assertNotNull(resultMap.get("nested"));
    }

    @DisplayName("serializeVariable: List should be handled as Map (is JSON object)")
    @Test
    void serializeVariableList() throws Exception {
        List<Object> value = List.of(1, 2, 3, "four");
        Object result = invokeSerializeVariable(value, null);
        
        assertNotNull(result);
        assertTrue(result instanceof String || result instanceof List);
    }

    @DisplayName("serializeVariable: toString fallback for unknown types")
    @Test
    void serializeVariableToStringFallback() throws Exception {
        Object value = new CustomObject("test-value");
        Object result = invokeSerializeVariable(value, null);
        
        assertEquals("CustomObject(test-value)", result);
    }

    @DisplayName("serializeVariable: Empty Map should be preserved")
    @Test
    void serializeVariableEmptyMap() throws Exception {
        Map<String, Object> value = new HashMap<>();
        Object result = invokeSerializeVariable(value, null);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @DisplayName("serializeVariable: Map with null values should preserve nulls")
    @Test
    void serializeVariableMapWithNullValues() throws Exception {
        Map<String, Object> value = new HashMap<>();
        value.put("key1", "value1");
        value.put("key2", null);
        Object result = invokeSerializeVariable(value, null);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertEquals("value1", resultMap.get("key1"));
        assertTrue(resultMap.containsKey("key2"));
        assertNull(resultMap.get("key2"));
    }

    @DisplayName("serializeVariable: Double should be converted via toString fallback")
    @Test
    void serializeVariableDouble() throws Exception {
        Double value = 3.14159;
        Object result = invokeSerializeVariable(value, null);
        assertEquals("3.14159", result);
    }

    @DisplayName("serializeVariable: Float should be converted via toString fallback")
    @Test
    void serializeVariableFloat() throws Exception {
        Float value = 2.71f;
        Object result = invokeSerializeVariable(value, null);
        assertEquals("2.71", result.toString());
    }

    // ===================== Tests for convertToMap method =====================

    @DisplayName("convertToMap: null input returns empty map")
    @Test
    void convertToMapNullInput() {
        Map<String, Object> result = serializer.convertToMap(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @DisplayName("convertToMap: HashMap input is converted")
    @Test
    void convertToMapHashMap() {
        Map<String, Object> input = new HashMap<>();
        input.put("key1", "value1");
        input.put("key2", "value2");
        
        Map<String, Object> result = serializer.convertToMap(input);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @DisplayName("convertToMap: Immutable Map is converted to new HashMap")
    @Test
    void convertToMapImmutableMap() {
        Map<String, Object> input = Map.of("key", "value");
        Map<String, Object> result = serializer.convertToMap(input);
        
        assertNotNull(result);
        assertNotSame(input, result);
        assertEquals("value", result.get("key"));
    }

    @DisplayName("convertToMap: Empty Map is preserved")
    @Test
    void convertToMapEmptyMap() {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> result = serializer.convertToMap(input);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===================== Tests for collectReturnVariables method =====================

    @DisplayName("collectReturnVariables: no return variables configured")
    @Test
    void collectReturnVariablesEmpty() {
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(), false);
        Map<String, Object> capturedVariables = Map.of("var1", "value1");
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @DisplayName("collectReturnVariables: single return variable")
    @Test
    void collectReturnVariablesSingle() {
        ToolParameter returnVar = new ToolParameter("result", "string");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(returnVar), false);
        Map<String, Object> capturedVariables = Map.of("result", "test-value");
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertEquals("test-value", result.get("result"));
    }

    @DisplayName("collectReturnVariables: multiple return variables")
    @Test
    void collectReturnVariablesMultiple() {
        ToolParameter var1 = new ToolParameter("output1", "string");
        ToolParameter var2 = new ToolParameter("output2", "integer");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(var1, var2), false);
        
        Map<String, Object> capturedVariables = new HashMap<>();
        capturedVariables.put("output1", "first");
        capturedVariables.put("output2", 42);
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertEquals("first", result.get("output1"));
        assertEquals(42, result.get("output2"));
    }

    @DisplayName("collectReturnVariables: missing variable is included as null")
    @Test
    void collectReturnVariablesMissingVariable() {
        ToolParameter returnVar = new ToolParameter("missing", "string");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(returnVar), false);
        Map<String, Object> capturedVariables = new HashMap<>();
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertTrue(result.containsKey("missing"));
        assertNull(result.get("missing"));
    }

    @DisplayName("collectReturnVariables: mixed existing and missing variables")
    @Test
    void collectReturnVariablesMixed() {
        ToolParameter var1 = new ToolParameter("exists", "string");
        ToolParameter var2 = new ToolParameter("missing", "string");
        ToolParameter var3 = new ToolParameter("alsoExists", "integer");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(var1, var2, var3), false);
        
        Map<String, Object> capturedVariables = new HashMap<>();
        capturedVariables.put("exists", "value1");
        capturedVariables.put("alsoExists", 999);
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertEquals("value1", result.get("exists"));
        assertNull(result.get("missing"));
        assertEquals(999, result.get("alsoExists"));
    }

    @DisplayName("collectReturnVariables: preserves insertion order (LinkedHashMap)")
    @Test
    void collectReturnVariablesPreservesOrder() {
        ToolParameter var1 = new ToolParameter("first", "string");
        ToolParameter var2 = new ToolParameter("second", "string");
        ToolParameter var3 = new ToolParameter("third", "string");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(var1, var2, var3), false);
        
        Map<String, Object> capturedVariables = new HashMap<>();
        capturedVariables.put("first", "1st");
        capturedVariables.put("second", "2nd");
        capturedVariables.put("third", "3rd");
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        
        Object[] keys = result.keySet().toArray();
        assertEquals("first", keys[0]);
        assertEquals("second", keys[1]);
        assertEquals("third", keys[2]);
    }

    @DisplayName("collectReturnVariables: handles complex serialized objects")
    @Test
    void collectReturnVariablesComplexObjects() {
        Map<String, Object> complexObj = new HashMap<>();
        complexObj.put("nested", Map.of("key", "value"));
        complexObj.put("number", 42);
        
        ToolParameter var = new ToolParameter("complex", "object");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(var), false);
        
        Map<String, Object> capturedVariables = Map.of("complex", complexObj);
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertNotNull(result.get("complex"));
        assertTrue(result.get("complex") instanceof Map);
    }

    @DisplayName("collectReturnVariables: handles null values in captured variables")
    @Test
    void collectReturnVariablesWithNullCapturedValues() {
        ToolParameter var1 = new ToolParameter("nullVar", "string");
        ToolParameter var2 = new ToolParameter("realVar", "string");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(var1, var2), false);
        
        Map<String, Object> capturedVariables = new HashMap<>();
        capturedVariables.put("nullVar", null);
        capturedVariables.put("realVar", "value");
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        assertTrue(result.containsKey("nullVar"));
        assertNull(result.get("nullVar"));
        assertEquals("value", result.get("realVar"));
    }

    @DisplayName("collectReturnVariables: large collection of return variables")
    @Test
    void collectReturnVariablesLargeCollection() {
        List<ToolParameter> vars = new ArrayList<>();
        Map<String, Object> capturedVars = new HashMap<>();
        
        for (int i = 0; i < 50; i++) {
            String name = "var" + i;
            vars.add(new ToolParameter(name, "string"));
            capturedVars.put(name, "value" + i);
        }
        
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), vars, false);
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVars);
        assertNotNull(result);
        assertEquals(50, result.size());
        
        assertEquals("value0", result.get("var0"));
        assertEquals("value25", result.get("var25"));
        assertEquals("value49", result.get("var49"));
    }

    @DisplayName("collectReturnVariables: case-sensitive variable matching")
    @Test
    void collectReturnVariablesCaseSensitive() {
        ToolParameter var1 = new ToolParameter("MyVar", "string");
        ToolParameter var2 = new ToolParameter("myvar", "string");
        ToolDefinition definition = new ToolDefinition("process", "tool", "desc", List.of(), List.of(var1, var2), false);
        
        Map<String, Object> capturedVariables = new HashMap<>();
        capturedVariables.put("MyVar", "uppercase");
        capturedVariables.put("myvar", "lowercase");
        
        Map<String, Object> result = serializer.collectReturnVariables(definition, capturedVariables);
        assertNotNull(result);
        
        assertEquals("uppercase", result.get("MyVar"));
        assertEquals("lowercase", result.get("myvar"));
        assertEquals(2, result.size());
    }

    // ===================== Tests for JSON parsing methods =====================

    @DisplayName("parseJsonString: null input returns null")
    @Test
    void parseJsonStringNull() {
        Object result = serializer.parseJsonString(null);
        assertNull(result);
    }

    @DisplayName("parseJsonString: empty string returns empty string")
    @Test
    void parseJsonStringEmpty() {
        Object result = serializer.parseJsonString("");
        assertEquals("", result);
    }

    @DisplayName("parseJsonString: non-JSON string returns as-is")
    @Test
    void parseJsonStringNonJson() {
        Object result = serializer.parseJsonString("not json");
        assertEquals("not json", result);
    }

    @DisplayName("parseJsonString: JSON object is parsed to Map")
    @Test
    void parseJsonStringObject() {
        String json = "{\"id\":\"123\",\"name\":\"John\"}";
        Object result = serializer.parseJsonString(json);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("name"));
    }

    @DisplayName("parseJsonString: nested JSON object is parsed correctly")
    @Test
    void parseJsonStringNestedObject() {
        String json = "{\"user\":{\"id\":\"123\",\"profile\":{\"name\":\"John\"}}}";
        Object result = serializer.parseJsonString(json);
        
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.get("user") instanceof Map);
        Map<?, ?> userMap = (Map<?, ?>) map.get("user");
        assertTrue(userMap.get("profile") instanceof Map);
    }

    @DisplayName("parseJsonString: JSON array is parsed to List")
    @Test
    void parseJsonStringArray() {
        String json = "[1,2,3]";
        Object result = serializer.parseJsonString(json);
        
        assertNotNull(result);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertTrue(list.size() > 0);
    }

    @DisplayName("parseJsonString: empty JSON object returns empty Map")
    @Test
    void parseJsonStringEmptyObject() {
        String json = "{}";
        Object result = serializer.parseJsonString(json);
        
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @DisplayName("parseJsonString: empty JSON array returns empty List")
    @Test
    void parseJsonStringEmptyArray() {
        String json = "[]";
        Object result = serializer.parseJsonString(json);
        
        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty());
    }

    @DisplayName("parseJsonString: correctly parses string ending with escaped backslash")
    @Test
    void parseJsonStringBackslashAtStringEnd() {
        // JSON: {"path":"C:\\"} - the string ends with a single backslash, quote closes the string
        String json = "{\"path\":\"C:\\\\\"}";
        
        Object result = serializer.parseJsonString(json);
        
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("path"));
        String path = (String) map.get("path");
        assertEquals("C:\\", path);
    }

    @DisplayName("parseJsonString: correctly handles mixed escape sequences")
    @Test
    void parseJsonStringMixedEscapeSequences() {
        // Complex JSON with: path with backslashes, escaped quotes, newlines
        String json = "{\"file\":\"C:\\\\Users\\\\test\\\\\",\"message\":\"Line1\\nLine2\",\"quoted\":\"He said \\\"Hi\\\"\"}";
        
        Object result = serializer.parseJsonString(json);
        
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        
        String file = (String) map.get("file");
        String message = (String) map.get("message");
        String quoted = (String) map.get("quoted");
        
        assertTrue(file.contains("Users"));
        assertTrue(message.contains("Line1") && message.contains("Line2"));
        assertTrue(quoted.contains("Hi"));
    }

    // ===================== Helper methods =====================

    private Object invokeSerializeVariable(Object value, String typeHint) throws Exception {
        Method method = VariableSerializer.class.getDeclaredMethod("serializeVariable", Object.class, String.class);
        method.setAccessible(true);
        return method.invoke(serializer, value, typeHint);
    }

    private boolean invokeIsNotInString(String str, int pos) throws Exception {
        Method method = VariableSerializer.class.getDeclaredMethod("isNotInString", String.class, int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(serializer, str, pos);
    }

    static class CustomObject {
        private final String value;

        CustomObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomObject(" + value + ")";
        }
    }
}
