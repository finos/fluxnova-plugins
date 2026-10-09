package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import net.jqwik.api.*;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link ConversationMapper} argument mapping.
 *
 * <p><strong>Validates: Requirements 12.3, 12.4, 12.5</strong></p>
 */
class ConversationMapperPropertyTest {

    private final ConversationMapper mapper = new ConversationMapper();

    private AgentConfig agentConfig() {
        return new AgentConfig("proc-1", "agent-1", "ollama", "llama3.1",
                "system prompt", "agent-1");
    }

    /**
     * Property 21: Argument Mapping Round-Trip —
     * For any non-blank/non-"{}" arguments string, the ConversationMapper preserves it
     * through toLlmResponse → entryToMessage cycle; blank/null/"{}" normalizes to null
     * in ToolCallRequest and reconstructs as "{}" in AssistantMessage.ToolCall.
     */
    @Property
    void nonBlankNonEmptyObjectArgumentsArePreservedThroughRoundTrip(
            @ForAll("nonTrivialArguments") String arguments) {

        // Step 1: Create a ChatResponse with a tool call carrying the arguments
        AssistantMessage assistant = AssistantMessage.builder()
                .content("Calling tool.")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "myTool", arguments)))
                .build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));

        // Step 2: toLlmResponse extracts arguments into ToolCallRequest
        LlmResponse llmResponse = mapper.toLlmResponse(response, List.of());

        assertThat(llmResponse.toolCalls()).hasSize(1);
        ToolCallRequest request = llmResponse.toolCalls().get(0);
        // Non-blank, non-"{}" arguments should be preserved as-is
        assertThat(request.arguments()).isEqualTo(arguments);

        // Step 3: Round-trip back through history → toSpringAi → extract AssistantMessage
        List<ConversationEntry> history = llmResponse.updatedHistory();
        List<Message> messages = mapper.toSpringAi(agentConfig(), new ResolvedContext(Map.of()), history);

        // Find the assistant message (last one should be ASSISTANT type)
        AssistantMessage roundTripped = (AssistantMessage) messages.stream()
                .filter(m -> m instanceof AssistantMessage)
                .reduce((first, second) -> second) // last assistant message
                .orElseThrow();

        assertThat(roundTripped.getToolCalls()).hasSize(1);
        assertThat(roundTripped.getToolCalls().get(0).arguments()).isEqualTo(arguments);
    }

    @Property
    void blankOrEmptyObjectArgumentsNormalizeToNullAndReconstructAsEmptyObject(
            @ForAll("trivialArguments") String arguments) {

        // Step 1: Create a ChatResponse with "trivial" arguments (blank, "{}", etc.)
        AssistantMessage assistant = AssistantMessage.builder()
                .content("Calling tool.")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "myTool", arguments)))
                .build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));

        // Step 2: toLlmResponse normalizes trivial arguments to null
        LlmResponse llmResponse = mapper.toLlmResponse(response, List.of());

        assertThat(llmResponse.toolCalls()).hasSize(1);
        ToolCallRequest request = llmResponse.toolCalls().get(0);
        assertThat(request.arguments())
                .as("Blank/empty-object arguments should normalize to null")
                .isNull();

        // Step 3: Round-trip: entryToMessage should reconstruct as "{}"
        List<ConversationEntry> history = llmResponse.updatedHistory();
        List<Message> messages = mapper.toSpringAi(agentConfig(), new ResolvedContext(Map.of()), history);

        AssistantMessage roundTripped = (AssistantMessage) messages.stream()
                .filter(m -> m instanceof AssistantMessage)
                .reduce((first, second) -> second)
                .orElseThrow();

        assertThat(roundTripped.getToolCalls()).hasSize(1);
        assertThat(roundTripped.getToolCalls().get(0).arguments())
                .as("Null arguments should reconstruct as '{}' in AssistantMessage.ToolCall")
                .isEqualTo("{}");
    }

    @Provide
    Arbitrary<String> nonTrivialArguments() {
        // Generate valid JSON-like argument strings that are non-blank and not just "{}"
        return Arbitraries.of(
                "{\"prompt\":\"Investigate transaction\"}",
                "{\"prompt\":\"Check AML status for customer C-42\"}",
                "{\"key\":\"value\"}",
                "{\"prompt\":\"a\"}",
                "{\"x\":1,\"y\":2}",
                "{\"nested\":{\"inner\":\"data\"}}",
                "{\"prompt\":\"Hello world, please process this request.\"}",
                "{\"action\":\"run\",\"target\":\"analysis\"}",
                "{\"items\":[1,2,3]}"
        );
    }

    @Provide
    Arbitrary<String> trivialArguments() {
        // Arguments that should be treated as "no arguments" — blank, whitespace, or "{}"
        return Arbitraries.of(
                "{}",
                " {} ",
                "",
                "  ",
                "\t",
                " "
        );
    }
}
