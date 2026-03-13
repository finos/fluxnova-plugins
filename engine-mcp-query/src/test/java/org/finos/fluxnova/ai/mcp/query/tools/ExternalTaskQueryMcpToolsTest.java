package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.ExternalTaskResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.ExternalTaskQueryDto;
import org.finos.fluxnova.bpm.engine.ExternalTaskService;
import org.finos.fluxnova.bpm.engine.externaltask.ExternalTask;
import org.finos.fluxnova.bpm.engine.externaltask.ExternalTaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalTaskQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ExternalTaskQueryDto emptyQuery() {
        try {
            return MAPPER.readValue("{}", ExternalTaskQueryDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private ExternalTaskService externalTaskService;

    private ExternalTaskQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new ExternalTaskQueryMcpTools(externalTaskService, 200);
    }

    @Nested
    class QueryExternalTasks {

        @Mock(answer = Answers.RETURNS_SELF)
        private ExternalTaskQuery query;

        @BeforeEach
        void setUp() {
            when(externalTaskService.createExternalTaskQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<ExternalTaskResultDto> result = tools.queryExternalTasks(emptyQuery(), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).externalTaskId(any());
            verify(query, never()).topicName(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());
            Date lockBefore = new Date();
            Date lockAfter = new Date();

            ExternalTaskQueryDto dto = new ExternalTaskQueryDto(
                    "et-1", List.of("et-1", "et-2"),
                    "worker-1", lockBefore, lockAfter,
                    "orderProcessing",
                    true, true,
                    "exec-1", "pi-1", List.of("pi-1", "pi-2"),
                    "pd-1", "serviceTask1", List.of("serviceTask1", "serviceTask2"),
                    5L, 100L,
                    true, true,
                    true, true,
                    List.of("t1", "t2")
            );

            tools.queryExternalTasks(dto, null);

            verify(query).externalTaskId("et-1");
            verify(query).externalTaskIdIn(Set.of("et-1", "et-2"));
            verify(query).workerId("worker-1");
            verify(query).lockExpirationBefore(lockBefore);
            verify(query).lockExpirationAfter(lockAfter);
            verify(query).topicName("orderProcessing");
            verify(query).locked();
            verify(query).notLocked();
            verify(query).executionId("exec-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processInstanceIdIn("pi-1", "pi-2");
            verify(query).processDefinitionId("pd-1");
            verify(query).activityId("serviceTask1");
            verify(query).activityIdIn("serviceTask1", "serviceTask2");
            verify(query).priorityHigherThanOrEquals(5L);
            verify(query).priorityLowerThanOrEquals(100L);
            verify(query).suspended();
            verify(query).active();
            verify(query).withRetriesLeft();
            verify(query).noRetriesLeft();
            verify(query).tenantIdIn("t1", "t2");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            ExternalTaskQueryDto dto = new ExternalTaskQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null,
                    false, null,
                    null, null, Collections.emptyList(),
                    null, null, Collections.emptyList(),
                    null, null,
                    null, false,
                    null, false,
                    Collections.emptyList()
            );

            tools.queryExternalTasks(dto, null);

            verify(query, never()).locked();
            verify(query, never()).notLocked();
            verify(query, never()).suspended();
            verify(query, never()).active();
            verify(query, never()).withRetriesLeft();
            verify(query, never()).noRetriesLeft();
            verify(query, never()).externalTaskIdIn(any());
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            ExternalTask et = mock(ExternalTask.class);
            Date lockExp = new Date();
            Date createTime = new Date();
            when(et.getId()).thenReturn("et-1");
            when(et.getTopicName()).thenReturn("orderProcessing");
            when(et.getWorkerId()).thenReturn("worker-1");
            when(et.getLockExpirationTime()).thenReturn(lockExp);
            when(et.getCreateTime()).thenReturn(createTime);
            when(et.getProcessInstanceId()).thenReturn("pi-1");
            when(et.getExecutionId()).thenReturn("exec-1");
            when(et.getActivityId()).thenReturn("serviceTask1");
            when(et.getActivityInstanceId()).thenReturn("ai-1");
            when(et.getProcessDefinitionId()).thenReturn("pd-1");
            when(et.getProcessDefinitionKey()).thenReturn("orderProcess");
            when(et.getProcessDefinitionVersionTag()).thenReturn("v1");
            when(et.getRetries()).thenReturn(3);
            when(et.getErrorMessage()).thenReturn("Timeout");
            when(et.isSuspended()).thenReturn(false);
            when(et.getTenantId()).thenReturn("t1");
            when(et.getPriority()).thenReturn(10L);
            when(et.getBusinessKey()).thenReturn("order-123");
            when(query.list()).thenReturn(List.of(et));

            List<ExternalTaskResultDto> result = tools.queryExternalTasks(emptyQuery(), null);

            assertEquals(1, result.size());
            ExternalTaskResultDto r = result.getFirst();
            assertEquals("et-1", r.id());
            assertEquals("orderProcessing", r.topicName());
            assertEquals("worker-1", r.workerId());
            assertEquals(lockExp, r.lockExpirationTime());
            assertEquals(createTime, r.createTime());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals("exec-1", r.executionId());
            assertEquals("serviceTask1", r.activityId());
            assertEquals("ai-1", r.activityInstanceId());
            assertEquals("pd-1", r.processDefinitionId());
            assertEquals("orderProcess", r.processDefinitionKey());
            assertEquals("v1", r.processDefinitionVersionTag());
            assertEquals(3, r.retries());
            assertEquals("Timeout", r.errorMessage());
            assertFalse(r.suspended());
            assertEquals("t1", r.tenantId());
            assertEquals(10L, r.priority());
            assertEquals("order-123", r.businessKey());
        }
    }
}
