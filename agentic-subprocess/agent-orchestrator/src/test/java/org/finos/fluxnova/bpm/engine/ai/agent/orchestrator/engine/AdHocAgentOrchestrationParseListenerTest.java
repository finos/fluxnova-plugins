package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ActivityTypes;
import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.delegate.ExecutionListener;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.PvmEvent;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.TransitionImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdHocAgentOrchestrationParseListenerTest {

    @Mock
    private AgentSubprocessEntryListener entryListener;
    @Mock
    private SubprocessToolCompletionListener completionListener;
    @Mock
    private Element element;
    @Mock
    private Element extensionElements;
    @Mock
    private Element agentConfigElement;
    @Mock
    private ScopeImpl scope;
    @Mock
    private ActivityImpl activity;
    @Mock
    private ActivityImpl childTool;
    @Mock
    private ActivityImpl childNonTool;
    @Mock
    private TransitionImpl incomingTransition;

    private AdHocAgentOrchestrationParseListener parseListener;

    @BeforeEach
    void setUp() {
        parseListener = new AdHocAgentOrchestrationParseListener(entryListener, completionListener);
        lenient().when(element.getTagName()).thenReturn(ActivityTypes.SUB_PROCESS_AD_HOC);
    }

    @Nested
    class NoAgentConfig {

        @Test
        void parseSubProcess_noExtensionElements_doesNothing() {
            when(element.element("extensionElements")).thenReturn(null);

            parseListener.parseSubProcess(element, scope, activity);

            verify(activity, never()).addBuiltInListener(anyString(), any(ExecutionListener.class));
        }

        @Test
        void parseSubProcess_noAgentConfig_doesNothing() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(null);

            parseListener.parseSubProcess(element, scope, activity);

            verify(activity, never()).addBuiltInListener(anyString(), any(ExecutionListener.class));
        }
    }

    @Nested
    class AutoCompleteValidation {

        @Test
        void parseSubProcess_whenAutoCompletePropertyTrue_throwsBpmnParseException() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);
            when(activity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_AUTO_COMPLETE))
                    .thenReturn(Boolean.TRUE);

            assertThrows(BpmnParseException.class,
                    () -> parseListener.parseSubProcess(element, scope, activity));
        }

        @Test
        void parseSubProcess_whenAutoCompleteAttributeTrue_throwsBpmnParseException() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);
            when(element.attribute("autoComplete")).thenReturn("true");

            assertThrows(BpmnParseException.class,
                    () -> parseListener.parseSubProcess(element, scope, activity));
        }
    }

    @Nested
    class ListenerAndBehaviorRegistration {

        @Test
        void parseSubProcess_withAgentConfig_registersEntryListenerAndAgentAdHocBehavior() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);
            when(activity.getActivities()).thenReturn(List.of());

            parseListener.parseSubProcess(element, scope, activity);

            verify(activity).addBuiltInListener(PvmEvent.EVENTNAME_START, entryListener);
            ArgumentCaptor<AgentAdHocSubProcessActivityBehavior> behaviorCaptor =
                    ArgumentCaptor.forClass(AgentAdHocSubProcessActivityBehavior.class);
            verify(activity).setActivityBehavior(behaviorCaptor.capture());
            assertInstanceOf(AgentAdHocSubProcessActivityBehavior.class, behaviorCaptor.getValue());
        }

        @Test
        void parseSubProcess_doesNotAttachCompletionListenerToChildEndEvent() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);

            when(childTool.getProperty("type")).thenReturn(ActivityTypes.TASK_SERVICE);
            when(childTool.getId()).thenReturn("childTool");
            when(childTool.getIncomingTransitions()).thenReturn(Collections.emptyList());
            when(activity.getActivities()).thenReturn(List.of(childTool));

            parseListener.parseSubProcess(element, scope, activity);

            verify(childTool, never()).addBuiltInListener(eq(PvmEvent.EVENTNAME_END), any());
            verify(activity).setActivityBehavior(any(AgentAdHocSubProcessActivityBehavior.class));
        }

        @Test
        void parseSubProcess_nonTriggerableChild_noListenersRegistered() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);

            when(childNonTool.getProperty("type")).thenReturn(ActivityTypes.TASK_SERVICE);
            when(incomingTransition.getSource()).thenReturn(childTool);
            when(childTool.getId()).thenReturn("childTool");
            when(activity.findActivity("childTool")).thenReturn(childTool);
            when(childNonTool.getIncomingTransitions()).thenReturn(List.of(incomingTransition));
            when(activity.getActivities()).thenReturn(List.of(childNonTool));

            parseListener.parseSubProcess(element, scope, activity);

            verify(childNonTool, never()).addBuiltInListener(anyString(),
                    any(ExecutionListener.class));
        }
    }

    @Nested
    class FragmentBehaviorCompletion {

        @Test
        void concurrentChildExecutionEnded_notifiesCompletionWhenLastFragmentBranchEnds() {
            AgentAdHocSubProcessActivityBehavior behavior =
                    new AgentAdHocSubProcessActivityBehavior(
                            completionListener,
                            Map.of("toolA", Set.of("toolA", "stepB")),
                            Map.of("toolA", "toolA", "stepB", "toolA"));

            ExecutionEntity scopeExec = mock(ExecutionEntity.class);
            ExecutionEntity endedExec = mock(ExecutionEntity.class);
            ActivityImpl stepB = mock(ActivityImpl.class);
            when(stepB.getId()).thenReturn("stepB");
            when(endedExec.getActivity()).thenReturn(stepB);
            when(endedExec.getVariableLocal("_agentToolCallId")).thenReturn("call-1");
            when(scopeExec.getExecutions()).thenReturn(List.of(endedExec));

            behavior.concurrentChildExecutionEnded(scopeExec, endedExec);

            verify(completionListener).notify(endedExec);
        }

        @Test
        void concurrentChildExecutionEnded_waitsWhenParallelSiblingBranchStillActive() {
            AgentAdHocSubProcessActivityBehavior behavior =
                    new AgentAdHocSubProcessActivityBehavior(
                            completionListener,
                            Map.of("toolA", Set.of("toolA", "branch1", "branch2")),
                            Map.of("toolA", "toolA", "branch1", "toolA", "branch2", "toolA"));

            ExecutionEntity scopeExec = mock(ExecutionEntity.class);
            ExecutionEntity branch1Exec = mock(ExecutionEntity.class);
            ExecutionEntity branch2Exec = mock(ExecutionEntity.class);
            ActivityImpl branch1 = mock(ActivityImpl.class);
            ActivityImpl branch2 = mock(ActivityImpl.class);
            when(branch1.getId()).thenReturn("branch1");
            when(branch2.getId()).thenReturn("branch2");
            when(branch1Exec.getActivity()).thenReturn(branch1);
            when(branch1Exec.getVariableLocal("_agentToolCallId")).thenReturn("call-1");
            when(branch2Exec.getActivity()).thenReturn(branch2);
            when(branch2Exec.isActive()).thenReturn(true);
            when(branch2Exec.isEnded()).thenReturn(false);
            when(scopeExec.getExecutions()).thenReturn(List.of(branch1Exec, branch2Exec));

            behavior.concurrentChildExecutionEnded(scopeExec, branch1Exec);

            verify(completionListener, never()).notify(any());
        }
    }
}
