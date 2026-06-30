package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.tool.AgentToolSchemaConverter;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that exercises the full HTTP path through Spring AI's OpenAI client.
 *
 * <p>This test wires a real {@link OpenAiChatModel} pointing at a WireMock server. It verifies:
 * <ul>
 *   <li>The HTTP request body sent to the LLM API has correct message/tool JSON structure</li>
 *   <li>Tool schemas (empty-object pattern) are correctly serialized on the wire</li>
 *   <li>A realistic API response with tool_calls is correctly deserialized into {@link LlmResponse}</li>
 *   <li>Multi-turn conversation history is properly serialized (assistant + tool messages)</li>
 * </ul>
 */
@WireMockTest
class SpringAiLlmServiceHttpIntegrationTest {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    // -----------------------------------------------------------------------
    // Scenario 1: Full agentic call — tool call response
    // Verifies request wire format AND tool call response parsing
    // -----------------------------------------------------------------------

    @Test
    void withToolCatalogueAndContext_requestBodyWellFormedAndToolCallIdParsed(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(CHAT_COMPLETIONS_PATH))
                .willReturn(okJson(toolCallResponseJson("call_abc123", "creditScoreCheck"))));

        SpringAiLlmService service = buildService(wmInfo);

        LlmResponse response = service.call(
                config(),
                catalogue(),
                new ResolvedContext(Map.of("customerId", "c-42")),
                List.of(ConversationEntry.user("Run a credit check")));

        // -- Verify request body structure --
        verify(postRequestedFor(urlEqualTo(CHAT_COMPLETIONS_PATH))
                .withRequestBody(matchingJsonPath("$.model", equalTo(config().model())))
                // System prompt is the first message
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
                .withRequestBody(matchingJsonPath("$.messages[0].content", containing(config().systemPrompt())))
                // User message
                .withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.messages[1].content", equalTo("Run a credit check")))
                // Context system message (appended last)
                .withRequestBody(matchingJsonPath("$.messages[2].role", equalTo("system")))
                .withRequestBody(matchingJsonPath("$.messages[2].content", containing("customerId = c-42")))
                // Tools array — verify the tool schema format on the wire
                .withRequestBody(matchingJsonPath("$.tools[0].type", equalTo("function")))
                .withRequestBody(matchingJsonPath("$.tools[0].function.name", equalTo("creditScoreCheck")))
                .withRequestBody(matchingJsonPath("$.tools[0].function.description", containing("Credit Check")))
                .withRequestBody(matchingJsonPath("$.tools[0].function.parameters.type", equalTo("object")))
                .withRequestBody(matchingJsonPath("$.tools[0].function.parameters.properties", equalToJson("{}")))
        );

        // -- Verify response parsing --
        assertEquals(1, response.toolCalls().size());
        assertEquals("call_abc123", response.toolCalls().get(0).toolCallId());
        assertEquals("creditScoreCheck", response.toolCalls().get(0).toolId());
        // updatedHistory = prior history (1 user msg) + new assistant entry
        assertEquals(2, response.updatedHistory().size());
    }

    // -----------------------------------------------------------------------
    // Scenario 2: Multi-turn conversation history serialization
    // Verifies that prior assistant tool_calls and tool results are correctly
    // serialized in the next request.
    // -----------------------------------------------------------------------

    @Test
    void withPriorToolCallHistory_assistantAndToolMessagesSerializedOnWire(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(CHAT_COMPLETIONS_PATH))
                .willReturn(okJson(doneResponseJson())));

        SpringAiLlmService service = buildService(wmInfo);

        // Simulate second turn: history contains prior assistant tool call + tool result
        List<ConversationEntry> history = List.of(
                ConversationEntry.user("Run a credit check"),
                ConversationEntry.assistant("", List.of(new ToolCallRequest("call_1", "creditScoreCheck"))),
                ConversationEntry.tool("call_1", Map.of("creditScore", 750)));

        LlmResponse response = service.call(
                config(),
                catalogue(),
                new ResolvedContext(Map.of("customerId", "c-42")),
                history);

        // Verify history messages are sent on the wire
        verify(postRequestedFor(urlEqualTo(CHAT_COMPLETIONS_PATH))
                // messages[0] = system prompt
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
                // messages[1] = user
                .withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.messages[1].content", equalTo("Run a credit check")))
                // messages[2] = assistant with tool_calls
                .withRequestBody(matchingJsonPath("$.messages[2].role", equalTo("assistant")))
                .withRequestBody(matchingJsonPath("$.messages[2].tool_calls[0].id", equalTo("call_1")))
                .withRequestBody(matchingJsonPath("$.messages[2].tool_calls[0].function.name", equalTo("creditScoreCheck")))
                // messages[3] = tool result
                .withRequestBody(matchingJsonPath("$.messages[3].role", equalTo("tool")))
                .withRequestBody(matchingJsonPath("$.messages[3].tool_call_id", equalTo("call_1")))
                .withRequestBody(matchingJsonPath("$.messages[3].content", equalTo("[750]")))
        );

        // No tool calls
        assertTrue(response.toolCalls().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Scenario 3: Multiple tools in catalogue — all serialized
    // -----------------------------------------------------------------------

    @Test
    void withMultipleToolCatalogue_allToolsIncludedInRequest(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(CHAT_COMPLETIONS_PATH))
                .willReturn(okJson(doneResponseJson())));

        SpringAiLlmService service = buildService(wmInfo);

        AgentToolCatalogue multiToolCatalogue = new AgentToolCatalogue("proc-1", "agent-1", List.of(
                new AgentToolEntry("creditScoreCheck", "Credit Check",
                        "Looks up the credit score.", Set.of("customerId"), Set.of("creditScore")),
                new AgentToolEntry("addressLookup", "Address Lookup",
                        "Fetches the address.", Set.of("customerId"), Set.of("address"))));

        service.call(config(), multiToolCatalogue, new ResolvedContext(Map.of()), List.of(ConversationEntry.user("Go")));

        verify(postRequestedFor(urlEqualTo(CHAT_COMPLETIONS_PATH))
                .withRequestBody(matchingJsonPath("$.tools[0].function.name", equalTo("creditScoreCheck")))
                .withRequestBody(matchingJsonPath("$.tools[1].function.name", equalTo("addressLookup")))
                .withRequestBody(matchingJsonPath("$.tools[1].function.parameters.type", equalTo("object")))
        );
    }

    // -----------------------------------------------------------------------
    // Scenario 4: Parallel tool calls in response
    // Note: This is almost a unit test, but is specifically testing
    // tool call Ids being deserialised correctly. Mocks too many things
    // to be moved to a single unit test
    // -----------------------------------------------------------------------

    @Test
    void withParallelToolCallResponse_allToolCallIdsParsedCorrectly(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(CHAT_COMPLETIONS_PATH))
                .willReturn(okJson(parallelToolCallResponseJson())));

        SpringAiLlmService service = buildService(wmInfo);

        LlmResponse response = service.call(
                config(),
                catalogue(),
                new ResolvedContext(Map.of()),
                List.of(ConversationEntry.user("Do everything")));

        assertEquals(2, response.toolCalls().size());
        assertEquals("call_1", response.toolCalls().get(0).toolCallId());
        assertEquals("creditScoreCheck", response.toolCalls().get(0).toolId());
        assertEquals("call_2", response.toolCalls().get(1).toolCallId());
        assertEquals("addressLookup", response.toolCalls().get(1).toolId());
    }

    private SpringAiLlmService buildService(WireMockRuntimeInfo wmInfo) {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .build()));

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .apiKey("test-key")
                .restClientBuilder(restClientBuilder)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .build();

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("openai", chatModel));
        AgentToolSchemaConverter converter = new AgentToolSchemaConverter();
        return new SpringAiLlmService(registry, converter);
    }

    private AgentConfig config() {
        return new AgentConfig("proc-1", "agent-1", "openai", "gpt-4",
                "You are a helpful agent.", "agent-1");
    }

    private AgentToolCatalogue catalogue() {
        return new AgentToolCatalogue("proc-1", "agent-1", List.of(
                new AgentToolEntry("creditScoreCheck", "Credit Check",
                        "Looks up the credit score.",
                        Set.of("customerId"), Set.of("creditScore")),
                new AgentToolEntry("addressLookup", "Address Lookup",
                        "Fetches the address.",
                        Set.of("customerId"), Set.of("address"))));
    }

    // -----------------------------------------------------------------------
    // JSON response builders — realistic OpenAI API format
    // -----------------------------------------------------------------------

    private static String toolCallResponseJson(String callId, String functionName) {
        return """
                {
                  "id": "chatcmpl-test-1",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {
                            "id": "%s",
                            "type": "function",
                            "function": {
                              "name": "%s",
                              "arguments": "{}"
                            }
                          }
                        ]
                      },
                      "finish_reason": "tool_calls"
                    }
                  ],
                  "usage": {"prompt_tokens": 50, "completion_tokens": 10, "total_tokens": 60}
                }
                """.formatted(callId, functionName);
    }

    private static String doneResponseJson() {
        return """
                {
                  "id": "chatcmpl-test-2",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "",
                        "tool_calls": null
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {"prompt_tokens": 30, "completion_tokens": 8, "total_tokens": 38}
                }
                """;
    }

    private static String parallelToolCallResponseJson() {
        return """
                {
                  "id": "chatcmpl-test-3",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {
                            "id": "call_1",
                            "type": "function",
                            "function": {
                              "name": "creditScoreCheck",
                              "arguments": "{}"
                            }
                          },
                          {
                            "id": "call_2",
                            "type": "function",
                            "function": {
                              "name": "addressLookup",
                              "arguments": "{}"
                            }
                          }
                        ]
                      },
                      "finish_reason": "tool_calls"
                    }
                  ],
                  "usage": {"prompt_tokens": 60, "completion_tokens": 15, "total_tokens": 75}
                }
                """;
    }
}
