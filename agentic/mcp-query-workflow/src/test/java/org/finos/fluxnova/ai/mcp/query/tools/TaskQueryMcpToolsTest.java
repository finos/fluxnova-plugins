package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.TaskResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.TaskQueryDto;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.finos.fluxnova.bpm.engine.task.DelegationState;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.finos.fluxnova.bpm.engine.task.TaskQuery;
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
class TaskQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static TaskQueryDto emptyDto() {
        try {
            return MAPPER.readValue("{}", TaskQueryDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private TaskService taskService;

    private TaskQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new TaskQueryMcpTools(taskService, 200);
    }

    // ========================================================================
    // Task Query Tests
    // ========================================================================

    @Nested
    class QueryTasks {

        @Mock(answer = Answers.RETURNS_SELF)
        private TaskQuery query;

        @BeforeEach
        void setUp() {
            when(taskService.createTaskQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<TaskResultDto> result = tools.queryTasks(emptyDto(), null);

            assertTrue(result.isEmpty());
            verify(query).initializeFormKeys();
            verify(query).listPage(anyInt(), anyInt());
            verify(query, never()).taskId(any());
            verify(query, never()).taskAssignee(any());
        }

        @Test
        void nullDto_callsInitializeFormKeys() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<TaskResultDto> result = tools.queryTasks(null, null);

            assertTrue(result.isEmpty());
            verify(query).initializeFormKeys();
            verify(query).listPage(anyInt(), anyInt());
        }

        @Test
        void stringFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            TaskQueryDto dto = new TaskQueryDto(
                    "task-1", null,
                    "pi-1", null,
                    "order-123", null, "order-%",
                    "def-1", "invoiceProcess", null,
                    "Invoice Process", "Invoice%",
                    "exec-1",
                    "john", "jo%", null,
                    "jane",
                    "managers", "john", null,
                    "john",
                    null, null,
                    "approveInvoice", null, "approve%",
                    "Approve Invoice", "Reject Invoice", "Approve%", "Reject%",
                    "Please approve", "approve%",
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null,
                    null, null, null, null,
                    null, null,
                    null, null,
                    "parent-1",
                    "case-1", "case-bk-1", "case-%",
                    "caseDef-1", "myCaseDef", "My Case", "My%",
                    "caseExec-1",
                    null
            );

            tools.queryTasks(dto, null);

            verify(query).taskId("task-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processInstanceBusinessKey("order-123");
            verify(query).processInstanceBusinessKeyLike("order-%");
            verify(query).processDefinitionId("def-1");
            verify(query).processDefinitionKey("invoiceProcess");
            verify(query).processDefinitionName("Invoice Process");
            verify(query).processDefinitionNameLike("Invoice%");
            verify(query).executionId("exec-1");
            verify(query).taskAssignee("john");
            verify(query).taskAssigneeLike("jo%");
            verify(query).taskOwner("jane");
            verify(query).taskCandidateGroup("managers");
            verify(query).taskCandidateUser("john");
            verify(query).taskInvolvedUser("john");
            verify(query).taskDefinitionKey("approveInvoice");
            verify(query).taskDefinitionKeyLike("approve%");
            verify(query).taskName("Approve Invoice");
            verify(query).taskNameNotEqual("Reject Invoice");
            verify(query).taskNameLike("Approve%");
            verify(query).taskNameNotLike("Reject%");
            verify(query).taskDescription("Please approve");
            verify(query).taskDescriptionLike("approve%");
            verify(query).taskParentTaskId("parent-1");
            verify(query).caseInstanceId("case-1");
            verify(query).caseInstanceBusinessKey("case-bk-1");
            verify(query).caseInstanceBusinessKeyLike("case-%");
            verify(query).caseDefinitionId("caseDef-1");
            verify(query).caseDefinitionKey("myCaseDef");
            verify(query).caseDefinitionName("My Case");
            verify(query).caseDefinitionNameLike("My%");
            verify(query).caseExecutionId("caseExec-1");
        }

        @Test
        void listFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            TaskQueryDto dto = new TaskQueryDto(
                    null, List.of("t1", "t2"),
                    null, List.of("pi-1", "pi-2"),
                    null, List.of("bk-1", "bk-2"), null,
                    null, null, List.of("proc1", "proc2"),
                    null, null, null,
                    null, null, List.of("john", "jane"),
                    null, null, null, List.of("managers", "admins"),
                    null, null, null,
                    null, List.of("approve", "review"), null,
                    null, null, null, null,
                    null, null,
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null,
                    null, null, null, null,
                    null, null,
                    List.of("t1", "t2"), null,
                    null,
                    null, null, null,
                    null, null, null, null,
                    null,
                    null
            );

            tools.queryTasks(dto, null);

            verify(query).taskIdIn("t1", "t2");
            verify(query).processInstanceIdIn("pi-1", "pi-2");
            verify(query).processInstanceBusinessKeyIn("bk-1", "bk-2");
            verify(query).processDefinitionKeyIn("proc1", "proc2");
            verify(query).taskAssigneeIn("john", "jane");
            verify(query).taskCandidateGroupIn(List.of("managers", "admins"));
            verify(query).taskDefinitionKeyIn("approve", "review");
            verify(query).tenantIdIn("t1", "t2");
        }

        @Test
        void integerAndDateFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());
            Date dueDate = new Date();
            Date dueAfter = new Date();
            Date dueBefore = new Date();
            Date followUpDate = new Date();
            Date followUpAfter = new Date();
            Date followUpBefore = new Date();
            Date followUpBeforeOrNotExistent = new Date();
            Date createdOn = new Date();
            Date createdAfter = new Date();
            Date createdBefore = new Date();
            Date updatedAfter = new Date();

