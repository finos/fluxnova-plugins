package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for converting between String and byte[] using UTF-8 encoding,
 * and for JSON serialization/deserialization with overflow handling.
 *
 * <p>This is used by AgentStateManager to handle variables that exceed the
 * 4000-character limit of the TEXT_ column in ACT_HI_DETAIL. By converting large
 * strings to byte arrays, Fluxnova's type-based routing directs them to
 * ACT_GE_BYTEARRAY instead, which has no size limit.
 */
public class StringSerializationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(StringSerializationUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int OVERFLOW_THRESHOLD = 4000;

    private StringSerializationUtils() {
        // Utility class, not instantiable
    }

    /**
     * Converts a String to a UTF-8 encoded byte array.
     *
     * @param value the string to convert; may be null
     * @return the UTF-8 encoded bytes, or null if input was null
     */
    public static byte[] stringToBytes(String value) {
        if (value == null) {
            return null;
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Converts a UTF-8 encoded byte array back to a String.
     *
     * @param value the byte array to convert; may be null
     * @return the decoded string, or null if input was null
     */
    public static String bytesToString(byte[] value) {
        if (value == null) {
            return null;
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param value the object to serialize; must not be null
     * @return the JSON string representation
     * @throws IllegalStateException if serialization fails
     */
    public static String serializeToJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize agent state to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string to an object of the specified type.
     *
     * @param json the JSON string to deserialize
     * @param type the type reference for deserialization
     * @param <T> the type parameter
     * @return the deserialized object
     * @throws IllegalStateException if deserialization fails
     */
    public static <T> T deserializeFromJson(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize agent state from JSON", e);
        }
    }

    /**
     * Serializes an object to JSON and converts to a byte array if the serialized
     * string exceeds the overflow threshold (4000 characters). This allows Fluxnova's
     * type-based routing to use ByteArrayType for large values.
     *
     * @param value the object to serialize; must not be null
     * @return a String if under 4000 chars, or a byte[] if larger
     */
    public static Object serializeWithOverflowHandling(Object value) {
        String serialized = serializeToJson(value);
        
        if (serialized.length() > OVERFLOW_THRESHOLD) {
            Object bytesValue = stringToBytes(serialized);
            if (LOG.isDebugEnabled()) {
                LOG.debug("[OVERFLOW HANDLING] Serialized value ({} chars) converted to bytes for ByteArrayType storage", 
                    serialized.length());
            }
            return bytesValue;
        }
        return serialized;
    }

    /**
     * Deserializes a value that may be stored as either a String or byte array,
     * handling both formats transparently.
     *
     * @param variable the variable value (may be null, String, or byte[])
     * @param type the type reference for deserialization
     * @param <T> the type parameter
     * @return the deserialized object, or null if variable was null
     * @throws IllegalStateException if deserialization fails
     */
    public static <T> T deserializeWithOverflowHandling(Object variable, TypeReference<T> type) {
        if (variable == null) {
            return null;
        }
        
        String json;
        if (variable instanceof byte[]) {
            // Stored as ByteArrayType - convert back to string
            byte[] bytes = (byte[]) variable;
            json = bytesToString(bytes);
        } else if (variable instanceof String) {
            // Stored as StringType - use directly
            json = (String) variable;
        } else {
            // Unexpected type
            LOG.warn("[OVERFLOW HANDLING] Unexpected variable type: {}", 
                    variable.getClass().getSimpleName());
            return null;
        }
        
        return deserializeFromJson(json, type);
    }
}
