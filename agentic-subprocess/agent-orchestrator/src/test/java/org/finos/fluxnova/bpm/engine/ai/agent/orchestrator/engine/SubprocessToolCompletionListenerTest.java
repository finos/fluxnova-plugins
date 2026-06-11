package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubprocessToolCompletionListenerTest {

    private static final String SCOPE_EXECUTION_ID = "scope-exec-001";
    private static final String ACTIVITY_ID = "creditScoreCheck";
    private static final String TOOL_CALL_ID = "tc-001";

    @Mock
    private ExecutionEntity execution;

    @Mock
    private ExecutionEntity parentExecution;

    @Mock
    private CommandContext commandContext;

    @Mock
    private JobManager jobManager;

    private SubprocessToolCompletionListener listener;

    @BeforeEach
    void setUp() {
        listener = new SubprocessToolCompletionListener();
    }

    private void stubToolCallExecution() {
        when(execution.getVariable("_agentToolCallId")).thenReturn(TOOL_CALL_ID);
        when(execution.getCurrentActivityId()).thenReturn(ACTIVITY_ID);
        when(execution.getParent()).thenReturn(parentExecution);
        when(parentExecution.isScope()).thenReturn(true);
        when(parentExecution.getId()).thenReturn(SCOPE_EXECUTION_ID);
    }

    @Nested
    class NoToolCallId {

        @Test
        void notify_whenNoToolCallId_skips() {
            when(execution.getVariable("_agentToolCallId")).thenReturn(null);

            listener.notify(execution);

            verify(execution).getVariable("_agentToolCallId");
            verify(execution).getId();
            verifyNoMoreInteractions(execution);
        }
    }

    @Nested
    class ToolCompletion {

        @Test
        void notify_createsJobWithCorrectHandlerTypeConfigAndParentExecution() {
            stubToolCallExecution();
            when(commandContext.getJobManager()).thenReturn(jobManager);

            try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
                contextMock.when(Context::getCommandContext).thenReturn(commandContext);
                listener.notify(execution);
            }

            ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
            verify(jobManager).insertAndHintJobExecutor(captor.capture());

            MessageEntity job = captor.getValue();
            assertEquals(AgentOrchestrationJobHandler.TYPE, job.getJobHandlerType());
            assertEquals(parentExecution, job.getExecution());

            AgentOrchestrationConfig config = AgentOrchestrationConfig
                    .fromCanonicalString(job.getJobHandlerConfigurationRaw());
            assertTrue(config.hasToolResult());
            assertEquals(TOOL_CALL_ID, config.toolResult().toolCallId());
            assertEquals(ACTIVITY_ID, config.toolResult().toolElementId());
            assertNull(config.toolResult().errorMessage());
        }
    }
}
