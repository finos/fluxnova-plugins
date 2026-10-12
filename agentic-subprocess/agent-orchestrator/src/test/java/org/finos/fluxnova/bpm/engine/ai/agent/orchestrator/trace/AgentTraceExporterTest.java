package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.junit.jupiter.api.Test;

class AgentTraceExporterTest {

  @Test
  void formatTrace_includesAssistantMessagesToolCallsAndToolOutputs() {
    List<ConversationEntry> history =
        List.of(
            ConversationEntry.assistant(
                "Checking credit profile.",
                List.of(new ToolCallRequest("tc-1", "ServiceTask_CreditScoreCheck"))),
            ConversationEntry.tool(
                "tc-1",
                Map.of(
                    "status",
                    "ok",
                    "toolElementId",
                    "ServiceTask_CreditScoreCheck",
                    "outputs",
                    Map.of("creditScore", 710, "aiRiskScore", 0.25))),
            ConversationEntry.assistant("Credit profile reviewed.", List.of()));

    String trace = AgentTraceExporter.formatTrace(history);

    assertTrue(trace.contains("Assistant: Checking credit profile."));
    assertTrue(trace.contains("-> tool call: ServiceTask_CreditScoreCheck"));
    assertTrue(trace.contains("creditScore=710"));
    assertTrue(trace.contains("Assistant: Credit profile reviewed."));
    assertEquals("Credit profile reviewed.", AgentTraceExporter.extractLastAssistantMessage(history));
  }
}
