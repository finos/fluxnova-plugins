package org.finos.fluxnova.bpm.engine.ai.a2a.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.finos.fluxnova.bpm.engine.ai.a2a.engine.A2aRemoteCallActivityBehaviour;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.delegate.BpmnError;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.el.Expression;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WireMock-based integration tests for standalone A2A remote call mode.
 * <p>
 * Tests the components integrated together:
 * <ul>
 *   <li>{@link A2aInvocationService} with a real {@link RestClient} pointing at WireMock</li>
 *   <li>{@link A2aRemoteCallActivityBehaviour} with the real invocation service</li>
 *   <li>Mocked {@code ActivityExecution} and {@code ExpressionManager} to simulate engine context</li>
 *   <li>WireMock stubs to simulate various A2A agent responses</li>
 * </ul>
 *
 * <p>Validates: Requirements 6.1, 6.2, 6.4, 6.6, 6.7, 13.4, 15.4, 17.1, 17.2, 17.4</p>
 */
class A2aStandaloneIntegrationTest {

    private static WireMockServer wireMockServer;

    private A2aInvocationService invocationService;
    private A2aRemoteCallActivityBehaviour behaviour;

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
        behaviour = new A2aRemoteCallActivityBehaviour(invocationService);
    }

    // ========================================================================
    // Test 1: Successful standalone service task execution
    // Validates: Requirements 6.1, 6.2, 6.4
    // ========================================================================

    @Test
    @DisplayName("Success: completed response → stores {activityId}Result variable and leaves")
    void successfulInvocation_storesResultVariable_andLeaves() throws Exception {
        // Stub WireMock to return a completed A2A response
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "test-id",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [
                          { "kind": "text", "text": "Investigation complete. No issues found." }
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
        String activityId = "callAmlAgent";

        // Setup mocks for engine context
        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig(
                "${agentUrl}", "${prompt}", "AML Agent", "Investigates AML alerts");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn(activityId);
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${agentUrl}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Investigate transaction TX-123");

        // Execute with mocked static Context
        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            behaviour.execute(execution);
        }

        // Verify: result stored as {activityId}Result
        verify(execution).setVariable("callAmlAgentResult", "Investigation complete. No issues found.");

        // Verify: WireMock received the correct JSON-RPC request
        wireMockServer.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                .withRequestBody(matchingJsonPath("$.method", equalTo("message/send")))
                .withRequestBody(matchingJsonPath("$.params.message.role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.params.message.parts[0].kind", equalTo("text")))
                .withRequestBody(matchingJsonPath("$.params.message.parts[0].text",
                        equalTo("Investigate transaction TX-123"))));
    }

    // ========================================================================
    // Test 2: HTTP error (500) → BpmnError thrown
    // Validates: Requirements 6.6, 13.4
    // ========================================================================

    @Test
    @DisplayName("HTTP error (500): WireMock returns 500 → BpmnError('a2a-remote-call-error') thrown")
    void httpError500_throwsBpmnError() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        String agentUrl = wireMockServer.baseUrl() + "/";

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Do something");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            assertEquals("a2a-remote-call-error", thrown.getErrorCode());
            assertTrue(thrown.getMessage().contains("HTTP error"),
                    "Error message should mention HTTP error, got: " + thrown.getMessage());
        }

        // Verify: no variable stored on failure
        verify(execution, never()).setVariable(any(), any());
    }

    // ========================================================================
    // Test 3: Timeout → BpmnError thrown
    // Validates: Requirements 6.6, 17.1, 17.2
    // ========================================================================

    @Test
    @DisplayName("Timeout: WireMock delays beyond timeout → BpmnError thrown")
    void timeout_throwsBpmnError() {
        // Stub WireMock to delay response beyond our 2s timeout
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000)  // 5 seconds delay, exceeds 2s read timeout
                        .withBody("{}")));

        String agentUrl = wireMockServer.baseUrl() + "/";

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Do something");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            assertEquals("a2a-remote-call-error", thrown.getErrorCode());
            // Error message should indicate timeout/connection issue
            assertNotNull(thrown.getMessage());
            assertFalse(thrown.getMessage().isBlank());
        }

        // Verify: no variable stored on failure
        verify(execution, never()).setVariable(any(), any());
    }

    // ========================================================================
    // Test 4: Malformed response → BpmnError thrown
    // Validates: Requirements 6.6, 13.4
    // ========================================================================

    @Test
    @DisplayName("Malformed response: WireMock returns invalid JSON → BpmnError thrown")
    void malformedResponse_throwsBpmnError() {
        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("this is not valid JSON at all {{{}")));

        String agentUrl = wireMockServer.baseUrl() + "/";

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Do something");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            assertEquals("a2a-remote-call-error", thrown.getErrorCode());
            assertTrue(thrown.getMessage().contains("Malformed") || thrown.getMessage().contains("malformed")
                            || thrown.getMessage().contains("JSON") || thrown.getMessage().contains("parse"),
                    "Error message should indicate JSON parsing failure, got: " + thrown.getMessage());
        }

        // Verify: no variable stored on failure
        verify(execution, never()).setVariable(any(), any());
    }

    // ========================================================================
    // Test 5: Failed task state → BpmnError thrown
    // Validates: Requirements 6.6, 13.4
    // ========================================================================

    @Test
    @DisplayName("Failed task state: WireMock returns status.state='failed' → BpmnError thrown")
    void failedTaskState_throwsBpmnError() {
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "test-id",
                  "result": {
                    "status": {
                      "state": "failed",
                      "message": "Agent could not complete the task due to internal error"
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

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Do something");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            assertEquals("a2a-remote-call-error", thrown.getErrorCode());
            assertTrue(thrown.getMessage().contains("could not complete") || thrown.getMessage().contains("internal error"),
                    "Error message should contain failure details, got: " + thrown.getMessage());
        }

        // Verify: no variable stored on failure
        verify(execution, never()).setVariable(any(), any());
    }

    // ========================================================================
    // Test 6: EL expression resolution
    // Validates: Requirements 6.1, 6.2, 15.4
    // ========================================================================

    @Test
    @DisplayName("EL expression resolution: url and prompt resolved from execution variables")
    void elExpressionResolution_resolvesUrlAndPrompt() throws Exception {
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "test-id",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [
                          { "kind": "text", "text": "Done." }
                        ]
                      }
                    ]
                  }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/a2a"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        String resolvedUrl = wireMockServer.baseUrl() + "/a2a";
        String resolvedPrompt = "Investigate transaction TX-456";

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        // Use EL expressions that reference execution variables
        String urlElExpression = "${environment.agentBaseUrl + '/a2a'}";
        String promptElExpression = "${'Investigate transaction ' + transactionId}";

        A2aRemoteCallConfig config = new A2aRemoteCallConfig(
                urlElExpression, promptElExpression, "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("investigateTask");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression(urlElExpression)).thenReturn(urlExpr);
        when(expressionManager.createExpression(promptElExpression)).thenReturn(promptExpr);
        // The EL engine resolves the expressions to concrete values
        when(urlExpr.getValue(execution)).thenReturn(resolvedUrl);
        when(promptExpr.getValue(execution)).thenReturn(resolvedPrompt);

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            behaviour.execute(execution);
        }

        // Verify: expression manager was asked to create expressions from the raw EL strings
        verify(expressionManager).createExpression(urlElExpression);
        verify(expressionManager).createExpression(promptElExpression);

        // Verify: the expressions were evaluated against the execution
        verify(urlExpr).getValue(execution);
        verify(promptExpr).getValue(execution);

        // Verify: result stored correctly
        verify(execution).setVariable("investigateTaskResult", "Done.");

        // Verify: WireMock received the resolved prompt (not the EL expression)
        wireMockServer.verify(postRequestedFor(urlEqualTo("/a2a"))
                .withRequestBody(matchingJsonPath("$.params.message.parts[0].text",
                        equalTo("Investigate transaction TX-456"))));
    }

    // ========================================================================
    // Test 7: BpmnError catchable by error boundary event (simulated)
    // Validates: Requirements 6.7, 13.4
    // ========================================================================

    @Test
    @DisplayName("BpmnError catchable by error boundary event: error has correct code")
    void bpmnError_isCatchableByBoundaryEvent() {
        // Simulate an unreachable agent (connection refused scenario)
        // Use a port that nothing is listening on
        String unreachableUrl = "http://localhost:1/unreachable";

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("remoteTask");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(unreachableUrl);
        when(promptExpr.getValue(execution)).thenReturn("Prompt text");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            // The error code is "a2a-remote-call-error" — this is what a boundary event would catch
            assertEquals("a2a-remote-call-error", thrown.getErrorCode(),
                    "BpmnError error code must be 'a2a-remote-call-error' so boundary events can catch it");

            // The error message should be descriptive
            assertNotNull(thrown.getMessage());
            assertFalse(thrown.getMessage().isBlank(),
                    "BpmnError must have a non-blank descriptive message");
        }
    }

    // ========================================================================
    // Test 8: Multi-part response concatenation
    // Validates: Requirement 6.4 (response stored correctly)
    // ========================================================================

    @Test
    @DisplayName("Success with multi-part response: text parts concatenated with newlines")
    void multiPartResponse_concatenatedWithNewlines() throws Exception {
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "test-id",
                  "result": {
                    "status": { "state": "completed" },
                    "artifacts": [
                      {
                        "parts": [
                          { "kind": "text", "text": "Part 1: Analysis complete." },
                          { "kind": "text", "text": "Part 2: No anomalies detected." }
                        ]
                      },
                      {
                        "parts": [
                          { "kind": "text", "text": "Part 3: Recommendation: approve." }
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

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("multiPartTask");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Analyze data");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            behaviour.execute(execution);
        }

        // Verify: all text parts concatenated with newlines
        String expectedResult = "Part 1: Analysis complete.\nPart 2: No anomalies detected.\nPart 3: Recommendation: approve.";
        verify(execution).setVariable("multiPartTaskResult", expectedResult);
    }

    // ========================================================================
    // Test 9: Unsupported state → BpmnError thrown
    // Validates: Requirement 6.6 (any non-completed/failed state is error)
    // ========================================================================

    @Test
    @DisplayName("Unsupported state: status.state='working' → BpmnError thrown")
    void unsupportedState_throwsBpmnError() {
        String responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": "test-id",
                  "result": {
                    "status": { "state": "working" }
                  }
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        String agentUrl = wireMockServer.baseUrl() + "/";

        ExecutionEntity execution = mock(ExecutionEntity.class);
        ActivityImpl activity = mock(ActivityImpl.class);
        ProcessEngineConfigurationImpl engineConfig = mock(ProcessEngineConfigurationImpl.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        Expression urlExpr = mock(Expression.class);
        Expression promptExpr = mock(Expression.class);

        A2aRemoteCallConfig config = new A2aRemoteCallConfig("${url}", "${prompt}", "Agent", "desc");

        when(execution.getActivity()).thenReturn(activity);
        when(activity.getId()).thenReturn("task1");
        when(activity.getProperty("a2aRemoteCallConfig")).thenReturn(config);
        when(engineConfig.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.createExpression("${url}")).thenReturn(urlExpr);
        when(expressionManager.createExpression("${prompt}")).thenReturn(promptExpr);
        when(urlExpr.getValue(execution)).thenReturn(agentUrl);
        when(promptExpr.getValue(execution)).thenReturn("Do something");

        try (MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class)) {
            contextMock.when(Context::getProcessEngineConfiguration).thenReturn(engineConfig);

            BpmnError thrown = assertThrows(BpmnError.class, () -> behaviour.execute(execution));

            assertEquals("a2a-remote-call-error", thrown.getErrorCode());
            assertTrue(thrown.getMessage().contains("Unsupported task state") || thrown.getMessage().contains("working"),
                    "Error message should mention unsupported state, got: " + thrown.getMessage());
        }

        verify(execution, never()).setVariable(any(), any());
    }
}
