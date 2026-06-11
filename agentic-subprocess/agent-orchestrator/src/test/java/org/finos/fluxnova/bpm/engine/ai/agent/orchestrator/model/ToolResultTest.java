package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultTest {

    @Nested
    class NormalConstruction {
        @Test
        void isError_returnsFalseWhenNoErrorMessage() {
            ToolResult result = new ToolResult("tc-1", "taskA", null);

            assertFalse(result.isError());
        }
    }

    @Nested
    class ErrorFactory {

        @Test
        void error_setsErrorMessageAndNullElementId() {
            ToolResult result = ToolResult.error("tc-99", "Tool not found");

            assertEquals("tc-99", result.toolCallId());
            assertNull(result.toolElementId());
            assertEquals("Tool not found", result.errorMessage());
        }

        @Test
        void error_isErrorReturnsTrue() {
            ToolResult result = ToolResult.error("tc-1", "Failure reason");

            assertTrue(result.isError());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void constructor_withErrorMessage_isError() {
            ToolResult result = new ToolResult("tc-1", "taskA", "Timed out");

            assertTrue(result.isError());
            assertEquals("Timed out", result.errorMessage());
        }
    }
}
