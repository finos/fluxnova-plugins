package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.AgentOrchestrationConfig;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.JobManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.MessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentSubprocessEntryListenerTest {

    @Mock
    private ExecutionEntity execution;

    @Mock
    private CommandContext commandContext;

    @Mock
    private JobManager jobManager;

    private AgentSubprocessEntryListener listener;

    @BeforeEach
    void setUp() {
        listener = new AgentSubprocessEntryListener();
    }

    @Test
    void notify_createsJobWithCorrectConfiguration() {
        when(commandContext.getJobManager()).thenReturn(jobManager);
 
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(Context::getCommandContext).thenReturn(commandContext);
            listener.notify(execution);
        }

        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(jobManager).insertAndHintJobExecutor(captor.capture());
        MessageEntity job = captor.getValue();

        assertEquals(AgentOrchestrationJobHandler.TYPE, job.getJobHandlerType());
        assertEquals(execution, job.getExecution());
        assertFalse(AgentOrchestrationConfig
                .fromCanonicalString(job.getJobHandlerConfigurationRaw()).hasToolResult());
    }
}
