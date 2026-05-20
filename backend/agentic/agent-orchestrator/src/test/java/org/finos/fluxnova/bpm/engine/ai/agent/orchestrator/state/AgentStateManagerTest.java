package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.Role;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentStateManagerTest {

        private static final String EXECUTION_ID = "exec-123";

        @Mock
        private RuntimeService runtimeService;

        private AgentStateManager stateManager;

        @BeforeEach
        void setUp() {
                stateManager = new AgentStateManager(runtimeService);
        }

        @Nested
        class ConversationHistory {

                @Test
                void loadHistory_whenNoVariable_returnsEmptyList() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentConversationHistory")).thenReturn(null);

                        List<ConversationEntry> history = stateManager.loadHistory(EXECUTION_ID);

                        assertTrue(history.isEmpty());
                }

                @Test
                void saveAndLoadHistory_roundTrips() {
                        List<ConversationEntry> history = List.of(ConversationEntry.user("Hello"),
                                        ConversationEntry.assistant("Hi there", List
                                                        .of(new ToolCallRequest("tc1", "tool1"))));

                        stateManager.saveHistory(EXECUTION_ID, history);

                        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentConversationHistory"), captor.capture());

                        String json = captor.getValue();
                        assertNotNull(json);

                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentConversationHistory")).thenReturn(json);

                        List<ConversationEntry> loaded = stateManager.loadHistory(EXECUTION_ID);
                        assertEquals(2, loaded.size());
                        assertEquals(Role.USER, loaded.get(0).role());
                        assertEquals("Hello", loaded.get(0).content());
                        assertEquals(Role.ASSISTANT, loaded.get(1).role());
                        assertEquals(1, loaded.get(1).toolCalls().size());
                        assertEquals("tool1", loaded.get(1).toolCalls().get(0).toolId());
                }
        }

        @Nested
        class PendingToolCalls {

                @Test
                void savePendingToolCalls_persistsToVariable() {
                        Set<String> pending = new HashSet<>(Set.of("tc1", "tc2", "tc3"));

                        stateManager.savePendingToolCalls(EXECUTION_ID, pending);

                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentPendingToolCalls"), anyString());
                }

                @Test
                void isPendingToolCall_returnsTrueWhenPresent() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentPendingToolCalls")).thenReturn("[\"tc1\",\"tc2\"]");

                        assertTrue(stateManager.isPendingToolCall(EXECUTION_ID, "tc1"));
                }

                @Test
                void isPendingToolCall_returnsFalseWhenAbsent() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentPendingToolCalls")).thenReturn("[\"tc1\"]");

                        assertFalse(stateManager.isPendingToolCall(EXECUTION_ID, "tc-unknown"));
                }

                @Test
                void completeToolCall_removesAndReturnsTrueWhenLastCall() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentPendingToolCalls")).thenReturn("[\"tc1\"]");

                        boolean allDone = stateManager.completeToolCall(EXECUTION_ID, "tc1");

                        assertTrue(allDone);
                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentPendingToolCalls"), anyString());
                }

                @Test
                void completeToolCall_removesAndReturnsFalseWhenMoreRemain() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentPendingToolCalls")).thenReturn("[\"tc1\",\"tc2\"]");

                        boolean allDone = stateManager.completeToolCall(EXECUTION_ID, "tc1");

                        assertFalse(allDone);
                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentPendingToolCalls"), anyString());
                }
        }

        @Nested
        class ToolResultBuffer {

                @Test
                void loadToolResultBuffer_whenNoVariable_returnsEmptyList() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer")).thenReturn(null);

                        List<ToolResult> buffer = stateManager.loadToolResultBuffer(EXECUTION_ID);

                        assertTrue(buffer.isEmpty());
                }

                @Test
                void appendToResultBuffer_addsToExistingBuffer() {
                        ToolResult result1 = new ToolResult("tc1", "taskA", null);
                        ToolResult result2 = new ToolResult("tc2", "taskB", null);

                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer")).thenReturn(null);
                        stateManager.appendToResultBuffer(EXECUTION_ID, result1);

                        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentToolResultBuffer"), captor.capture());

                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer")).thenReturn(captor.getValue());
                        stateManager.appendToResultBuffer(EXECUTION_ID, result2);

                        verify(runtimeService, times(2)).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentToolResultBuffer"), captor.capture());

                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer")).thenReturn(captor.getValue());

                        List<ToolResult> buffer = stateManager.loadToolResultBuffer(EXECUTION_ID);
                        assertEquals(2, buffer.size());
                        assertEquals("tc1", buffer.get(0).toolCallId());
                        assertEquals("tc2", buffer.get(1).toolCallId());
                }

                @Test
                void appendAllToResultBuffer_addsMultipleResults() {
                        List<ToolResult> results = List.of(ToolResult.error("tc1", "Unknown tool"),
                                        ToolResult.error("tc2", "Another error"));

                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer")).thenReturn(null);

                        stateManager.appendAllToResultBuffer(EXECUTION_ID, results);

                        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentToolResultBuffer"), captor.capture());

                        when(runtimeService.getVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer")).thenReturn(captor.getValue());

                        List<ToolResult> loaded = stateManager.loadToolResultBuffer(EXECUTION_ID);
                        assertEquals(2, loaded.size());
                        assertEquals("tc1", loaded.get(0).toolCallId());
                        assertEquals("Unknown tool", loaded.get(0).errorMessage());
                }

                @Test
                void clearToolResultBuffer_removesVariable() {
                        stateManager.clearToolResultBuffer(EXECUTION_ID);

                        verify(runtimeService).removeVariableLocal(EXECUTION_ID,
                                        "_agentToolResultBuffer");
                }
        }

        @Nested
        class ToolCallQueue {

                @Test
                void loadToolCallQueue_whenNoVariable_returnsEmptyList() {
                        when(runtimeService.getVariableLocal(EXECUTION_ID, "_agentToolCallQueue"))
                                        .thenReturn(null);

                        List<ToolCallRequest> queue = stateManager.loadToolCallQueue(EXECUTION_ID);

                        assertTrue(queue.isEmpty());
                }

                @Test
                void saveAndLoadToolCallQueue_roundTrips() {
                        List<ToolCallRequest> queue = List.of(new ToolCallRequest("tc2", "tool2"),
                                        new ToolCallRequest("tc3", "tool3"));

                        stateManager.saveToolCallQueue(EXECUTION_ID, queue);

                        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                        verify(runtimeService).setVariableLocal(eq(EXECUTION_ID),
                                        eq("_agentToolCallQueue"), captor.capture());

                        when(runtimeService.getVariableLocal(EXECUTION_ID, "_agentToolCallQueue"))
                                        .thenReturn(captor.getValue());

                        List<ToolCallRequest> loaded = stateManager.loadToolCallQueue(EXECUTION_ID);
                        assertEquals(2, loaded.size());
                        assertEquals("tool2", loaded.get(0).toolId());
                        assertEquals("tool3", loaded.get(1).toolId());
                }
        }
}
