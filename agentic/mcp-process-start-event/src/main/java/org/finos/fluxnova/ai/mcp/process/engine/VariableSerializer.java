package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles serialization and collection of process variables for MCP responses.
 * 
 * Responsible for:
 * - Collecting configured return variables from process-scope
 * - Serializing variables to JSON-compatible format
 * - Parsing JSON strings to structured objects
 * - Converting between variable map types (VariableMap to HashMap)
 * 
 * This class ensures that return variables are properly formatted and that
 * individual serialization failures do not prevent other variables from being included.
 */
public class VariableSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(VariableSerializer.class);

    /**
     * Collects configured return variables from pre-captured process-scope variables.
     *
     * Return variables are resolved exclusively from the pre-captured variables map
     * that was obtained at the first async boundary. Execution-local variables are
     * not considered. If a configured variable does not exist, it is included with
     * a null value. Individual variable retrieval or serialization failures do not
     * prevent other variables from being collected.
     *
     * @param definition the tool definition with return variable configuration
     * @param capturedVariablesObj the pre-captured process-scope variables (VariableMap or Map)
     * @return map of variable names to serialized values (or null if missing/failed)
     */
    public Map<String, Object> collectReturnVariables(ToolDefinition definition, Object capturedVariablesObj) {
        Map<String, Object> values = new LinkedHashMap<>();

        // Convert to a Map if needed (handles VariableMap type from Fluxnova)
        Map<String, Object> capturedVariables = convertToMap(capturedVariablesObj);

        LOG.debug("collectReturnVariables: capturedVariables = {}", capturedVariables);
        LOG.debug("collectReturnVariables: returnVariables count = {}", definition.returnVariables().size());

        for (ToolParameter returnVar : definition.returnVariables()) {
            try {
                Object value = capturedVariables.get(returnVar.name());
                LOG.debug("collectReturnVariables: getting {} = {} ", returnVar.name(), value);
                Object serialized = serializeVariable(value, returnVar.type());

                LOG.debug("collectReturnVariables: after serialize {} = {} ", returnVar.name(), serialized);
                values.put(returnVar.name(), serialized);
                LOG.debug("MCP - Collected return variable '{}': {}",
                        returnVar.name(), serialized != null ? "value set" : "null");
            } catch (Exception e) {
                LOG.warn("MCP - Failed to collect return variable '{}', setting to null",
                        returnVar.name(), e);
                values.put(returnVar.name(), null);
            }
        }

        return values;
    }

    /**
     * Serializes a variable value to JSON-compatible format using Fluxnova/Spin.
     *
     * <h2>Supported Types</h2>
     *
     * <ul>
     *   <li>String: Returned as-is</li>
     *   <li>Integer, Long: Returned as numeric value</li>
     *   <li>Boolean: Returned as boolean value</li>
     *   <li>Date: Returned as timestamp in milliseconds</li>
     *   <li>JSON objects/arrays: Returned as structured JSON</li>
     *   <li>Spin JSON: Unwrapped and returned as structured JSON</li>
     *   <li>Serializable Objects: Serialized via Spin.JSON()</li>
     *   <li>Other types: Serialized via toString() or null on failure</li>
     * </ul>
     *
     * <h2>JSON Structure Preservation</h2>
     *
     * JSON values are returned as structured objects or arrays, not escaped
     * strings. For example, a JSON object {@code {"id":"123","name":"John"}}
     * is returned as a structured JSON object, not as an escaped string.
     *
     * <h2>Error Handling</h2>
     *
     * Individual serialization failures do not cause exceptions. Instead,
     * the variable is included in the response with a null value, and a
     * warning is logged.
     *
     * @param value the variable value to serialize, may be null
     * @param typeHint optional type hint from the parameter definition
     * @return a JSON-compatible object suitable for response inclusion,
     *         or null if serialization failed or value was null
     */
    public Object serializeVariable(Object value, String typeHint) {
        if (value == null) {
            return null;
        }

        try {
            // String, Integer, Long, Boolean types
            if (value instanceof String || value instanceof Integer ||
                value instanceof Long || value instanceof Boolean) {
                return value;
            }

            // Date handling
            if (value instanceof java.util.Date) {
                return ((java.util.Date) value).getTime();
            }

            // Spin JSON - unwrap to native JSON structure
            if (isSpinJson(value)) {
                return unwrapSpinJson(value);
            }

            // JSON objects (if value is already parsed JSON)
            if (isJsonObject(value)) {
                return value;  // Return as structured JSON, not escaped string
            }

            // Java objects - attempt Spin serialization
            if (isSpinAvailable()) {
                return serializeWithSpin(value);
            }

            // Fallback: try toString() for simple objects
            return value.toString();

        } catch (Exception e) {
            LOG.warn("MCP - Failed to serialize variable of type {}, returning null: {}",
                    value.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Parses a JSON string back to a structured object (Map/List).
     *
     * Converts JSON string representations to structured Java objects
     * (Map for JSON objects, List for JSON arrays) for proper JSON response
     * formatting. Returns structured objects rather than escaped strings
     * to match the documented behavior that "JSON values are returned as
     * structured objects or arrays, not escaped strings."
     *
     * @param jsonString the JSON string to parse
     * @return the parsed JSON object structure as Map/List, or the string if parsing fails
     */
    public Object parseJsonString(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return jsonString;
        }
        
        try {
            String trimmed = jsonString.trim();
            
            // Parse JSON object: {...}
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                return parseJsonObject(trimmed);
            }
            
            // Parse JSON array: [...]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                return parseJsonArray(trimmed);
            }
            
            // Not JSON, return as-is
            return jsonString;
        } catch (Exception e) {
            LOG.debug("MCP - Failed to parse JSON string, returning as-is: {}", e.getMessage());
            return jsonString;  // Return as-is if parsing fails
        }
    }

    /**
     * Parses a JSON object string into a Map.
     * Simple recursive descent parser supporting nested objects and arrays.
     * 
     * @param jsonObject the JSON object string
     * @return parsed Map
     */
    private Map<String, Object> parseJsonObject(String jsonObject) {
        Map<String, Object> result = new LinkedHashMap<>();
        String content = jsonObject.substring(1, jsonObject.length() - 1).trim();
        
        if (content.isEmpty()) {
            return result;
        }
        
        // Split by commas that are not inside nested structures or strings
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            
            if ((ch == '{' || ch == '[') && isNotInString(content, i)) {
                depth++;
            } else if ((ch == '}' || ch == ']') && isNotInString(content, i)) {
                depth--;
            } else if (ch == ',' && depth == 0 && isNotInString(content, i)) {
                // Process key-value pair
                String pair = content.substring(start, i).trim();
                processJsonPair(result, pair);
                start = i + 1;
            }
        }
        
        // Process last pair
        if (start < content.length()) {
            String pair = content.substring(start).trim();
            processJsonPair(result, pair);
        }
        
        return result;
    }

    /**
     * Parses a JSON array string into a List.
     * 
     * @param jsonArray the JSON array string
     * @return parsed List
     */
    private List<Object> parseJsonArray(String jsonArray) {
        List<Object> result = new ArrayList<>();
        String content = jsonArray.substring(1, jsonArray.length() - 1).trim();
        
        if (content.isEmpty()) {
            return result;
        }
        
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            
            if ((ch == '{' || ch == '[') && isNotInString(content, i)) {
                depth++;
            } else if ((ch == '}' || ch == ']') && isNotInString(content, i)) {
                depth--;
            } else if ((ch == ',' || i == content.length() - 1) && depth == 0 && isNotInString(content, i)) {
                int end = (i == content.length() - 1 && ch != ',') ? i + 1 : i;
                String valueStr = content.substring(start, end).trim();
                if (!valueStr.isEmpty()) {
                    result.add(parseJsonValue(valueStr));
                }
                start = i + 1;
            }
        }
        
        return result;
    }

    /**
     * Parses a single JSON value (string, number, boolean, null, object, or array).
     * 
     * @param valueStr the JSON value string
     * @return parsed value
     */
    private Object parseJsonValue(String valueStr) {
        valueStr = valueStr.trim();
        
        if (valueStr.isEmpty()) {
            return null;
        }
        
        // null
        if ("null".equals(valueStr)) {
            return null;
        }
        
        // boolean
        if ("true".equals(valueStr)) {
            return true;
        }
        if ("false".equals(valueStr)) {
            return false;
        }
        
        // number
        if (valueStr.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
            try {
                if (valueStr.contains(".") || valueStr.contains("e") || valueStr.contains("E")) {
                    return Double.parseDouble(valueStr);
                } else {
                    return Long.parseLong(valueStr);
                }
            } catch (NumberFormatException e) {
                return valueStr;
            }
        }
        
        // string
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return unquoteString(valueStr);
        }
        
        // object
        if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            return parseJsonObject(valueStr);
        }
        
        // array
        if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
            return parseJsonArray(valueStr);
        }
        
        // unknown
        return valueStr;
    }

    /**
     * Processes a single "key": value pair from JSON.
     */
    private void processJsonPair(Map<String, Object> map, String pair) {
        int colonIndex = findColonIndex(pair);
        if (colonIndex > 0) {
            String keyPart = pair.substring(0, colonIndex).trim();
            String valuePart = pair.substring(colonIndex + 1).trim();
            
            String key = unquoteString(keyPart);
            Object value = parseJsonValue(valuePart);
            map.put(key, value);
        }
    }

    /**
     * Finds the colon that separates key from value (not inside strings/brackets).
     */
    private int findColonIndex(String pair) {
        int depth = 0;
        for (int i = 0; i < pair.length(); i++) {
            char ch = pair.charAt(i);
            if ((ch == '{' || ch == '[') && isNotInString(pair, i)) {
                depth++;
            } else if ((ch == '}' || ch == ']') && isNotInString(pair, i)) {
                depth--;
            } else if (ch == ':' && depth == 0 && isNotInString(pair, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if a character position is inside a JSON string (not escaped).
     * Correctly handles escaped backslashes by counting consecutive backslashes
     * before a quote to determine if the quote itself is escaped.
     * 
     * @param str the string to check
     * @param pos the position to check
     * @return true if position is inside an unescaped string
     */
    private boolean isNotInString(String str, int pos) {
        boolean inString = false;
        for (int i = 0; i < pos; i++) {
            if (str.charAt(i) == '"' && !isEscaped(str, i)) {
                inString = !inString;
            }
        }
        return !inString;
    }

    /**
     * Determines if a quote character at the given position is escaped by counting
     * the number of consecutive backslashes before it. An odd number of backslashes
     * means the quote is escaped; an even number means it is not.
     * 
     * Examples:
     * - "text\"quote" → position of \" has 1 backslash → escaped (true)
     * - "path\\" → position of closing " has 2 backslashes → not escaped (false)
     * - "mixed\\\"quoted" → position of \" has 3 backslashes → escaped (true)
     * 
     * @param str the string to check
     * @param pos the position of the quote to check
     * @return true if the quote is escaped, false otherwise
     */
    private boolean isEscaped(String str, int pos) {
        if (pos == 0) return false;
        int backslashCount = 0;
        for (int i = pos - 1; i >= 0 && str.charAt(i) == '\\'; i--) {
            backslashCount++;
        }
        return backslashCount % 2 == 1;
    }

    /**
     * Removes quotes from a JSON string value and unescapes special characters.
     * 
     * @param quotedStr the quoted string
     * @return unquoted string
     */
    private String unquoteString(String quotedStr) {
        if (quotedStr.startsWith("\"") && quotedStr.endsWith("\"")) {
            String unquoted = quotedStr.substring(1, quotedStr.length() - 1);
            // Unescape common JSON escape sequences
            unquoted = unquoted.replace("\\\"", "\"");
            unquoted = unquoted.replace("\\\\", "\\");
            unquoted = unquoted.replace("\\n", "\n");
            unquoted = unquoted.replace("\\r", "\r");
            unquoted = unquoted.replace("\\t", "\t");
            return unquoted;
        }
        return quotedStr;
    }

    /**
     * Checks if a value is a Spin JSON object.
     *
     * @param value the value to check
     * @return true if value is a Spin JSON object, false otherwise
     */
    private boolean isSpinJson(Object value) {
        // Check if value is org.finos.fluxnova.spin.json.SpinJsonObject or similar
        return value != null && value.getClass().getName()
                .startsWith("org.finos.fluxnova.spin.json.");
    }

    /**
     * Unwraps a Spin JSON object to extract the underlying JSON structure.
     *
     * @param spinJsonValue the Spin JSON value to unwrap
     * @return the underlying JSON structure as a parsed object, or null if unwrapping fails
     */
    private Object unwrapSpinJson(Object spinJsonValue) {
        // Extract underlying JSON from Spin wrapper
        try {
            // Spin JSON has methods like toJSON(), toString(), etc.
            java.lang.reflect.Method toJsonMethod = spinJsonValue.getClass().getMethod("toJSON");
            String jsonString = (String) toJsonMethod.invoke(spinJsonValue);
            // Parse back to structured object (not escaped string)
            return parseJsonString(jsonString);
        } catch (Exception e) {
            LOG.warn("MCP - Failed to unwrap Spin JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a value is already a JSON-compatible Map structure.
     *
     * @param value the value to check
     * @return true if value is a Map, false otherwise
     */
    private boolean isJsonObject(Object value) {
        // Check if already a JSON-compatible Map structure
        return value instanceof java.util.Map;
    }

    /**
     * Checks if the Spin library is available on the classpath.
     *
     * @return true if Spin is available, false otherwise
     */
    private boolean isSpinAvailable() {
        try {
            Class.forName("org.finos.fluxnova.spin.Spin");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Serializes a Java object to JSON using Spin.
     *
     * @param value the object to serialize
     * @return the serialized JSON object structure, or null if serialization fails
     */
    private Object serializeWithSpin(Object value) {
        // Use Fluxnova Spin for JSON serialization
        try {
            // Implementation using org.finos.fluxnova.spin.Spin.JSON()
            // Spin.JSON(value).toString() -> JSON string
            // Parse back to structured object for JSON response
            Class<?> spinClass = Class.forName("org.finos.fluxnova.spin.Spin");
            java.lang.reflect.Method jsonMethod = spinClass.getMethod("JSON", Object.class);
            Object spinJson = jsonMethod.invoke(null, value);
            java.lang.reflect.Method toStringMethod = spinJson.getClass().getMethod("toString");
            String jsonString = (String) toStringMethod.invoke(spinJson);
            return parseJsonString(jsonString);
        } catch (Exception e) {
            LOG.warn("MCP - Spin serialization failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts VariableMap (or any Map-like object) to a standard Map<String, Object>.
     * Handles both VariableMap from the Fluxnova framework and mock HashMap objects.
     *
     * @param variablesObj the variables object (could be VariableMap or HashMap)
     * @return a Map<String, Object> representing the variables
     */
    public Map<String, Object> convertToMap(Object variablesObj) {

        if (variablesObj == null) {
            LOG.debug("convertToMap: input is null, returning empty map");
            return new HashMap<>();
        }

        // If it's already a Map, convert entries to a new HashMap
        if (variablesObj instanceof java.util.Map) {
            LOG.debug("convertToMap: is instanceof Map");
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> varMap = (java.util.Map<String, Object>) variablesObj;
            LOG.debug("convertToMap: varMap = {}", varMap);
            Map<String, Object> result = new HashMap<>(varMap);
            LOG.debug("convertToMap: created new HashMap = {}", result);
            return result;
        }

        LOG.debug("convertToMap: is NOT instanceof Map, trying entrySet()");

        // If VariableMap-like object with a way to get entries
        try {
            // Try to invoke entrySet() method (common to Map interface)
            java.lang.reflect.Method entrySetMethod = variablesObj.getClass().getMethod("entrySet");
            @SuppressWarnings("unchecked")
            java.util.Set<java.util.Map.Entry<String, Object>> entries =
                (java.util.Set<java.util.Map.Entry<String, Object>>) entrySetMethod.invoke(variablesObj);

            Map<String, Object> result = new HashMap<>();
            for (java.util.Map.Entry<String, Object> entry : entries) {
                result.put(entry.getKey(), entry.getValue());
            }
            LOG.debug("convertToMap: created from entrySet = {}", result);
            return result;
        } catch (Exception e) {
            LOG.warn("MCP - Failed to convert variables object to Map: {}", e.getMessage());
            LOG.debug("convertToMap - exception = {}", e);
            return new HashMap<>();
        }
    }
}
