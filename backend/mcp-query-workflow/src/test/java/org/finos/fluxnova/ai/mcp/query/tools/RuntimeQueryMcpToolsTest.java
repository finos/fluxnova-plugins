package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuntimeQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T empty(Class<T> type) {
        try {
            return MAPPER.readValue("{}", type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private RuntimeService runtimeService;

    private RuntimeQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new RuntimeQueryMcpTools(runtimeService, 200);
    }

    // ========================================================================
    // Process Instance Query Tests
    // ========================================================================

    @Nested
    class QueryProcessInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private ProcessInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<ProcessInstanceResultDto> result = tools.queryProcessInstances(empty(ProcessInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(anyInt(), anyInt());
            verify(query, never()).processInstanceId(any());
            verify(query, never()).processDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            ProcessInstanceQueryDto dto = new ProcessInstanceQueryDto(
                    "pi-1", List.of("pi-1", "pi-2"),
                    "order-123", "order-%",
                    "myProcess", List.of("proc1", "proc2"), List.of("excluded1"),
                    "def:1:abc", "deploy-1",
                    "super-1", "sub-1", "case-1", "super-case-1", "sub-case-1",
                    true, true, true,
                    "inc-1", "failedJob", "Something went wrong", "%error%",
                    List.of("t1", "t2"), true, true,
                    List.of("act1", "act2"), true, true, true, true
            );

            tools.queryProcessInstances(dto, null);

            verify(query).processInstanceId("pi-1");
            verify(query).processInstanceIds(Set.of("pi-1", "pi-2"));
            verify(query).processInstanceBusinessKey("order-123");
            verify(query).processInstanceBusinessKeyLike("order-%");
            verify(query).processDefinitionKey("myProcess");
            verify(query).processDefinitionKeyIn("proc1", "proc2");
            verify(query).processDefinitionKeyNotIn("excluded1");
            verify(query).processDefinitionId("def:1:abc");
            verify(query).deploymentId("deploy-1");
            verify(query).superProcessInstanceId("super-1");
            verify(query).subProcessInstanceId("sub-1");
            verify(query).caseInstanceId("case-1");
            verify(query).superCaseInstanceId("super-case-1");
            verify(query).subCaseInstanceId("sub-case-1");
            verify(query).active();
            verify(query).suspended();
            verify(query).withIncident();
            verify(query).incidentId("inc-1");
            verify(query).incidentType("failedJob");
            verify(query).incidentMessage("Something went wrong");
            verify(query).incidentMessageLike("%error%");
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).processDefinitionWithoutTenantId();
            verify(query).activityIdIn("act1", "act2");
            verify(query).rootProcessInstances();
            verify(query).leafProcessInstances();
            verify(query).matchVariableNamesIgnoreCase();
            verify(query).matchVariableValuesIgnoreCase();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            ProcessInstanceQueryDto dto = new ProcessInstanceQueryDto(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null,
                    false, null, false,
                    null, null, null, null,
                    Collections.emptyList(), null, null, null, null, null, null, null
            );

            tools.queryProcessInstances(dto, null);

            verify(query, never()).active();
            verify(query, never()).suspended();
            verify(query, never()).withIncident();
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            ProcessInstance pi = mockProcessInstance("pi-1", "def:1:abc", "order-123", "root-1", "case-1", true, "tenant-a");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(pi));

            List<ProcessInstanceResultDto> result = tools.queryProcessInstances(empty(ProcessInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            ProcessInstanceResultDto r = result.getFirst();
            assertEquals("pi-1", r.id());
            assertEquals("def:1:abc", r.processDefinitionId());
            assertEquals("order-123", r.businessKey());
            assertEquals("root-1", r.rootProcessInstanceId());
            assertEquals("case-1", r.caseInstanceId());
            assertTrue(r.suspended());
            assertEquals("tenant-a", r.tenantId());
        }

        @Test
        void multipleResults() {
            ProcessInstance pi1 = mockProcessInstance("pi-1", "def-1", null, null, null, false, null);
            ProcessInstance pi2 = mockProcessInstance("pi-2", "def-2", "bk-2", null, null, true, "t1");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(pi1, pi2));

            List<ProcessInstanceResultDto> result = tools.queryProcessInstances(empty(ProcessInstanceQueryDto.class), null);

            assertEquals(2, result.size());
            assertEquals("pi-1", result.getFirst().id());
            assertEquals("pi-2", result.get(1).id());
        }
    }

    // ========================================================================
    // Execution Query Tests
    // ========================================================================

    @Nested
    class QueryExecutions {

        @Mock(answer = Answers.RETURNS_SELF)
        private ExecutionQuery query;

        @BeforeEach
        void setUp() {
            when(runtimeService.createExecutionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<ExecutionResultDto> result = tools.queryExecutions(empty(ExecutionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(anyInt(), anyInt());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            ExecutionQueryDto dto = new ExecutionQueryDto(
                    "exec-1", "pi-1", "bk-1", "def-1", "myProcess", "task1",
                    "mySignal", "myMessage",
                    true, true,
                    "inc-1", "failedJob", "Error", "%error%",
                    List.of("t1"), true, true, true
            );

            tools.queryExecutions(dto, null);

            verify(query).executionId("exec-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processInstanceBusinessKey("bk-1");
            verify(query).processDefinitionId("def-1");
            verify(query).processDefinitionKey("myProcess");
            verify(query).activityId("task1");
            verify(query).signalEventSubscriptionName("mySignal");
            verify(query).messageEventSubscriptionName("myMessage");
            verify(query).active();
            verify(query).suspended();
            verify(query).incidentId("inc-1");
            verify(query).incidentType("failedJob");
            verify(query).incidentMessage("Error");
            verify(query).incidentMessageLike("%error%");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).matchVariableNamesIgnoreCase();
            verify(query).matchVariableValuesIgnoreCase();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            ExecutionQueryDto dto = new ExecutionQueryDto(
                    null, null, null, null, null, null, null, null,
                    false, null,
                    null, null, null, null, null, null, null, null
            );

            tools.queryExecutions(dto, null);

            verify(query, never()).active();
            verify(query, never()).suspended();
        }

        @Test
        void resultMapping() {
            Execution exec = mock(Execution.class);
            when(exec.getId()).thenReturn("exec-1");
            when(exec.getProcessInstanceId()).thenReturn("pi-1");
            when(exec.isSuspended()).thenReturn(false);
            when(exec.isEnded()).thenReturn(false);
            when(exec.getTenantId()).thenReturn("t1");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(exec));

            List<ExecutionResultDto> result = tools.queryExecutions(empty(ExecutionQueryDto.class), null);

            assertEquals(1, result.size());
            assertEquals("exec-1", result.getFirst().id());
            assertEquals("pi-1", result.getFirst().processInstanceId());
            assertFalse(result.getFirst().suspended());
            assertEquals("t1", result.getFirst().tenantId());
        }
    }

    // ========================================================================
    // Incident Query Tests
    // ========================================================================

    @Nested
    class QueryIncidents {

        @Mock(answer = Answers.RETURNS_SELF)
        private IncidentQuery query;

        @BeforeEach
        void setUp() {
            when(runtimeService.createIncidentQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<IncidentResultDto> result = tools.queryIncidents(empty(IncidentQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(anyInt(), anyInt());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());
            Date before = new Date();
            Date after = new Date();

            IncidentQueryDto dto = new IncidentQueryDto(
                    "inc-1", "failedJob", "Error", "%error%",
                    "def-1", List.of("proc1", "proc2"),
                    "pi-1", "exec-1",
                    before, after,
                    "act-1", "failed-act-1",
                    "cause-1", "root-cause-1", "config-1",
                    List.of("t1"), List.of("jd-1", "jd-2")
            );

            tools.queryIncidents(dto, null);

            verify(query).incidentId("inc-1");
            verify(query).incidentType("failedJob");
            verify(query).incidentMessage("Error");
            verify(query).incidentMessageLike("%error%");
            verify(query).processDefinitionId("def-1");
            verify(query).processDefinitionKeyIn("proc1", "proc2");
            verify(query).processInstanceId("pi-1");
            verify(query).executionId("exec-1");
            verify(query).incidentTimestampBefore(before);
            verify(query).incidentTimestampAfter(after);
            verify(query).activityId("act-1");
            verify(query).failedActivityId("failed-act-1");
            verify(query).causeIncidentId("cause-1");
            verify(query).rootCauseIncidentId("root-cause-1");
            verify(query).configuration("config-1");
            verify(query).tenantIdIn("t1");
            verify(query).jobDefinitionIdIn("jd-1", "jd-2");
        }

        @Test
        void resultMapping() {
            Incident incident = mock(Incident.class);
            when(incident.getId()).thenReturn("inc-1");
            when(incident.getIncidentType()).thenReturn("failedJob");
            when(incident.getIncidentMessage()).thenReturn("Error occurred");
            when(incident.getProcessInstanceId()).thenReturn("pi-1");
            when(incident.getExecutionId()).thenReturn("exec-1");
            when(incident.getActivityId()).thenReturn("serviceTask1");
            when(incident.getProcessDefinitionId()).thenReturn("def-1");
            when(incident.getTenantId()).thenReturn("t1");
            Date timestamp = new Date();
            when(incident.getIncidentTimestamp()).thenReturn(timestamp);
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(incident));

            List<IncidentResultDto> result = tools.queryIncidents(empty(IncidentQueryDto.class), null);

            assertEquals(1, result.size());
            assertEquals("inc-1", result.getFirst().id());
            assertEquals("failedJob", result.getFirst().incidentType());
            assertEquals("Error occurred", result.getFirst().incidentMessage());
            assertEquals("pi-1", result.getFirst().processInstanceId());
        }
    }

    // ========================================================================
    // Event Subscription Query Tests
    // ========================================================================

    @Nested
    class QueryEventSubscriptions {

        @Mock(answer = Answers.RETURNS_SELF)
        private EventSubscriptionQuery query;

        @BeforeEach
        void setUp() {
            when(runtimeService.createEventSubscriptionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<EventSubscriptionResultDto> result = tools.queryEventSubscriptions(empty(EventSubscriptionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(anyInt(), anyInt());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            EventSubscriptionQueryDto dto = new EventSubscriptionQueryDto(
                    "es-1", "myEvent", "message",
                    "exec-1", "pi-1", "act-1",
                    List.of("t1", "t2"), true, true
            );

            tools.queryEventSubscriptions(dto, null);

            verify(query).eventSubscriptionId("es-1");
            verify(query).eventName("myEvent");
            verify(query).eventType("message");
            verify(query).executionId("exec-1");
            verify(query).processInstanceId("pi-1");
            verify(query).activityId("act-1");
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).includeEventSubscriptionsWithoutTenantId();
        }

        @Test
        void resultMapping() {
            EventSubscription es = mock(EventSubscription.class);
            when(es.getId()).thenReturn("es-1");
            when(es.getEventType()).thenReturn("message");
            when(es.getEventName()).thenReturn("orderReceived");
            when(es.getExecutionId()).thenReturn("exec-1");
            when(es.getProcessInstanceId()).thenReturn("pi-1");
            when(es.getActivityId()).thenReturn("receiveTask1");
            when(es.getTenantId()).thenReturn("t1");
            Date created = new Date();
            when(es.getCreated()).thenReturn(created);
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(es));

            List<EventSubscriptionResultDto> result = tools.queryEventSubscriptions(empty(EventSubscriptionQueryDto.class), null);

            assertEquals(1, result.size());
            assertEquals("es-1", result.getFirst().id());
            assertEquals("message", result.getFirst().eventType());
            assertEquals("orderReceived", result.getFirst().eventName());
        }
    }

    // ========================================================================
    // Variable Instance Query Tests
    // ========================================================================

    @Nested
    class QueryVariableInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private VariableInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(runtimeService.createVariableInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters_andDisablesBinaryFetching() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<VariableInstanceResultDto> result = tools.queryVariableInstances(empty(VariableInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).disableBinaryFetching();
            verify(query).listPage(anyInt(), anyInt());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            VariableInstanceQueryDto dto = new VariableInstanceQueryDto(
                    "orderId", "order%",
                    List.of("pi-1", "pi-2"), List.of("exec-1"),
                    List.of("case-1"), List.of("ce-1"),
                    List.of("task-1"), List.of("batch-1"),
                    List.of("ai-1"), List.of("t1"),
                    List.of("orderId", "status"), List.of("scope-1"),
                    true, true
            );

            tools.queryVariableInstances(dto, null);

            verify(query).variableName("orderId");
            verify(query).variableNameLike("order%");
            verify(query).variableNameIn("orderId", "status");
            verify(query).processInstanceIdIn("pi-1", "pi-2");
            verify(query).executionIdIn("exec-1");
            verify(query).caseInstanceIdIn("case-1");
            verify(query).caseExecutionIdIn("ce-1");
            verify(query).taskIdIn("task-1");
            verify(query).batchIdIn("batch-1");
            verify(query).activityInstanceIdIn("ai-1");
            verify(query).variableScopeIdIn("scope-1");
            verify(query).tenantIdIn("t1");
            verify(query).matchVariableNamesIgnoreCase();
            verify(query).matchVariableValuesIgnoreCase();
        }

        @Test
        void emptyLists_doNotApplyFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            VariableInstanceQueryDto dto = new VariableInstanceQueryDto(
                    null, null,
                    Collections.emptyList(), Collections.emptyList(),
                    null, null,
                    Collections.emptyList(), null, null,
                    Collections.emptyList(),
                    null, null, null, null
            );

            tools.queryVariableInstances(dto, null);

            verify(query, never()).processInstanceIdIn(any(String[].class));
            verify(query, never()).executionIdIn(any(String[].class));
            verify(query, never()).taskIdIn(any(String[].class));
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            VariableInstance var = mock(VariableInstance.class);
            when(var.getId()).thenReturn("var-1");
            when(var.getName()).thenReturn("orderId");
            when(var.getValue()).thenReturn("ORD-123");
            when(var.getTypeName()).thenReturn("string");
            when(var.getProcessInstanceId()).thenReturn("pi-1");
            when(var.getExecutionId()).thenReturn("exec-1");
            when(var.getTenantId()).thenReturn("t1");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(var));

            List<VariableInstanceResultDto> result = tools.queryVariableInstances(empty(VariableInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            assertEquals("var-1", result.getFirst().id());
            assertEquals("orderId", result.getFirst().name());
            assertEquals("ORD-123", result.getFirst().value());
            assertEquals("string", result.getFirst().typeName());
            assertEquals("pi-1", result.getFirst().processInstanceId());
        }

        @Test
        void resultMapping_variableValueError() {
            VariableInstance var = mock(VariableInstance.class);
            when(var.getId()).thenReturn("var-1");
            when(var.getName()).thenReturn("binaryData");
            when(var.getValue()).thenThrow(new RuntimeException("Cannot deserialize"));
            when(var.getTypeName()).thenReturn("bytes");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(var));

            List<VariableInstanceResultDto> result = tools.queryVariableInstances(empty(VariableInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            assertNull(result.getFirst().value());
            assertTrue(result.getFirst().errorMessage().contains("Cannot deserialize"));
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private ProcessInstance mockProcessInstance(String id, String defId, String businessKey,
                                                String rootId, String caseId, boolean suspended, String tenantId) {
        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn(id);
        when(pi.getProcessDefinitionId()).thenReturn(defId);
        when(pi.getBusinessKey()).thenReturn(businessKey);
        when(pi.getRootProcessInstanceId()).thenReturn(rootId);
        when(pi.getCaseInstanceId()).thenReturn(caseId);
        when(pi.isSuspended()).thenReturn(suspended);
        when(pi.getTenantId()).thenReturn(tenantId);
        return pi;
    }

}
