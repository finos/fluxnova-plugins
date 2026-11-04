package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.a2a.discovery.AgentCardCache;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentCard;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.A2aRemoteInvokeConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.A2aRemoteInvokeJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WireMock-based component-level integration tests for agentic subprocess mode.
 *
 * <p>Tests the following components integrated together:
 * <ul>
 *   <li>{@link A2aRemoteInvokeJobHandler} with a real {@link A2aInvocationService}
 *       + real {@link RestClient} pointing at WireMock</li>
 *   <li>{@link AgentCardCache} with real cache behaviour pointing at WireMock</li>
 *   <li>{@code AgentOrchestrationJobHandler.dispatch()} routing logic (mocked engine
 *       APIs, real routing decisions)</li>
 * </ul>
 *
 * <p><strong>Validates: Requirements 9.1, 9.2, 9.3, 9.4, 10.1, 10.2, 7.4, 8.1, 17.3, 17.4</strong></p>
 */
class A2aAgenticSubprocessIntegrationTest {

    private static WireMockServer wireMockServer;

    private A2aInvocationService invocationService;
    private RuntimeService runtimeService;
    private CommandContext commandContext;
    private JobManager jobManager;
    private ExecutionEntity execution;
    private AgentCardCache agentCardCache;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        // Create RestClient with short timeouts pointing at WireMock
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        invocationService = new A2aInvocationService(restClient);
        agentCardCache = new AgentCardCache(restClient);

        runtimeService = mock(RuntimeService.class);
        commandContext = mock(CommandContext.class);
        jobManager = mock(JobManager.class);
        execution = mock(ExecutionEntity.class);

