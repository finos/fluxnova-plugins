package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.batch.history.HistoricBatch;
import org.finos.fluxnova.bpm.engine.batch.history.HistoricBatchQuery;
import org.finos.fluxnova.bpm.engine.history.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryQueryMcpToolsTest {

    private static final int DEFAULT_MAX = 200;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T empty(Class<T> type) {
        try {
            return MAPPER.readValue("{}", type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private HistoryService historyService;

    private HistoryQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new HistoryQueryMcpTools(historyService, DEFAULT_MAX);
    }

    // -------------------------------------------------------------------------
    // queryHistoricProcessInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricProcessInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricProcessInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricProcessInstanceResultDto> result =
                    tools.queryHistoricProcessInstances(empty(HistoricProcessInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).processInstanceId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date startedBefore = new Date();
            Date startedAfter = new Date();

            HistoricProcessInstanceQueryDto dto = new HistoricProcessInstanceQueryDto(
                    "pi-1", List.of("pi-1", "pi-2"), List.of("pi-3"),
                    "pd-1", "orderProcess", List.of("orderProcess", "invoiceProcess"),
                    List.of("otherKey"), "Order Process", "Order%",
                    "BK-001", List.of("BK-001", "BK-002"), "BK%",
                    true, false, true, false, "open",
                    "failedJob", "Job failed", "Job failed%",
                    true, "case-1", "super-pi-1", "sub-pi-1",
                    "super-ci-1", "sub-ci-1", true,
                    startedBefore, startedAfter, null, null,
                    "user1", true, false, false, false, false,
                    List.of("t1"), true, null
            );

            tools.queryHistoricProcessInstances(dto, null);

            verify(query).processInstanceId("pi-1");
            verify(query).withIncidents();
            verify(query).incidentStatus("open");
            verify(query).incidentType("failedJob");
            verify(query).incidentMessage("Job failed");
            verify(query).incidentMessageLike("Job failed%");
            verify(query).withJobsRetrying();
            verify(query).rootProcessInstances();
            verify(query).startedBefore(startedBefore);
            verify(query).startedAfter(startedAfter);
            verify(query).startedBy("user1");
            verify(query).active();
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricProcessInstanceQueryDto dto = new HistoricProcessInstanceQueryDto(
                    null, null, null,
                    null, null, null,
                    null, null, null,
                    null, null, null,
                    false, null, false, null, null,
                    null, null, null,
                    false, null, null, null,
                    null, null, false,
                    null, null, null, null,
                    null, false, null, null, null, null,
                    null, false, null
            );

            tools.queryHistoricProcessInstances(dto, null);

            verify(query, never()).finished();
            verify(query, never()).unfinished();
            verify(query, never()).withIncidents();
            verify(query, never()).withRootIncidents();
            verify(query, never()).withJobsRetrying();
            verify(query, never()).rootProcessInstances();
            verify(query, never()).active();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricProcessInstance hpi = mock(HistoricProcessInstance.class);
            Date startTime = new Date();
            when(hpi.getId()).thenReturn("pi-1");
            when(hpi.getBusinessKey()).thenReturn("BK-001");
            when(hpi.getProcessDefinitionKey()).thenReturn("orderProcess");
            when(hpi.getProcessDefinitionId()).thenReturn("orderProcess:1:abcd");
            when(hpi.getStartTime()).thenReturn(startTime);
            when(hpi.getEndTime()).thenReturn(null);
            when(hpi.getTenantId()).thenReturn("t1");
            when(hpi.getState()).thenReturn("ACTIVE");
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hpi));

            List<HistoricProcessInstanceResultDto> result =
                    tools.queryHistoricProcessInstances(empty(HistoricProcessInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricProcessInstanceResultDto r = result.getFirst();
            assertEquals("pi-1", r.id());
            assertEquals("BK-001", r.businessKey());
            assertEquals("orderProcess", r.processDefinitionKey());
            assertEquals(startTime, r.startTime());
            assertNull(r.endTime());
            assertEquals("t1", r.tenantId());
            assertEquals("ACTIVE", r.state());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricActivityInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricActivityInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricActivityInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricActivityInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricActivityInstanceResultDto> result =
                    tools.queryHistoricActivityInstances(empty(HistoricActivityInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).activityInstanceId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date startedBefore = new Date();

            HistoricActivityInstanceQueryDto dto = new HistoricActivityInstanceQueryDto(
                    "ai-1", "pi-1", "pd-1", "exec-1",
                    "serviceTask1", "My Service Task", "My Service%",
                    "serviceTask", "user1",
                    true, false, false, false,
                    startedBefore, null, null, null,
                    List.of("t1"), true, null
            );

            tools.queryHistoricActivityInstances(dto, null);

            verify(query).activityInstanceId("ai-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processDefinitionId("pd-1");
            verify(query).executionId("exec-1");
            verify(query).activityId("serviceTask1");
            verify(query).activityName("My Service Task");
            verify(query).activityNameLike("My Service%");
            verify(query).activityType("serviceTask");
            verify(query).taskAssignee("user1");
            verify(query).finished();
            verify(query).startedBefore(startedBefore);
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricActivityInstanceQueryDto dto = new HistoricActivityInstanceQueryDto(
                    null, null, null, null,
                    null, null, null, null, null,
                    false, null, false, null,
                    null, null, null, null,
                    null, false, null
            );

            tools.queryHistoricActivityInstances(dto, null);

            verify(query, never()).finished();
            verify(query, never()).unfinished();
            verify(query, never()).completeScope();
            verify(query, never()).canceled();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricActivityInstance hai = mock(HistoricActivityInstance.class);
            Date startTime = new Date();
            when(hai.getId()).thenReturn("ai-1");
            when(hai.getActivityId()).thenReturn("serviceTask1");
            when(hai.getActivityType()).thenReturn("serviceTask");
            when(hai.getProcessInstanceId()).thenReturn("pi-1");
            when(hai.getStartTime()).thenReturn(startTime);
            when(hai.getDurationInMillis()).thenReturn(500L);
            when(hai.isCanceled()).thenReturn(false);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hai));

            List<HistoricActivityInstanceResultDto> result =
                    tools.queryHistoricActivityInstances(empty(HistoricActivityInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricActivityInstanceResultDto r = result.getFirst();
            assertEquals("ai-1", r.id());
            assertEquals("serviceTask1", r.activityId());
            assertEquals("serviceTask", r.activityType());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals(startTime, r.startTime());
            assertEquals(500L, r.durationInMillis());
            assertFalse(r.canceled());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricTaskInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricTaskInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricTaskInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricTaskInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricTaskInstanceResultDto> result =
                    tools.queryHistoricTaskInstances(empty(HistoricTaskInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).taskId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricTaskInstanceQueryDto dto = new HistoricTaskInstanceQueryDto(
                    "task-1", "pi-1", "root-pi-1", "BK-001",
                    List.of("BK-001"), "BK%", "exec-1",
                    List.of("ai-1"), "pd-1", "orderProcess", "Order Process",
                    "cd-1", "ck-1", "Order Case", "ci-1", "ce-1",
                    "My Task", "My Task%", "A description", "A description%",
                    "userTask", List.of("userTask"), "completed", "completed%",
                    true, false, "user1", "user1%", "owner1", "owner1%",
                    5, true, false,
                    "involved1", "group1", "candidate1", "candidateGroup1",
                    true, false, "parent-task",
                    null, null, false, null, null,
                    List.of("t1"), true, null
            );

            tools.queryHistoricTaskInstances(dto, null);

            verify(query).taskId("task-1");
            verify(query).processInstanceId("pi-1");
            verify(query).taskAssignee("user1");
            verify(query).taskOwner("owner1");
            verify(query).taskPriority(5);
            verify(query).taskAssigned();
            verify(query).taskDefinitionKey("userTask");
            verify(query).processFinished();
            verify(query).withCandidateGroups();
            verify(query).taskParentTaskId("parent-task");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricTaskInstanceQueryDto dto = new HistoricTaskInstanceQueryDto(
                    null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    false, null, null, null, null, null,
                    null, false, null,
                    null, null, null, null,
                    false, null, null,
                    null, null, false, null, null,
                    null, false, null
            );

            tools.queryHistoricTaskInstances(dto, null);

            verify(query, never()).taskAssigned();
            verify(query, never()).taskUnassigned();
            verify(query, never()).processFinished();
            verify(query, never()).processUnfinished();
            verify(query, never()).withCandidateGroups();
            verify(query, never()).withoutCandidateGroups();
            verify(query, never()).withoutTaskDueDate();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricTaskInstance hti = mock(HistoricTaskInstance.class);
            Date startTime = new Date();
            when(hti.getId()).thenReturn("task-1");
            when(hti.getName()).thenReturn("Approve Request");
            when(hti.getAssignee()).thenReturn("user1");
            when(hti.getProcessInstanceId()).thenReturn("pi-1");
            when(hti.getStartTime()).thenReturn(startTime);
            when(hti.getPriority()).thenReturn(50);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hti));

            List<HistoricTaskInstanceResultDto> result =
                    tools.queryHistoricTaskInstances(empty(HistoricTaskInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricTaskInstanceResultDto r = result.getFirst();
            assertEquals("task-1", r.id());
            assertEquals("Approve Request", r.name());
            assertEquals("user1", r.assignee());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals(startTime, r.startTime());
            assertEquals(50, r.priority());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricDetails
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricDetails {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricDetailQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricDetailQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricDetailResultDto> result =
                    tools.queryHistoricDetails(empty(HistoricDetailQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).detailId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricDetailQueryDto dto = new HistoricDetailQueryDto(
                    "detail-1", "pi-1", "ci-1", "exec-1", "ce-1",
                    "ai-1", "task-1", "var-inst-1",
                    List.of("String", "Integer"), "my%",
                    false, true, false,
                    "op-1", List.of("pi-1"), List.of("t1"), true, null
            );

            tools.queryHistoricDetails(dto, null);

            verify(query).detailId("detail-1");
            verify(query).processInstanceId("pi-1");
            verify(query).caseInstanceId("ci-1");
            verify(query).executionId("exec-1");
            verify(query).caseExecutionId("ce-1");
            verify(query).activityInstanceId("ai-1");
            verify(query).taskId("task-1");
            verify(query).variableInstanceId("var-inst-1");
            verify(query).variableTypeIn("String", "Integer");
            verify(query).variableNameLike("my%");
            verify(query).variableUpdates();
            verify(query).userOperationId("op-1");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricDetailQueryDto dto = new HistoricDetailQueryDto(
                    null, null, null, null, null,
                    null, null, null,
                    null, null,
                    false, null, null,
                    null, null, null, false, null
            );

            tools.queryHistoricDetails(dto, null);

            verify(query, never()).formFields();
            verify(query, never()).variableUpdates();
            verify(query, never()).excludeTaskDetails();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricDetail hd = mock(HistoricDetail.class);
            Date time = new Date();
            when(hd.getId()).thenReturn("detail-1");
            when(hd.getProcessInstanceId()).thenReturn("pi-1");
            when(hd.getExecutionId()).thenReturn("exec-1");
            when(hd.getTime()).thenReturn(time);
            when(hd.getTenantId()).thenReturn("t1");
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hd));

            List<HistoricDetailResultDto> result =
                    tools.queryHistoricDetails(empty(HistoricDetailQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricDetailResultDto r = result.getFirst();
            assertEquals("detail-1", r.id());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals("exec-1", r.executionId());
            assertEquals(time, r.time());
            assertEquals("t1", r.tenantId());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricVariableInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricVariableInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricVariableInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricVariableInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricVariableInstanceResultDto> result =
                    tools.queryHistoricVariableInstances(empty(HistoricVariableInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).variableId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricVariableInstanceQueryDto dto = new HistoricVariableInstanceQueryDto(
                    "var-1", "pi-1", "pd-1", "orderProcess",
                    "ci-1", "myVar", "my%",
                    List.of("String", "Integer"), true, true,
                    List.of("pi-1"), List.of("task-1"), List.of("exec-1"),
                    List.of("ce-1"), List.of("stage1"), List.of("ai-1"),
                    List.of("t1"), true, null
            );

            tools.queryHistoricVariableInstances(dto, null);

            verify(query).variableId("var-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).caseInstanceId("ci-1");
            verify(query).variableName("myVar");
            verify(query).variableNameLike("my%");
            verify(query).variableTypeIn("String", "Integer");
            verify(query).matchVariableNamesIgnoreCase();
            verify(query).matchVariableValuesIgnoreCase();
            verify(query).processInstanceIdIn("pi-1");
            verify(query).taskIdIn("task-1");
            verify(query).executionIdIn("exec-1");
            verify(query).caseExecutionIdIn("ce-1");
            verify(query).caseActivityIdIn("stage1");
            verify(query).activityInstanceIdIn("ai-1");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricVariableInstanceQueryDto dto = new HistoricVariableInstanceQueryDto(
                    null, null, null, null,
                    null, null, null,
                    null, false, null,
                    null, null, null,
                    null, null, null,
                    null, false, null
            );

            tools.queryHistoricVariableInstances(dto, null);

            verify(query, never()).matchVariableNamesIgnoreCase();
            verify(query, never()).matchVariableValuesIgnoreCase();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricVariableInstance hvi = mock(HistoricVariableInstance.class);
            Date createTime = new Date();
            when(hvi.getId()).thenReturn("var-1");
            when(hvi.getName()).thenReturn("myVar");
            when(hvi.getTypeName()).thenReturn("String");
            when(hvi.getValue()).thenReturn("hello");
            when(hvi.getProcessInstanceId()).thenReturn("pi-1");
            when(hvi.getTenantId()).thenReturn("t1");
            when(hvi.getCreateTime()).thenReturn(createTime);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hvi));

            List<HistoricVariableInstanceResultDto> result =
                    tools.queryHistoricVariableInstances(empty(HistoricVariableInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricVariableInstanceResultDto r = result.getFirst();
            assertEquals("var-1", r.id());
            assertEquals("myVar", r.name());
            assertEquals("String", r.typeName());
            assertEquals("hello", r.value());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals("t1", r.tenantId());
            assertEquals(createTime, r.createTime());
        }
    }

    // -------------------------------------------------------------------------
    // queryUserOperationLog
    // -------------------------------------------------------------------------

    @Nested
    class QueryUserOperationLog {

        @Mock(answer = Answers.RETURNS_SELF)
        private UserOperationLogQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createUserOperationLogQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<UserOperationLogEntryResultDto> result =
                    tools.queryUserOperationLog(empty(UserOperationLogQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).entityType(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date after = new Date();
            Date before = new Date();

            UserOperationLogQueryDto dto = new UserOperationLogQueryDto(
                    "Task", List.of("Task", "ProcessInstance"),
                    "Assign", "deploy-1", "pd-1", "orderProcess",
                    "pi-1", "exec-1", "cd-1", "ci-1", "ce-1",
                    "task-1", "job-1", "jdef-1", "batch-1",
                    "user1", "op-1", "ext-1", "assignee",
                    "TaskWorker", List.of("TaskWorker"),
                    after, before,
                    List.of("t1"), true, null
            );

            tools.queryUserOperationLog(dto, null);

            verify(query).entityType("Task");
            verify(query).entityTypeIn("Task", "ProcessInstance");
            verify(query).operationType("Assign");
            verify(query).deploymentId("deploy-1");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).processInstanceId("pi-1");
            verify(query).executionId("exec-1");
            verify(query).taskId("task-1");
            verify(query).userId("user1");
            verify(query).operationId("op-1");
            verify(query).property("assignee");
            verify(query).category("TaskWorker");
            verify(query).categoryIn("TaskWorker");
            verify(query).afterTimestamp(after);
            verify(query).beforeTimestamp(before);
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            UserOperationLogQueryDto dto = new UserOperationLogQueryDto(
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, false, null
            );

            tools.queryUserOperationLog(dto, null);

            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            UserOperationLogEntry entry = mock(UserOperationLogEntry.class);
            Date ts = new Date();
            when(entry.getId()).thenReturn("log-1");
            when(entry.getUserId()).thenReturn("user1");
            when(entry.getOperationType()).thenReturn("Assign");
            when(entry.getEntityType()).thenReturn("Task");
            when(entry.getTimestamp()).thenReturn(ts);
            when(entry.getProperty()).thenReturn("assignee");
            when(entry.getNewValue()).thenReturn("user2");
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(entry));

            List<UserOperationLogEntryResultDto> result =
                    tools.queryUserOperationLog(empty(UserOperationLogQueryDto.class), null);

            assertEquals(1, result.size());
            UserOperationLogEntryResultDto r = result.getFirst();
            assertEquals("log-1", r.id());
            assertEquals("user1", r.userId());
            assertEquals("Assign", r.operationType());
            assertEquals("Task", r.entityType());
            assertEquals(ts, r.timestamp());
            assertEquals("assignee", r.property());
            assertEquals("user2", r.newValue());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricIncidents
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricIncidents {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricIncidentQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricIncidentQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricIncidentResultDto> result =
                    tools.queryHistoricIncidents(empty(HistoricIncidentQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).incidentId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricIncidentQueryDto dto = new HistoricIncidentQueryDto(
                    "inc-1", "failedJob", "Job failed", "Job failed%",
                    "pd-1", "orderProcess", List.of("orderProcess"),
                    "pi-1", "exec-1",
                    null, null, null, null,
                    "serviceTask1", "serviceTask1",
                    "cause-inc-1", "root-inc-1",
                    List.of("t1"), true,
                    "config1", "histConfig1",
                    List.of("jdef-1"), true, false, false, null
            );

            tools.queryHistoricIncidents(dto, null);

            verify(query).incidentId("inc-1");
            verify(query).incidentType("failedJob");
            verify(query).incidentMessage("Job failed");
            verify(query).incidentMessageLike("Job failed%");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).processDefinitionKeyIn("orderProcess");
            verify(query).processInstanceId("pi-1");
            verify(query).executionId("exec-1");
            verify(query).activityId("serviceTask1");
            verify(query).failedActivityId("serviceTask1");
            verify(query).causeIncidentId("cause-inc-1");
            verify(query).rootCauseIncidentId("root-inc-1");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).configuration("config1");
            verify(query).historyConfiguration("histConfig1");
            verify(query).jobDefinitionIdIn("jdef-1");
            verify(query).open();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricIncidentQueryDto dto = new HistoricIncidentQueryDto(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, false, null, null, null,
                    false, false, null, null
            );

            tools.queryHistoricIncidents(dto, null);

            verify(query, never()).open();
            verify(query, never()).resolved();
            verify(query, never()).deleted();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricIncident hi = mock(HistoricIncident.class);
            Date createTime = new Date();
            when(hi.getId()).thenReturn("inc-1");
            when(hi.getIncidentType()).thenReturn("failedJob");
            when(hi.getProcessInstanceId()).thenReturn("pi-1");
            when(hi.getCreateTime()).thenReturn(createTime);
            when(hi.isOpen()).thenReturn(true);
            when(hi.isDeleted()).thenReturn(false);
            when(hi.isResolved()).thenReturn(false);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hi));

            List<HistoricIncidentResultDto> result =
                    tools.queryHistoricIncidents(empty(HistoricIncidentQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricIncidentResultDto r = result.getFirst();
            assertEquals("inc-1", r.id());
            assertEquals("failedJob", r.incidentType());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals(createTime, r.createTime());
            assertTrue(r.open());
            assertFalse(r.deleted());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricIdentityLinkLog
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricIdentityLinkLog {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricIdentityLinkLogQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricIdentityLinkLogQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricIdentityLinkLogResultDto> result =
                    tools.queryHistoricIdentityLinkLog(empty(HistoricIdentityLinkLogQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).type(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date before = new Date();
            Date after = new Date();

            HistoricIdentityLinkLogQueryDto dto = new HistoricIdentityLinkLogQueryDto(
                    before, after, "candidate", "user1", "group1",
                    "task-1", "pd-1", "orderProcess",
                    "add", "assigner1",
                    List.of("t1"), true, null
            );

            tools.queryHistoricIdentityLinkLog(dto, null);

            verify(query).dateBefore(before);
            verify(query).dateAfter(after);
            verify(query).type("candidate");
            verify(query).userId("user1");
            verify(query).groupId("group1");
            verify(query).taskId("task-1");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).operationType("add");
            verify(query).assignerId("assigner1");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricIdentityLinkLogQueryDto dto = new HistoricIdentityLinkLogQueryDto(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, false, null
            );

            tools.queryHistoricIdentityLinkLog(dto, null);

            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricIdentityLinkLog hill = mock(HistoricIdentityLinkLog.class);
            Date time = new Date();
            when(hill.getId()).thenReturn("link-1");
            when(hill.getType()).thenReturn("candidate");
            when(hill.getUserId()).thenReturn("user1");
            when(hill.getTaskId()).thenReturn("task-1");
            when(hill.getTime()).thenReturn(time);
            when(hill.getOperationType()).thenReturn("add");
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hill));

            List<HistoricIdentityLinkLogResultDto> result =
                    tools.queryHistoricIdentityLinkLog(empty(HistoricIdentityLinkLogQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricIdentityLinkLogResultDto r = result.getFirst();
            assertEquals("link-1", r.id());
            assertEquals("candidate", r.type());
            assertEquals("user1", r.userId());
            assertEquals("task-1", r.taskId());
            assertEquals(time, r.time());
            assertEquals("add", r.operationType());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricCaseInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricCaseInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricCaseInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricCaseInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricCaseInstanceResultDto> result =
                    tools.queryHistoricCaseInstances(empty(HistoricCaseInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).caseInstanceId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date createdBefore = new Date();

            HistoricCaseInstanceQueryDto dto = new HistoricCaseInstanceQueryDto(
                    "ci-1", List.of("ci-1", "ci-2"),
                    "cd-1", "myCase", List.of("otherCase"),
                    "My Case", "My Case%",
                    "BK-001", "BK%",
                    List.of("stage1"), createdBefore, null, null, null,
                    "user1", "super-ci-1", "sub-ci-1",
                    "super-pi-1", "sub-pi-1",
                    List.of("t1"), true,
                    true, false, false, false, false, null
            );

            tools.queryHistoricCaseInstances(dto, null);

            verify(query).caseInstanceId("ci-1");
            verify(query).caseDefinitionId("cd-1");
            verify(query).caseDefinitionKey("myCase");
            verify(query).caseDefinitionName("My Case");
            verify(query).caseDefinitionNameLike("My Case%");
            verify(query).caseInstanceBusinessKey("BK-001");
            verify(query).caseInstanceBusinessKeyLike("BK%");
            verify(query).caseActivityIdIn("stage1");
            verify(query).createdBefore(createdBefore);
            verify(query).createdBy("user1");
            verify(query).superCaseInstanceId("super-ci-1");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).active();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricCaseInstanceQueryDto dto = new HistoricCaseInstanceQueryDto(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null,
                    null, false,
                    false, null, null, null, null, null
            );

            tools.queryHistoricCaseInstances(dto, null);

            verify(query, never()).active();
            verify(query, never()).completed();
            verify(query, never()).terminated();
            verify(query, never()).closed();
            verify(query, never()).notClosed();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricCaseInstance hci = mock(HistoricCaseInstance.class);
            Date createTime = new Date();
            when(hci.getId()).thenReturn("ci-1");
            when(hci.getBusinessKey()).thenReturn("BK-001");
            when(hci.getCaseDefinitionKey()).thenReturn("myCase");
            when(hci.getCreateTime()).thenReturn(createTime);
            when(hci.isActive()).thenReturn(true);
            when(hci.isClosed()).thenReturn(false);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hci));

            List<HistoricCaseInstanceResultDto> result =
                    tools.queryHistoricCaseInstances(empty(HistoricCaseInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricCaseInstanceResultDto r = result.getFirst();
            assertEquals("ci-1", r.id());
            assertEquals("BK-001", r.businessKey());
            assertEquals("myCase", r.caseDefinitionKey());
            assertEquals(createTime, r.createTime());
            assertTrue(r.active());
            assertFalse(r.closed());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricCaseActivityInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricCaseActivityInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricCaseActivityInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricCaseActivityInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricCaseActivityInstanceResultDto> result =
                    tools.queryHistoricCaseActivityInstances(empty(HistoricCaseActivityInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).caseActivityInstanceId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date createdBefore = new Date();

            HistoricCaseActivityInstanceQueryDto dto = new HistoricCaseActivityInstanceQueryDto(
                    "cai-1", List.of("cai-1", "cai-2"),
                    "ce-1", "ci-1", "cd-1",
                    "stage1", List.of("stage1", "task1"),
                    "My Stage", "stage",
                    createdBefore, null, null, null,
                    true, false, null, false, null, null,
                    true, false, null,
                    List.of("t1"), true, null
            );

            tools.queryHistoricCaseActivityInstances(dto, null);

            verify(query).caseActivityInstanceId("cai-1");
            verify(query).caseExecutionId("ce-1");
            verify(query).caseInstanceId("ci-1");
            verify(query).caseDefinitionId("cd-1");
            verify(query).caseActivityId("stage1");
            verify(query).caseActivityName("My Stage");
            verify(query).caseActivityType("stage");
            verify(query).createdBefore(createdBefore);
            verify(query).required();
            verify(query).active();
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricCaseActivityInstanceQueryDto dto = new HistoricCaseActivityInstanceQueryDto(
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    false, false, null, false, null, null,
                    false, false, null,
                    null, false, null
            );

            tools.queryHistoricCaseActivityInstances(dto, null);

            verify(query, never()).required();
            verify(query, never()).ended();
            verify(query, never()).available();
            verify(query, never()).active();
            verify(query, never()).completed();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricCaseActivityInstance hcai = mock(HistoricCaseActivityInstance.class);
            Date createTime = new Date();
            when(hcai.getId()).thenReturn("cai-1");
            when(hcai.getCaseActivityId()).thenReturn("stage1");
            when(hcai.getCaseActivityName()).thenReturn("My Stage");
            when(hcai.getCaseInstanceId()).thenReturn("ci-1");
            when(hcai.getCreateTime()).thenReturn(createTime);
            when(hcai.isActive()).thenReturn(true);
            when(hcai.isCompleted()).thenReturn(false);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hcai));

            List<HistoricCaseActivityInstanceResultDto> result =
                    tools.queryHistoricCaseActivityInstances(empty(HistoricCaseActivityInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricCaseActivityInstanceResultDto r = result.getFirst();
            assertEquals("cai-1", r.id());
            assertEquals("stage1", r.caseActivityId());
            assertEquals("My Stage", r.caseActivityName());
            assertEquals("ci-1", r.caseInstanceId());
            assertEquals(createTime, r.createTime());
            assertTrue(r.active());
            assertFalse(r.completed());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricDecisionInstances
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricDecisionInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricDecisionInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricDecisionInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricDecisionInstanceResultDto> result =
                    tools.queryHistoricDecisionInstances(empty(HistoricDecisionInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).decisionInstanceId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());
            Date evaluatedBefore = new Date();

            HistoricDecisionInstanceQueryDto dto = new HistoricDecisionInstanceQueryDto(
                    "ddi-1", List.of("ddi-1"),
                    "dd-1", List.of("dd-1"),
                    "myDecision", List.of("myDecision"),
                    "My Decision", "My Decision%",
                    "orderProcess", "pd-1", "pi-1",
                    "myCase", "cd-1", "ci-1",
                    List.of("serviceTask1"), List.of("ai-1"),
                    evaluatedBefore, null,
                    "user1", true, true,
                    "root-ddi-1", true,
                    List.of("t1"), true, null
            );

            tools.queryHistoricDecisionInstances(dto, null);

            verify(query).decisionInstanceId("ddi-1");
            verify(query).decisionInstanceIdIn("ddi-1");
            verify(query).decisionDefinitionId("dd-1");
            verify(query).decisionDefinitionIdIn("dd-1");
            verify(query).decisionDefinitionKey("myDecision");
            verify(query).decisionDefinitionKeyIn("myDecision");
            verify(query).decisionDefinitionName("My Decision");
            verify(query).decisionDefinitionNameLike("My Decision%");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).processDefinitionId("pd-1");
            verify(query).processInstanceId("pi-1");
            verify(query).caseDefinitionKey("myCase");
            verify(query).caseDefinitionId("cd-1");
            verify(query).caseInstanceId("ci-1");
            verify(query).activityIdIn("serviceTask1");
            verify(query).activityInstanceIdIn("ai-1");
            verify(query).evaluatedBefore(evaluatedBefore);
            verify(query).userId("user1");
            verify(query).includeInputs();
            verify(query).includeOutputs();
            verify(query).rootDecisionInstanceId("root-ddi-1");
            verify(query).rootDecisionInstancesOnly();
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricDecisionInstanceQueryDto dto = new HistoricDecisionInstanceQueryDto(
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null,
                    false, false, null, false,
                    null, false, null
            );

            tools.queryHistoricDecisionInstances(dto, null);

            verify(query, never()).includeInputs();
            verify(query, never()).includeOutputs();
            verify(query, never()).rootDecisionInstancesOnly();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricDecisionInstance hdi = mock(HistoricDecisionInstance.class);
            Date evaluationTime = new Date();
            when(hdi.getId()).thenReturn("ddi-1");
            when(hdi.getDecisionDefinitionKey()).thenReturn("myDecision");
            when(hdi.getDecisionDefinitionName()).thenReturn("My Decision");
            when(hdi.getEvaluationTime()).thenReturn(evaluationTime);
            when(hdi.getProcessInstanceId()).thenReturn("pi-1");
            when(hdi.getTenantId()).thenReturn("t1");
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hdi));

            List<HistoricDecisionInstanceResultDto> result =
                    tools.queryHistoricDecisionInstances(empty(HistoricDecisionInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricDecisionInstanceResultDto r = result.getFirst();
            assertEquals("ddi-1", r.id());
            assertEquals("myDecision", r.decisionDefinitionKey());
            assertEquals("My Decision", r.decisionDefinitionName());
            assertEquals(evaluationTime, r.evaluationTime());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals("t1", r.tenantId());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricJobLog
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricJobLog {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricJobLogQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricJobLogQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricJobLogResultDto> result =
                    tools.queryHistoricJobLog(empty(HistoricJobLogQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).logId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricJobLogQueryDto dto = new HistoricJobLogQueryDto(
                    "log-1", "job-1", "Connection refused", "jdef-1",
                    "timerExpiration", "R3/PT1H",
                    List.of("timerTask"), List.of("timerTask"),
                    List.of("exec-1"), "pi-1", "pd-1", "orderProcess",
                    "deploy-1", List.of("t1"), true,
                    "worker1", 5L, 100L,
                    true, false, false, false, null
            );

            tools.queryHistoricJobLog(dto, null);

            verify(query).logId("log-1");
            verify(query).jobId("job-1");
            verify(query).jobExceptionMessage("Connection refused");
            verify(query).jobDefinitionId("jdef-1");
            verify(query).jobDefinitionType("timerExpiration");
            verify(query).jobDefinitionConfiguration("R3/PT1H");
            verify(query).activityIdIn("timerTask");
            verify(query).failedActivityIdIn("timerTask");
            verify(query).executionIdIn("exec-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).deploymentId("deploy-1");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).hostname("worker1");
            verify(query).jobPriorityHigherThanOrEquals(5L);
            verify(query).jobPriorityLowerThanOrEquals(100L);
            verify(query).creationLog();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricJobLogQueryDto dto = new HistoricJobLogQueryDto(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, false, null, null, null,
                    false, null, null, null, null
            );

            tools.queryHistoricJobLog(dto, null);

            verify(query, never()).creationLog();
            verify(query, never()).failureLog();
            verify(query, never()).successLog();
            verify(query, never()).deletionLog();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricJobLog hjl = mock(HistoricJobLog.class);
            Date ts = new Date();
            when(hjl.getId()).thenReturn("log-1");
            when(hjl.getJobId()).thenReturn("job-1");
            when(hjl.getTimestamp()).thenReturn(ts);
            when(hjl.getJobRetries()).thenReturn(3);
            when(hjl.getJobPriority()).thenReturn(0L);
            when(hjl.isCreationLog()).thenReturn(true);
            when(hjl.isFailureLog()).thenReturn(false);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hjl));

            List<HistoricJobLogResultDto> result =
                    tools.queryHistoricJobLog(empty(HistoricJobLogQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricJobLogResultDto r = result.getFirst();
            assertEquals("log-1", r.id());
            assertEquals("job-1", r.jobId());
            assertEquals(ts, r.timestamp());
            assertEquals(3, r.jobRetries());
            assertTrue(r.creationLog());
            assertFalse(r.failureLog());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricBatches
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricBatches {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricBatchQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricBatchQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricBatchResultDto> result =
                    tools.queryHistoricBatches(empty(HistoricBatchQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).batchId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricBatchQueryDto dto = new HistoricBatchQueryDto(
                    "batch-1", "aPDeletion", true,
                    List.of("t1"), true, null
            );

            tools.queryHistoricBatches(dto, null);

            verify(query).batchId("batch-1");
            verify(query).type("aPDeletion");
            verify(query).completed(true);
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricBatchQueryDto dto = new HistoricBatchQueryDto(
                    null, null, null, null, false, null
            );

            tools.queryHistoricBatches(dto, null);

            verify(query, never()).completed(anyBoolean());
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricBatch hb = mock(HistoricBatch.class);
            Date startTime = new Date();
            when(hb.getId()).thenReturn("batch-1");
            when(hb.getType()).thenReturn("aPDeletion");
            when(hb.getTotalJobs()).thenReturn(100);
            when(hb.getStartTime()).thenReturn(startTime);
            when(hb.getTenantId()).thenReturn("t1");
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hb));

            List<HistoricBatchResultDto> result =
                    tools.queryHistoricBatches(empty(HistoricBatchQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricBatchResultDto r = result.getFirst();
            assertEquals("batch-1", r.id());
            assertEquals("aPDeletion", r.type());
            assertEquals(100, r.totalJobs());
            assertEquals(startTime, r.startTime());
            assertEquals("t1", r.tenantId());
        }
    }

    // -------------------------------------------------------------------------
    // queryHistoricExternalTaskLog
    // -------------------------------------------------------------------------

    @Nested
    class QueryHistoricExternalTaskLog {

        @Mock(answer = Answers.RETURNS_SELF)
        private HistoricExternalTaskLogQuery query;

        @BeforeEach
        void setUp() {
            when(historyService.createHistoricExternalTaskLogQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListPageWithNoFilters() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            List<HistoricExternalTaskLogResultDto> result =
                    tools.queryHistoricExternalTaskLog(empty(HistoricExternalTaskLogQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(0, DEFAULT_MAX);
            verify(query, never()).logId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricExternalTaskLogQueryDto dto = new HistoricExternalTaskLogQueryDto(
                    "log-1", "ext-1", "myTopic", "worker1", "Connection refused",
                    List.of("myServiceTask"), List.of("ai-1"), List.of("exec-1"),
                    "pi-1", "pd-1", "orderProcess",
                    List.of("t1"), true,
                    5L, 100L,
                    true, false, false, false, null
            );

            tools.queryHistoricExternalTaskLog(dto, null);

            verify(query).logId("log-1");
            verify(query).externalTaskId("ext-1");
            verify(query).topicName("myTopic");
            verify(query).workerId("worker1");
            verify(query).errorMessage("Connection refused");
            verify(query).activityIdIn("myServiceTask");
            verify(query).activityInstanceIdIn("ai-1");
            verify(query).executionIdIn("exec-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).priorityHigherThanOrEquals(5L);
            verify(query).priorityLowerThanOrEquals(100L);
            verify(query).creationLog();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(Collections.emptyList());

            HistoricExternalTaskLogQueryDto dto = new HistoricExternalTaskLogQueryDto(
                    null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, false, null, null,
                    false, null, null, null, null
            );

            tools.queryHistoricExternalTaskLog(dto, null);

            verify(query, never()).creationLog();
            verify(query, never()).failureLog();
            verify(query, never()).successLog();
            verify(query, never()).deletionLog();
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            HistoricExternalTaskLog hetl = mock(HistoricExternalTaskLog.class);
            Date ts = new Date();
            when(hetl.getId()).thenReturn("log-1");
            when(hetl.getExternalTaskId()).thenReturn("ext-1");
            when(hetl.getTopicName()).thenReturn("myTopic");
            when(hetl.getTimestamp()).thenReturn(ts);
            when(hetl.getPriority()).thenReturn(50L);
            when(hetl.isCreationLog()).thenReturn(true);
            when(hetl.isFailureLog()).thenReturn(false);
            when(query.listPage(0, DEFAULT_MAX)).thenReturn(List.of(hetl));

            List<HistoricExternalTaskLogResultDto> result =
                    tools.queryHistoricExternalTaskLog(empty(HistoricExternalTaskLogQueryDto.class), null);

            assertEquals(1, result.size());
            HistoricExternalTaskLogResultDto r = result.getFirst();
            assertEquals("log-1", r.id());
            assertEquals("ext-1", r.externalTaskId());
            assertEquals("myTopic", r.topicName());
            assertEquals(ts, r.timestamp());
            assertEquals(50L, r.priority());
            assertTrue(r.creationLog());
            assertFalse(r.failureLog());
        }
    }
}
