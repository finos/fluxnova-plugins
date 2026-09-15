package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StringSerializationUtilsTest {

    @Nested
    class StringBytesConversion {

        @Test
        void stringToBytes_convertsToUtf8() {
            String value = "Hello, 世界";
            byte[] bytes = StringSerializationUtils.stringToBytes(value);
            
            assertNotNull(bytes);
            assertEquals(value, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }

        @Test
        void stringToBytes_withNull_returnsNull() {
            assertNull(StringSerializationUtils.stringToBytes(null));
        }

        @Test
        void bytesToString_decodesUtf8() {
            String original = "Test with émojis 🎉";
            byte[] bytes = original.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            
            String result = StringSerializationUtils.bytesToString(bytes);
            assertEquals(original, result);
        }

        @Test
        void bytesToString_withNull_returnsNull() {
            assertNull(StringSerializationUtils.bytesToString(null));
        }
    }

    @Nested
    class JsonSerialization {

        @Test
        void serializeToJson_convertsObjectToJson() {
            Map<String, String> data = Map.of("key", "value", "name", "test");
            
            String json = StringSerializationUtils.serializeToJson(data);
            
            assertNotNull(json);
            assertTrue(json.contains("\"key\""));
            assertTrue(json.contains("\"value\""));
        }

        @Test
        void serializeToJson_withList_producesArray() {
            List<String> list = List.of("item1", "item2", "item3");
            
            String json = StringSerializationUtils.serializeToJson(list);
            
            assertTrue(json.startsWith("["));
            assertTrue(json.endsWith("]"));
            assertTrue(json.contains("item1"));
        }

        @Test
        void deserializeFromJson_reconstructsObject() {
            Map<String, Object> original = Map.of("id", 42, "name", "test");
            String json = StringSerializationUtils.serializeToJson(original);
            
            Map<String, Object> restored = StringSerializationUtils.deserializeFromJson(
                json, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            assertEquals(original, restored);
        }

        @Test
        void deserializeFromJson_withList_reconstructsList() {
            List<String> original = List.of("a", "b", "c");
            String json = StringSerializationUtils.serializeToJson(original);
            
            List<String> restored = StringSerializationUtils.deserializeFromJson(
                json,
                new TypeReference<List<String>>() {}
            );
            
            assertEquals(original, restored);
        }
    }

    @Nested
    class OverflowHandling {

        @Test
        void serializeWithOverflowHandling_underThreshold_returnsString() {
            Map<String, String> data = Map.of("key", "value");
            
            Object result = StringSerializationUtils.serializeWithOverflowHandling(data);
            
            assertInstanceOf(String.class, result);
            String json = (String) result;
            assertTrue(json.length() < 4000);
        }

        @Test
        void serializeWithOverflowHandling_overThreshold_returnsBytes() {
            // Create a large object that serializes to > 4000 chars
            Map<String, String> data = new HashMap<>();
            String largeValue = "x".repeat(500);
            for (int i = 0; i < 10; i++) {
                data.put("key" + i, largeValue);
            }
            
            Object result = StringSerializationUtils.serializeWithOverflowHandling(data);
            
            assertInstanceOf(byte[].class, result);
            byte[] bytes = (byte[]) result;
            String decoded = StringSerializationUtils.bytesToString(bytes);
            assertTrue(decoded.length() > 4000);
        }

        @Test
        void deserializeWithOverflowHandling_withString_returnsObject() {
            Map<String, String> original = Map.of("test", "data");
            String json = StringSerializationUtils.serializeToJson(original);
            
            Map<String, String> result = StringSerializationUtils.deserializeWithOverflowHandling(
                json,
                new TypeReference<Map<String, String>>() {}
            );
            
            assertEquals(original, result);
        }

        @Test
        void deserializeWithOverflowHandling_withBytes_returnsObject() {
            Map<String, String> original = Map.of("test", "data");
            String json = StringSerializationUtils.serializeToJson(original);
            byte[] bytes = StringSerializationUtils.stringToBytes(json);
            
            Map<String, String> result = StringSerializationUtils.deserializeWithOverflowHandling(
                bytes,
                new TypeReference<Map<String, String>>() {}
            );
            
            assertEquals(original, result);
        }

        @Test
        void deserializeWithOverflowHandling_withNull_returnsNull() {
            Object result = StringSerializationUtils.deserializeWithOverflowHandling(
                null,
                new TypeReference<Map<String, String>>() {}
            );
            
            assertNull(result);
        }

        @Test
        void roundTripThroughOverflowHandling_preservesData() {
            List<String> original = List.of("item1", "item2", "item3");
            
            Object serialized = StringSerializationUtils.serializeWithOverflowHandling(original);
            List<String> restored = StringSerializationUtils.deserializeWithOverflowHandling(
                serialized,
                new TypeReference<List<String>>() {}
            );
            
            assertEquals(original, restored);
        }
    }
}