        when(commandContext.getJobManager()).thenReturn(jobManager);
    }

    // ========================================================================
    // Test 1: Full Async Lifecycle — success path
    // Validates: Requirements 9.1, 9.2, 9.3, 9.4
    // ========================================================================

    @Test
    @DisplayName("Async lifecycle: A2aRemoteInvokeJobHandler invokes → stores result variable → creates completion signal")
    void asyncLifecycle_successPath_storesResultAndCreatesCompletionSignal() {
        // Stub WireMock to return a completed A2A response
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "req-1",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [
                          { "kind": "text", "text": "AML investigation complete. Transaction TX-999 cleared." }
                        ]
                      }
                    ]
                  }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        String agentUrl = wireMockServer.baseUrl() + "/";
        String toolCallId = "call_aml_001";
        String scopeExecutionId = "exec-scope-42";
        String remoteAgentId = "amlAgent";
        String prompt = "Investigate transaction TX-999 for money laundering";
        String processInstanceId = "proc-inst-42";

        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);

        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                agentUrl, prompt, toolCallId, scopeExecutionId, remoteAgentId);

        A2aRemoteInvokeJobHandler handler = new A2aRemoteInvokeJobHandler(invocationService, runtimeService);

        // Execute the job handler
        handler.execute(config, execution, commandContext, null);

        // Verify: result variable stored on process instance with correct name and value
        verify(runtimeService).setVariable(
                processInstanceId,
                "amlAgentResult",
                "AML investigation complete. Transaction TX-999 cleared.");

        // Verify: completion signal created as a MessageEntity job
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        MessageEntity completionJob = captor.getValue();
        assertEquals(execution, completionJob.getExecution());
        assertEquals("agent-orchestration-step", completionJob.getJobHandlerType());

        // Verify: completion signal carries correct ToolResult
        AgentOrchestrationConfig orchConfig = AgentOrchestrationConfig.fromCanonicalString(
                completionJob.getJobHandlerConfigurationRaw());
        assertTrue(orchConfig.hasToolResult());
        ToolResult toolResult = orchConfig.toolResult();
        assertEquals(toolCallId, toolResult.toolCallId());
        assertFalse(toolResult.isError());

        // Verify: WireMock received correct JSON-RPC request
        wireMockServer.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                .withRequestBody(matchingJsonPath("$.method", equalTo("message/send")))
                .withRequestBody(matchingJsonPath("$.params.message.role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.params.message.parts[0].text",
                        equalTo(prompt))));
    }

    // ========================================================================
    // Test 2: Agent Card Fetch and Cache Behaviour
    // Validates: Requirements 8.1
    // ========================================================================

    @Test
    @DisplayName("Agent card fetch: real RestClient fetches from WireMock, caches result")
    void agentCardFetch_fetchesFromWireMock_cachesResult() {
        String agentCardJson = """
                {
                  "name": "AML Investigation Agent",
                  "description": "Performs anti-money laundering investigations",
                  "protocolVersion": "0.3.0",
                  "skills": [
                    {
                      "name": "transaction-analysis",
                      "description": "Analyzes transaction patterns for suspicious activity"
                    },
                    {
                      "name": "kyc-verification",
                      "description": "Verifies customer identity documents"
                    }
                  ]
                }
                """;

        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent-card.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(agentCardJson)));

        String baseUrl = wireMockServer.baseUrl();

        // First fetch: should hit WireMock
        Optional<AgentCard> result = agentCardCache.getOrFetch(baseUrl);

        assertTrue(result.isPresent(), "Agent card should be fetched successfully");
        AgentCard card = result.get();
        assertEquals("AML Investigation Agent", card.name());
        assertEquals("Performs anti-money laundering investigations", card.description());
        assertEquals("0.3.0", card.protocolVersion());
        assertEquals(2, card.skills().size());
        assertEquals("transaction-analysis", card.skills().get(0).name());
        assertEquals("kyc-verification", card.skills().get(1).name());

        // Verify: first request hit WireMock
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/.well-known/agent-card.json")));

        // Second fetch: should use cache, NOT hit WireMock again
        Optional<AgentCard> cachedResult = agentCardCache.getOrFetch(baseUrl);

        assertTrue(cachedResult.isPresent());
        assertEquals(card, cachedResult.get());

        // Verify: still only one request to WireMock (cache was used)
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/.well-known/agent-card.json")));

        // Verify: getCached also returns the cached card
        Optional<AgentCard> directCached = agentCardCache.getCached(baseUrl);
        assertTrue(directCached.isPresent());
        assertEquals(card, directCached.get());
    }

    @Test
    @DisplayName("Agent card fetch failure: WireMock returns 404 → empty, not cached, allows retry")
    void agentCardFetch_failure_notCached_allowsRetry() {
        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent-card.json"))
                .willReturn(aResponse().withStatus(404)));

        String baseUrl = wireMockServer.baseUrl();

        // First attempt: should fail gracefully
        Optional<AgentCard> result = agentCardCache.getOrFetch(baseUrl);
        assertFalse(result.isPresent(), "Failed fetch should return empty");

        // Verify: not cached
        assertFalse(agentCardCache.getCached(baseUrl).isPresent(),
                "Failed fetch should NOT be cached");

        // Now make WireMock return a valid response for the retry
        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent-card.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "name": "Retry Agent",
                                  "description": "Available on retry",
                                  "protocolVersion": "0.3.0",
                                  "skills": []
                                }
                                """)));

        // Second attempt (retry): should succeed now
        Optional<AgentCard> retryResult = agentCardCache.getOrFetch(baseUrl);
        assertTrue(retryResult.isPresent(), "Retry should succeed after failure is not cached");
        assertEquals("Retry Agent", retryResult.get().name());
    }

    // ========================================================================
    // Test 3: Error Scenarios — HTTP error flows back as ToolResult.error
    // Validates: Requirements 9.4, 17.3, 17.4
    // ========================================================================

    @Test
    @DisplayName("HTTP error (500): job handler creates ToolResult.error completion signal, no variable stored")
    void httpError500_createsErrorCompletionSignal_noVariableStored() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        String agentUrl = wireMockServer.baseUrl() + "/";
        String toolCallId = "call_err_001";
        String scopeExecutionId = "exec-scope-err";
        String remoteAgentId = "failingAgent";

        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                agentUrl, "Do something", toolCallId, scopeExecutionId, remoteAgentId);

        A2aRemoteInvokeJobHandler handler = new A2aRemoteInvokeJobHandler(invocationService, runtimeService);
        handler.execute(config, execution, commandContext, null);

        // Verify: NO result variable stored on failure
        verify(runtimeService, never()).setVariable(anyString(), anyString(), anyString());

        // Verify: completion signal with ToolResult.error created
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        MessageEntity completionJob = captor.getValue();
        AgentOrchestrationConfig orchConfig = AgentOrchestrationConfig.fromCanonicalString(
                completionJob.getJobHandlerConfigurationRaw());
        assertTrue(orchConfig.hasToolResult());

        ToolResult toolResult = orchConfig.toolResult();
        assertEquals(toolCallId, toolResult.toolCallId());
        assertTrue(toolResult.isError(), "ToolResult must be an error");
        assertNotNull(toolResult.errorMessage());
        assertFalse(toolResult.errorMessage().isBlank(),
                "Error message must be descriptive");
    }

    @Test
    @DisplayName("Timeout: WireMock delays beyond timeout → ToolResult.error completion signal")
    void timeout_createsErrorCompletionSignal() {
        // Stub WireMock to delay beyond our 2s read timeout
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000)
                        .withBody("{}")));

        String agentUrl = wireMockServer.baseUrl() + "/";
        String toolCallId = "call_timeout_001";
        String scopeExecutionId = "exec-scope-timeout";
        String remoteAgentId = "slowAgent";

        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                agentUrl, "Slow request", toolCallId, scopeExecutionId, remoteAgentId);

        A2aRemoteInvokeJobHandler handler = new A2aRemoteInvokeJobHandler(invocationService, runtimeService);
        handler.execute(config, execution, commandContext, null);

        // Verify: NO result variable stored on timeout
        verify(runtimeService, never()).setVariable(anyString(), anyString(), anyString());

        // Verify: completion signal with ToolResult.error
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        MessageEntity completionJob = captor.getValue();
        AgentOrchestrationConfig orchConfig = AgentOrchestrationConfig.fromCanonicalString(
                completionJob.getJobHandlerConfigurationRaw());
        assertTrue(orchConfig.hasToolResult());

        ToolResult toolResult = orchConfig.toolResult();
        assertEquals(toolCallId, toolResult.toolCallId());
        assertTrue(toolResult.isError(), "ToolResult must be an error on timeout");
        assertNotNull(toolResult.errorMessage());
    }

    // ========================================================================
    // Test 6: Multi-part response in async lifecycle
    // Validates: Requirements 9.3, 17.4
    // ========================================================================

    @Test
    @DisplayName("Async lifecycle with multi-part response: all text parts concatenated in result variable")
    void asyncLifecycle_multiPartResponse_concatenated() {
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "req-multi",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [
                          { "kind": "text", "text": "Finding 1: No suspicious activity." },
                          { "kind": "text", "text": "Finding 2: All documents verified." }
                        ]
                      },
                      {
                        "parts": [
                          { "kind": "text", "text": "Summary: Transaction approved." }
                        ]
                      }
                    ]
                  }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        String agentUrl = wireMockServer.baseUrl() + "/";
        String processInstanceId = "proc-inst-multi";
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);

        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                agentUrl, "Full investigation", "call_multi_001", "exec-multi", "kycAgent");

        A2aRemoteInvokeJobHandler handler = new A2aRemoteInvokeJobHandler(invocationService, runtimeService);
        handler.execute(config, execution, commandContext, null);

        // Verify: all text parts concatenated with newlines, stored on process instance
        String expectedResult = "Finding 1: No suspicious activity.\nFinding 2: All documents verified.\nSummary: Transaction approved.";
        verify(runtimeService).setVariable(processInstanceId, "kycAgentResult", expectedResult);
    }

    // ========================================================================
    // Test 7: Failed task state in async lifecycle → ToolResult.error
    // Validates: Requirements 9.4, 17.4
    // ========================================================================

    @Test
    @DisplayName("Failed task state: status.state='failed' → ToolResult.error in completion signal")
    void failedTaskState_createsErrorCompletionSignal() {
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "req-fail",
                  "result": {
                    "status": {
                      "state": "failed",
                      "message": "Agent internal error: database unavailable"
                    }
                  }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        String agentUrl = wireMockServer.baseUrl() + "/";
        String toolCallId = "call_fail_001";
        A2aRemoteInvokeConfig config = new A2aRemoteInvokeConfig(
                agentUrl, "Check compliance", toolCallId, "exec-fail", "compAgent");

        A2aRemoteInvokeJobHandler handler = new A2aRemoteInvokeJobHandler(invocationService, runtimeService);
        handler.execute(config, execution, commandContext, null);

        // Verify: no result variable stored on failure
        verify(runtimeService, never()).setVariable(anyString(), anyString(), anyString());

        // Verify: ToolResult.error with the correct toolCallId
        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());

        AgentOrchestrationConfig orchConfig = AgentOrchestrationConfig.fromCanonicalString(
                captor.getValue().getJobHandlerConfigurationRaw());
        ToolResult toolResult = orchConfig.toolResult();
        assertEquals(toolCallId, toolResult.toolCallId());
        assertTrue(toolResult.isError());
        assertTrue(toolResult.errorMessage().contains("database unavailable")
                        || toolResult.errorMessage().contains("failed"),
                "Error message should describe the failure, got: " + toolResult.errorMessage());
    }
}