            TaskQueryDto dto = new TaskQueryDto(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null, null,
                    5, 10, 1,
                    dueDate, dueAfter, dueBefore, null,
                    followUpDate, followUpAfter, followUpBefore, followUpBeforeOrNotExistent,
                    createdOn, createdAfter, createdBefore, updatedAfter,
                    null,
                    null, null, null, null,
                    null, null,
                    null, null, null,
                    null, null, null, null, null, null, null,
                    null,
                    null
            );

            tools.queryTasks(dto, null);

            verify(query).taskPriority(5);
            verify(query).taskMaxPriority(10);
            verify(query).taskMinPriority(1);
            verify(query).dueDate(dueDate);
            verify(query).dueAfter(dueAfter);
            verify(query).dueBefore(dueBefore);
            verify(query).followUpDate(followUpDate);
            verify(query).followUpAfter(followUpAfter);
            verify(query).followUpBefore(followUpBefore);
            verify(query).followUpBeforeOrNotExistent(followUpBeforeOrNotExistent);
            verify(query).taskCreatedOn(createdOn);
            verify(query).taskCreatedAfter(createdAfter);
            verify(query).taskCreatedBefore(createdBefore);
            verify(query).taskUpdatedAfter(updatedAfter);
        }

        @Test
        void booleanFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            TaskQueryDto dto = new TaskQueryDto(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    true, true,
                    null, null, null,
                    null, null, null, null, null, null,
                    null, null, null,
                    null, null, null, true,
                    null, null, null, null,
                    null, null, null, null,
                    null,
                    true, true, true, true,
                    true, true,
                    null, true,
                    null,
                    null, null, null, null, null, null, null,
                    null,
                    true
            );

            tools.queryTasks(dto, null);

            verify(query).taskAssigned();
            verify(query).taskUnassigned();
            verify(query).withoutDueDate();
            verify(query).withCandidateGroups();
            verify(query).withoutCandidateGroups();
            verify(query).withCandidateUsers();
            verify(query).withoutCandidateUsers();
            verify(query).active();
            verify(query).suspended();
            verify(query).withoutTenantId();
            verify(query).excludeSubtasks();
        }

        @Test
        void delegationStateFilter() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            TaskQueryDto dto = new TaskQueryDto(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    "PENDING",
                    null, null, null, null,
                    null, null,
                    null, null, null,
                    null, null, null, null, null, null, null,
                    null,
                    null
            );

            tools.queryTasks(dto, null);

            verify(query).taskDelegationState(DelegationState.PENDING);
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            TaskQueryDto dto = new TaskQueryDto(
                    null, Collections.emptyList(),
                    null, Collections.emptyList(),
                    null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    false, null,
                    null, null, null,
                    null, null, null, null, null, null,
                    null, null, null,
                    null, null, null, false,
                    null, null, null, null,
                    null, null, null, null,
                    null,
                    null, null, null, null,
                    false, null,
                    Collections.emptyList(), null,
                    null,
                    null, null, null, null, null, null, null,
                    null,
                    null
            );

            tools.queryTasks(dto, null);

            verify(query, never()).active();
            verify(query, never()).suspended();
            verify(query, never()).taskAssigned();
            verify(query, never()).taskUnassigned();
            verify(query, never()).withoutDueDate();
            verify(query, never()).excludeSubtasks();
            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).taskIdIn(any(String[].class));
            verify(query, never()).processInstanceIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            Task task = mock(Task.class);
            when(task.getId()).thenReturn("task-1");
            when(task.getName()).thenReturn("Approve Invoice");
            when(task.getAssignee()).thenReturn("john");
            when(task.getOwner()).thenReturn("jane");
            Date created = new Date();
            when(task.getCreateTime()).thenReturn(created);
            Date lastUpdated = new Date();
            when(task.getLastUpdated()).thenReturn(lastUpdated);
            Date due = new Date();
            when(task.getDueDate()).thenReturn(due);
            Date followUp = new Date();
            when(task.getFollowUpDate()).thenReturn(followUp);
            when(task.getDelegationState()).thenReturn(DelegationState.PENDING);
            when(task.getDescription()).thenReturn("Please approve this invoice");
            when(task.getExecutionId()).thenReturn("exec-1");
            when(task.getParentTaskId()).thenReturn("parent-1");
            when(task.getPriority()).thenReturn(50);
            when(task.getProcessDefinitionId()).thenReturn("def:1:abc");
            when(task.getProcessInstanceId()).thenReturn("pi-1");
            when(task.getCaseExecutionId()).thenReturn("caseExec-1");
            when(task.getCaseDefinitionId()).thenReturn("caseDef-1");
            when(task.getCaseInstanceId()).thenReturn("case-1");
            when(task.getTaskDefinitionKey()).thenReturn("approveInvoice");
            when(task.isSuspended()).thenReturn(false);
            when(task.getFormKey()).thenReturn("embedded:app:approve-form.html");
            when(task.getTenantId()).thenReturn("t1");
            when(task.getTaskState()).thenReturn("Created");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(task));

            List<TaskResultDto> result = tools.queryTasks(emptyDto(), null);

            assertEquals(1, result.size());
            TaskResultDto r = result.getFirst();
            assertEquals("task-1", r.id());
            assertEquals("Approve Invoice", r.name());
            assertEquals("john", r.assignee());
            assertEquals("jane", r.owner());
            assertEquals(created, r.created());
            assertEquals(lastUpdated, r.lastUpdated());
            assertEquals(due, r.due());
            assertEquals(followUp, r.followUp());
            assertEquals("PENDING", r.delegationState());
            assertEquals("Please approve this invoice", r.description());
            assertEquals("exec-1", r.executionId());
            assertEquals("parent-1", r.parentTaskId());
            assertEquals(50, r.priority());
            assertEquals("def:1:abc", r.processDefinitionId());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals("caseExec-1", r.caseExecutionId());
            assertEquals("caseDef-1", r.caseDefinitionId());
            assertEquals("case-1", r.caseInstanceId());
            assertEquals("approveInvoice", r.taskDefinitionKey());
            assertFalse(r.suspended());
            assertEquals("embedded:app:approve-form.html", r.formKey());
            assertEquals("t1", r.tenantId());
            assertEquals("Created", r.taskState());
        }

        @Test
        void resultMapping_nullDelegationState() {
            Task task = mock(Task.class);
            when(task.getId()).thenReturn("task-1");
            when(task.getDelegationState()).thenReturn(null);
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(task));

            List<TaskResultDto> result = tools.queryTasks(emptyDto(), null);

            assertEquals(1, result.size());
            assertNull(result.getFirst().delegationState());
        }
    }
}
