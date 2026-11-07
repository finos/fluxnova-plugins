package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.runtime.Execution;
import org.finos.fluxnova.bpm.engine.runtime.Job;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstance;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestApplication.class)
@Import(TestConfig.class)
class AgentOrchestrationIntegrationTest {

    private static final String PROCESS_KEY = "agentProcess";
    private static final String AD_HOC_ID = "agentScope";
    private static final String DELEGATE_CLASS =
            "org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration.StaticOutputDelegate";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private LlmService llmOrchestrationService;

    @Autowired
    private AgentStateManager stateManager;

    private String deploymentId;

    @BeforeEach
    void resetMocks() {
        reset(llmOrchestrationService);
    }

    @AfterEach
    void cleanup() {
        if (deploymentId != null) {
            repositoryService.deleteDeployment(deploymentId, true);
            deploymentId = null;
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 1: Single-turn completion — LLM returns text, no tool calls
    // -----------------------------------------------------------------------

    @Test
    void singleTurnCompletion_noToolCalls_subprocessCompletes() {
        deploy(agentXml("toolA"));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Entry listener creates the first orchestration job
        executeNextJob(processInstance);

        // LLM returned no tool calls — subprocess should have completed
        assertProcessEnded(processInstance);
        verify(llmOrchestrationService, times(1)).call(any(), any(), any(), anyList());
    }

    // -----------------------------------------------------------------------
    // Scenario 2: Single tool call, single turn
    // -----------------------------------------------------------------------

    @Test
    void singleToolCall_completesAfterToolResult() {
        deploy(agentXml("toolA"));

        // First LLM call: request toolA
        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-1", "toolA"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Step 1: Entry job — LLM requests toolA
        executeNextJob(processInstance);

        Execution adHocExec = adHocExecution(processInstance);
        assertThat(adHocExec).isNotNull();
        assertThat(stateManager.isPendingToolCall(adHocExec.getId(), "call-1")).isTrue();

        // The tool activity (service task) runs and completes synchronously.
        // SubprocessToolCompletionListener fires and creates the next orchestration job.

        // Step 2: Tool completion job — feeds result to LLM, LLM returns text
        executeNextJob(processInstance);

        // Verify conversation history was accumulated
        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 3: Multi-turn conversation
    // -----------------------------------------------------------------------

    @Test
    void twoSequentialToolCalls_completesAfterBoth() {
        deploy(agentXml("toolA", "toolB"));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-1", "toolA"))
                .thenReturn(toolCallResponse("call-2", "toolB"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Step 1: Entry — LLM requests toolA
        executeNextJob(processInstance);
        assertProcessNotEnded(processInstance);

        // Step 2: toolA completes — LLM requests toolB
        executeNextJob(processInstance);
        assertProcessNotEnded(processInstance);

        // Step 3: toolB completes — LLM returns text, subprocess completes
        executeNextJob(processInstance);

        verify(llmOrchestrationService, times(3)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 4: Parallel tool calls
    // -----------------------------------------------------------------------

    @Test
    void parallelToolCalls_allCompleteBeforeNextLlmCall() {
        deploy(agentXml("toolA", "toolB"));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(parallelToolCallResponse(new ToolCallRequest("call-1", "toolA"),
                        new ToolCallRequest("call-2", "toolB")))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Step 1: Entry — LLM requests toolA and toolB in parallel
        executeNextJob(processInstance);

        Execution adHocExec = adHocExecution(processInstance);
        assertThat(adHocExec).isNotNull();
        assertThat(stateManager.isPendingToolCall(adHocExec.getId(), "call-1")).isTrue();
        assertThat(stateManager.isPendingToolCall(adHocExec.getId(), "call-2")).isTrue();

        // Both tool activities run. Each completion fires a tool-completion job.
        // The first tool-completion job removes its ID from pending but doesn't
        // trigger the next LLM call (pending still has the other).
        executeNextJob(processInstance);
        assertProcessNotEnded(processInstance);

        // The second tool-completion job clears pending and triggers the next LLM call.
        executeNextJob(processInstance);

        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 5: Tool invocation failure
    // -----------------------------------------------------------------------

    @Test
    void toolInvocationFailure_errorFedBackToLlm() {
        deploy(agentXml("toolA"));

        // LLM requests a tool that doesn't exist in the subprocess
        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-1", "nonExistentTool"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Step 1: Entry — LLM requests nonExistentTool, invocation fails
        executeNextJob(processInstance);

        // The failure should be buffered and a follow-up job scheduled immediately
        // (no pending tools, all failed → scheduleNextStep)

        // Step 2: Follow-up job — error result fed back to LLM, LLM returns text
        executeNextJob(processInstance);

        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 6: Empty tool catalogue — early termination
    // -----------------------------------------------------------------------

    @Test
    void emptyToolCatalogue_terminatesImmediately() {
        deploy(agentXmlNoTools());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Entry job fires — job handler detects empty catalogue and completes
        executeNextJob(processInstance);

        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 7: Inactive execution guard
    // -----------------------------------------------------------------------

    @Test
    void inactiveExecution_orchestrationStepSkipped() {
        deploy(agentXml("toolA"));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Execute the entry job — subprocess completes (text-only response)
        executeNextJob(processInstance);
        assertProcessEnded(processInstance);

        // If there were any stale jobs remaining for this execution, executing
        // them should be a no-op due to the inactive execution guard.
        List<Job> remainingJobs =
                managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
        for (Job job : remainingJobs) {
            managementService.executeJob(job.getId());
        }

        // LLM should only have been called once (the entry step)
        verify(llmOrchestrationService, times(1)).call(any(), any(), any(), anyList());
    }

    // -----------------------------------------------------------------------
    // Scenario 8: Multi-step sequential activity chain (A -> B -> C)
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void sequentialActivityChain_completesOnlyAfterTerminalActivityFinishes() {
        deploy(agentXmlWithBody("""
                <serviceTask id="toolA" name="Tool A" camunda:class="%1$s">
                  <outgoing>flowAB</outgoing>
                </serviceTask>
                <sequenceFlow id="flowAB" sourceRef="toolA" targetRef="stepB"/>
                <serviceTask id="stepB" name="Step B" camunda:class="%1$s">
                  <incoming>flowAB</incoming>
                  <outgoing>flowBC</outgoing>
                </serviceTask>
                <sequenceFlow id="flowBC" sourceRef="stepB" targetRef="stepC"/>
                <serviceTask id="stepC" name="Step C" camunda:class="%1$s">
                  <incoming>flowBC</incoming>
                </serviceTask>
                """.formatted(DELEGATE_CLASS)));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-chain", "toolA"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        // Entry job dispatches toolA, which synchronously runs toolA -> stepB -> stepC
        executeNextJob(processInstance);

        List<String> executed = (List<String>) runtimeService.getVariable(
                processInstance.getId(), "executedActivities");
        assertThat(executed).containsExactly("toolA", "stepB", "stepC");

        // Only a single tool-completion job should have been queued for the entire chain
        List<Job> completionJobs = managementService.createJobQuery()
                .processInstanceId(processInstance.getId()).list();
        assertThat(completionJobs).hasSize(1);

        executeNextJob(processInstance);

        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 9: Parallel branches inside a triggered tool fragment
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void parallelFragmentBranches_completesOnceAllBranchesFinish() {
        deploy(agentXmlWithBody("""
                <serviceTask id="toolA" name="Tool A" camunda:class="%1$s">
                  <outgoing>toFork</outgoing>
                </serviceTask>
                <sequenceFlow id="toFork" sourceRef="toolA" targetRef="forkGw"/>
                <parallelGateway id="forkGw">
                  <incoming>toFork</incoming>
                  <outgoing>toBranch1</outgoing>
                  <outgoing>toBranch2</outgoing>
                </parallelGateway>
                <sequenceFlow id="toBranch1" sourceRef="forkGw" targetRef="branch1"/>
                <sequenceFlow id="toBranch2" sourceRef="forkGw" targetRef="branch2"/>
                <serviceTask id="branch1" name="Branch 1" camunda:class="%1$s">
                  <incoming>toBranch1</incoming>
                </serviceTask>
                <serviceTask id="branch2" name="Branch 2" camunda:class="%1$s">
                  <incoming>toBranch2</incoming>
                </serviceTask>
                """.formatted(DELEGATE_CLASS)));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-parallel-frag", "toolA"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);

        executeNextJob(processInstance);

        List<String> executed = (List<String>) runtimeService.getVariable(
                processInstance.getId(), "executedActivities");
        assertThat(executed).containsExactlyInAnyOrder("toolA", "branch1", "branch2");

        // Exactly one tool completion job should be emitted after the last branch finishes
        List<Job> completionJobs = managementService.createJobQuery()
                .processInstanceId(processInstance.getId()).list();
        assertThat(completionJobs).hasSize(1);

        executeNextJob(processInstance);
        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 10: Conditional gateway path inside a triggered tool fragment
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void conditionalGatewayFragment_completesWhenSelectedTerminalActivityFinishes() {
        deploy(agentXmlWithBody("""
                <serviceTask id="toolA" name="Tool A" camunda:class="%1$s">
                  <outgoing>toGw</outgoing>
                </serviceTask>
                <sequenceFlow id="toGw" sourceRef="toolA" targetRef="condGw"/>
                <exclusiveGateway id="condGw" default="toLow">
                  <incoming>toGw</incoming>
                  <outgoing>toHigh</outgoing>
                  <outgoing>toLow</outgoing>
                </exclusiveGateway>
                <sequenceFlow id="toHigh" sourceRef="condGw" targetRef="highPath">
                  <conditionExpression xsi:type="tFormalExpression">${score >= 700}</conditionExpression>
                </sequenceFlow>
                <sequenceFlow id="toLow" sourceRef="condGw" targetRef="lowPath"/>
                <serviceTask id="highPath" name="High Path" camunda:class="%1$s">
                  <incoming>toHigh</incoming>
                </serviceTask>
                <serviceTask id="lowPath" name="Low Path" camunda:class="%1$s">
                  <incoming>toLow</incoming>
                </serviceTask>
                """.formatted(DELEGATE_CLASS)));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-cond", "toolA"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, Map.of("score", 750));

        executeNextJob(processInstance);

        List<String> executed = (List<String>) runtimeService.getVariable(
                processInstance.getId(), "executedActivities");
        assertThat(executed).containsExactly("toolA", "highPath");

        executeNextJob(processInstance);
        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 11: Bounded loop inside a triggered tool fragment
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void boundedLoopFragment_completesOnlyWhenLoopExitsAndTerminalActivityFinishes() {
        deploy(agentXmlWithBody("""
                <serviceTask id="toolA" name="Tool A" camunda:class="%1$s">
                  <outgoing>toLoopStep</outgoing>
                </serviceTask>
                <sequenceFlow id="toLoopStep" sourceRef="toolA" targetRef="loopStep"/>
                <serviceTask id="loopStep" name="Loop Step" camunda:class="%1$s">
                  <incoming>toLoopStep</incoming>
                  <incoming>loopBack</incoming>
                  <outgoing>toLoopGw</outgoing>
                </serviceTask>
                <sequenceFlow id="toLoopGw" sourceRef="loopStep" targetRef="loopGw"/>
                <exclusiveGateway id="loopGw" default="loopExitFlow">
                  <incoming>toLoopGw</incoming>
                  <outgoing>loopBack</outgoing>
                  <outgoing>loopExitFlow</outgoing>
                </exclusiveGateway>
                <sequenceFlow id="loopBack" sourceRef="loopGw" targetRef="loopStep">
                  <conditionExpression xsi:type="tFormalExpression">${loopIterations < 3}</conditionExpression>
                </sequenceFlow>
                <sequenceFlow id="loopExitFlow" sourceRef="loopGw" targetRef="finalStep"/>
                <serviceTask id="finalStep" name="Final Step" camunda:class="%1$s">
                  <incoming>loopExitFlow</incoming>
                </serviceTask>
                """.formatted(DELEGATE_CLASS)));

        when(llmOrchestrationService.call(any(), any(), any(), anyList()))
                .thenReturn(toolCallResponse("call-loop", "toolA"))
                .thenReturn(doneResponse());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, Map.of("loopIterations", 0));

        executeNextJob(processInstance);

        List<String> executed = (List<String>) runtimeService.getVariable(
                processInstance.getId(), "executedActivities");
        assertThat(executed).containsExactly("toolA", "loopStep", "loopStep", "loopStep", "finalStep");

        List<Job> completionJobs = managementService.createJobQuery()
                .processInstanceId(processInstance.getId()).list();
        assertThat(completionJobs).hasSize(1);

        executeNextJob(processInstance);
        verify(llmOrchestrationService, times(2)).call(any(), any(), any(), anyList());
        assertProcessEnded(processInstance);
    }

    // -----------------------------------------------------------------------
    // Scenario 12: Deploying with auto-complete enabled fails at parse time
    // -----------------------------------------------------------------------

    @Test
    void deployWithAutoCompleteEnabled_throwsParseException() {
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                  xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
                  targetNamespace="http://test">
                  <process id="invalidAutoCompleteProc" isExecutable="true">
                    <startEvent id="start"><outgoing>toAgent</outgoing></startEvent>
                    <sequenceFlow id="toAgent" sourceRef="start" targetRef="agentScope"/>
                    <adHocSubProcess id="agentScope" ordering="Parallel">
                      <extensionElements>
                        <agent:config provider="test" model="test-model"
                          systemPrompt="You are a test agent."/>
                      </extensionElements>
                      <incoming>toAgent</incoming>
                      <outgoing>toEnd</outgoing>
                      <serviceTask id="toolA" name="toolA" camunda:class="%s"/>
                    </adHocSubProcess>
                    <sequenceFlow id="toEnd" sourceRef="agentScope" targetRef="end"/>
                    <endEvent id="end"><incoming>toEnd</incoming></endEvent>
                  </process>
                </definitions>
                """.formatted(DELEGATE_CLASS);

        assertThatThrownBy(() -> repositoryService.createDeployment()
                .addString("invalid-agent.bpmn", invalidXml)
                .deploy())
                .isInstanceOfAny(BpmnParseException.class, ProcessEngineException.class)
                .hasMessageContaining("autoComplete");
    }

    // -----------------------------------------------------------------------
    // Helpers — job execution
    // -----------------------------------------------------------------------

    private void executeNextJob(ProcessInstance processInstance) {
        List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).as("Expected at least one executable job").isNotEmpty();
        managementService.executeJob(jobs.get(0).getId());
    }

    // -----------------------------------------------------------------------
    // Helpers — process assertions
    // -----------------------------------------------------------------------

    private Execution adHocExecution(ProcessInstance processInstance) {
        return runtimeService.createExecutionQuery().processInstanceId(processInstance.getId())
                .activityId(AD_HOC_ID).singleResult();
    }

    private void assertProcessEnded(ProcessInstance processInstance) {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId())
                .singleResult()).as("Process instance should have ended").isNull();
    }

    private void assertProcessNotEnded(ProcessInstance processInstance) {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId())
                .singleResult()).as("Process instance should still be running").isNotNull();
    }

    // -----------------------------------------------------------------------
    // Helpers — LLM response builders
    // -----------------------------------------------------------------------

    private static LlmResponse doneResponse() {
        return new LlmResponse("", List.of(),
                List.of(ConversationEntry.assistant("", List.of())));
    }

    private static LlmResponse toolCallResponse(String toolCallId, String toolId) {
        List<ToolCallRequest> toolCalls = List.of(new ToolCallRequest(toolCallId, toolId));
        return new LlmResponse(null, toolCalls,
                List.of(ConversationEntry.assistant(null, toolCalls)));
    }

    private static LlmResponse parallelToolCallResponse(ToolCallRequest... requests) {
        List<ToolCallRequest> toolCalls = List.of(requests);
        return new LlmResponse(null, toolCalls,
                List.of(ConversationEntry.assistant(null, toolCalls)));
    }

    // -----------------------------------------------------------------------
    // Helpers — BPMN XML builders
    // -----------------------------------------------------------------------

    private void deploy(String bpmnXml) {
        deploymentId = repositoryService.createDeployment().addString("agent-process.bpmn", bpmnXml)
                .deploy().getId();
    }

    private static String agentXml(String... toolActivityIds) {
        StringBuilder tools = new StringBuilder();
        for (String toolId : toolActivityIds) {
            tools.append("""
                    <serviceTask id="%s" name="%s" camunda:class="%s"/>
                    """.formatted(toolId, toolId, DELEGATE_CLASS));
        }

        return agentXmlWithBody(tools.toString());
    }

    private static String agentXmlWithBody(String bodyXml) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                  xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
                  targetNamespace="http://test">
                  <process id="%s" isExecutable="true">
                    <startEvent id="start"><outgoing>toAgent</outgoing></startEvent>
                    <sequenceFlow id="toAgent" sourceRef="start" targetRef="%s"/>
                    <adHocSubProcess id="%s" ordering="Parallel" camunda:autoComplete="false">
                      <extensionElements>
                        <agent:config provider="test" model="test-model"
                          systemPrompt="You are a test agent."/>
                      </extensionElements>
                      <incoming>toAgent</incoming>
                      <outgoing>toEnd</outgoing>
                %s
                    </adHocSubProcess>
                    <sequenceFlow id="toEnd" sourceRef="%s" targetRef="end"/>
                    <endEvent id="end"><incoming>toEnd</incoming></endEvent>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY, AD_HOC_ID, AD_HOC_ID, bodyXml, AD_HOC_ID);
    }

    private static String agentXmlNoTools() {
        return agentXml();
    }
}
