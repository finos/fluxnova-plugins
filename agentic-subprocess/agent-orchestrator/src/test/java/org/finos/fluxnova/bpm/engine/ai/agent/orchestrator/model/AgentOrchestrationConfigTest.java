package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentOrchestrationConfigTest {

    @Nested
    class EntryConfig {

        @Test
        void forEntry_roundTrips() {
            AgentOrchestrationConfig config = AgentOrchestrationConfig.forEntry();

            String json = config.toCanonicalString();
            AgentOrchestrationConfig restored = AgentOrchestrationConfig.fromCanonicalString(json);

            assertFalse(restored.hasToolResult());
            assertNull(restored.toolResult());
        }
    }

    @Nested
    class ToolCompletionConfig {

        @Test
        void forToolCompletion_roundTrips() {
            ToolResult result = new ToolResult("tc-42", "creditCheck", null);
            AgentOrchestrationConfig config = AgentOrchestrationConfig.forToolCompletion(result);

            String json = config.toCanonicalString();
            AgentOrchestrationConfig restored = AgentOrchestrationConfig.fromCanonicalString(json);

            assertTrue(restored.hasToolResult());
            assertEquals("tc-42", restored.toolResult().toolCallId());
            assertEquals("creditCheck", restored.toolResult().toolElementId());
            assertNull(restored.toolResult().errorMessage());
        }

        @Test
        void forToolCompletion_withError_roundTrips() {
            ToolResult result = ToolResult.error("tc-99", "Tool not found");
            AgentOrchestrationConfig config = AgentOrchestrationConfig.forToolCompletion(result);

            String json = config.toCanonicalString();
            AgentOrchestrationConfig restored = AgentOrchestrationConfig.fromCanonicalString(json);

            assertTrue(restored.hasToolResult());
            assertEquals("tc-99", restored.toolResult().toolCallId());
            assertNull(restored.toolResult().toolElementId());
            assertEquals("Tool not found", restored.toolResult().errorMessage());
        }
    }

    @Nested
    class InvalidInput {

        @Test
        void fromCanonicalString_withMalformedJson_throwsIllegalState() {
            assertThrows(IllegalStateException.class,
                    () -> AgentOrchestrationConfig.fromCanonicalString("not-valid-json"));
        }
    }
}
