package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ActivityTypes;
import org.finos.fluxnova.bpm.engine.delegate.ExecutionListener;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
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
        when(element.getTagName()).thenReturn(ActivityTypes.SUB_PROCESS_AD_HOC);
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
    class ListenerRegistration {

        @Test
        void parseSubProcess_withAgentConfig_registersEntryListener() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);
            when(activity.getActivities()).thenReturn(List.of());

            parseListener.parseSubProcess(element, scope, activity);

            verify(activity).addBuiltInListener(PvmEvent.EVENTNAME_START, entryListener);
        }

        @Test
        void parseSubProcess_triggerableChild_getsCompletionListener() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);

            when(childTool.getIncomingTransitions()).thenReturn(Collections.emptyList());
            when(activity.getActivities()).thenReturn(List.of(childTool));

            parseListener.parseSubProcess(element, scope, activity);

            verify(childTool).addBuiltInListener(PvmEvent.EVENTNAME_END, completionListener);
        }

        @Test
        void parseSubProcess_nonTriggerableChild_noCompletionListener() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);

            when(incomingTransition.getSource()).thenReturn(childTool);
            when(childTool.getId()).thenReturn("childTool");
            when(activity.findActivity("childTool")).thenReturn(childTool);
            when(childNonTool.getIncomingTransitions()).thenReturn(List.of(incomingTransition));
            when(activity.getActivities()).thenReturn(List.of(childNonTool));

            parseListener.parseSubProcess(element, scope, activity);

            verify(childNonTool, never()).addBuiltInListener(anyString(),
                    any(ExecutionListener.class));
        }

        @Test
        void parseSubProcess_mixedChildren_onlyTriggerableGetListener() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);

            when(incomingTransition.getSource()).thenReturn(childTool);
            when(childTool.getId()).thenReturn("childTool");
            when(activity.findActivity("childTool")).thenReturn(childTool);
            when(childTool.getIncomingTransitions()).thenReturn(Collections.emptyList());
            when(childNonTool.getIncomingTransitions()).thenReturn(List.of(incomingTransition));
            when(activity.getActivities()).thenReturn(List.of(childTool, childNonTool));

            parseListener.parseSubProcess(element, scope, activity);

            verify(activity).addBuiltInListener(PvmEvent.EVENTNAME_START, entryListener);
            verify(childTool).addBuiltInListener(PvmEvent.EVENTNAME_END, completionListener);
            verify(childNonTool, never()).addBuiltInListener(anyString(),
                    any(ExecutionListener.class));
        }

        @Test
        void parseSubProcess_noChildren_registersOnlyEntryListener() {
            when(element.element("extensionElements")).thenReturn(extensionElements);
            when(extensionElements.elementNS(AgentModelConstants.AGENT_NS, "config"))
                    .thenReturn(agentConfigElement);
            when(activity.getActivities()).thenReturn(Collections.emptyList());

            parseListener.parseSubProcess(element, scope, activity);

            verify(activity).addBuiltInListener(PvmEvent.EVENTNAME_START, entryListener);
            verify(activity, times(1)).addBuiltInListener(anyString(),
                    any(ExecutionListener.class));
        }
    }
}
