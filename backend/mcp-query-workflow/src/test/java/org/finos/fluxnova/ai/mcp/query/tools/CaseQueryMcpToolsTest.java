package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.CaseExecutionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.CaseInstanceResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseExecutionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseInstanceQueryDto;
import org.finos.fluxnova.bpm.engine.CaseService;
import org.finos.fluxnova.bpm.engine.runtime.CaseExecution;
import org.finos.fluxnova.bpm.engine.runtime.CaseExecutionQuery;
import org.finos.fluxnova.bpm.engine.runtime.CaseInstance;
import org.finos.fluxnova.bpm.engine.runtime.CaseInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T empty(Class<T> type) {
        try {
            return MAPPER.readValue("{}", type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private CaseService caseService;

    private CaseQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new CaseQueryMcpTools(caseService, 200);
    }

    @Nested
    class QueryCaseInstances {

        @Mock(answer = Answers.RETURNS_SELF)
        private CaseInstanceQuery query;

        @BeforeEach
        void setUp() {
            when(caseService.createCaseInstanceQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<CaseInstanceResultDto> result = tools.queryCaseInstances(empty(CaseInstanceQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(anyInt(), anyInt());
            verify(query, never()).caseInstanceId(any());
            verify(query, never()).caseDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            CaseInstanceQueryDto dto = new CaseInstanceQueryDto(
                    "ci-1", "bk-1", "loanCase", "caseDef:1",
                    "deploy-1", "super-pi-1", "sub-pi-1",
                    "super-ci-1", "sub-ci-1",
                    true, true, true,
                    List.of("t1", "t2"), true
            );

            tools.queryCaseInstances(dto, null);

            verify(query).caseInstanceId("ci-1");
            verify(query).caseInstanceBusinessKey("bk-1");
            verify(query).caseDefinitionKey("loanCase");
            verify(query).caseDefinitionId("caseDef:1");
            verify(query).deploymentId("deploy-1");
            verify(query).superProcessInstanceId("super-pi-1");
            verify(query).subProcessInstanceId("sub-pi-1");
            verify(query).superCaseInstanceId("super-ci-1");
            verify(query).subCaseInstanceId("sub-ci-1");
            verify(query).active();
            verify(query).completed();
            verify(query).terminated();
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            CaseInstanceQueryDto dto = new CaseInstanceQueryDto(
                    null, null, null, null,
                    null, null, null, null, null,
                    false, null, false,
                    Collections.emptyList(), null
            );

            tools.queryCaseInstances(dto, null);

            verify(query, never()).active();
            verify(query, never()).completed();
            verify(query, never()).terminated();
            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            CaseInstance ci = mock(CaseInstance.class);
            when(ci.getId()).thenReturn("ci-1");
            when(ci.getCaseInstanceId()).thenReturn("ci-1");
            when(ci.getCaseDefinitionId()).thenReturn("caseDef:1");
            when(ci.getBusinessKey()).thenReturn("bk-1");
            when(ci.getParentId()).thenReturn(null);
            when(ci.isActive()).thenReturn(true);
            when(ci.isCompleted()).thenReturn(false);
            when(ci.isTerminated()).thenReturn(false);
            when(ci.getTenantId()).thenReturn("t1");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(ci));

            List<CaseInstanceResultDto> result = tools.queryCaseInstances(empty(CaseInstanceQueryDto.class), null);

            assertEquals(1, result.size());
            CaseInstanceResultDto r = result.getFirst();
            assertEquals("ci-1", r.id());
            assertEquals("ci-1", r.caseInstanceId());
            assertEquals("caseDef:1", r.caseDefinitionId());
            assertEquals("bk-1", r.businessKey());
            assertNull(r.parentId());
            assertTrue(r.active());
            assertFalse(r.completed());
            assertFalse(r.terminated());
            assertEquals("t1", r.tenantId());
        }
    }

    @Nested
    class QueryCaseExecutions {

        @Mock(answer = Answers.RETURNS_SELF)
        private CaseExecutionQuery query;

        @BeforeEach
        void setUp() {
            when(caseService.createCaseExecutionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            List<CaseExecutionResultDto> result = tools.queryCaseExecutions(empty(CaseExecutionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).listPage(anyInt(), anyInt());
            verify(query, never()).caseExecutionId(any());
            verify(query, never()).activityId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            CaseExecutionQueryDto dto = new CaseExecutionQueryDto(
                    "exec-1", "ci-1", "bk-1",
                    "caseDef:1", "loanCase", "humanTask1",
                    true, true, true, true, true,
                    List.of("t1"), true
            );

            tools.queryCaseExecutions(dto, null);

            verify(query).caseExecutionId("exec-1");
            verify(query).caseInstanceId("ci-1");
            verify(query).caseInstanceBusinessKey("bk-1");
            verify(query).caseDefinitionId("caseDef:1");
            verify(query).caseDefinitionKey("loanCase");
            verify(query).activityId("humanTask1");
            verify(query).required();
            verify(query).available();
            verify(query).enabled();
            verify(query).active();
            verify(query).disabled();
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            CaseExecutionQueryDto dto = new CaseExecutionQueryDto(
                    null, null, null, null, null, null,
                    false, null, false, null, false,
                    Collections.emptyList(), null
            );

            tools.queryCaseExecutions(dto, null);

            verify(query, never()).required();
            verify(query, never()).available();
            verify(query, never()).enabled();
            verify(query, never()).active();
            verify(query, never()).disabled();
            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).withoutTenantId();
        }

        @Test
        void resultMapping() {
            CaseExecution ce = mock(CaseExecution.class);
            when(ce.getId()).thenReturn("exec-1");
            when(ce.getCaseInstanceId()).thenReturn("ci-1");
            when(ce.getCaseDefinitionId()).thenReturn("caseDef:1");
            when(ce.getActivityId()).thenReturn("humanTask1");
            when(ce.getActivityName()).thenReturn("Review Loan Application");
            when(ce.getActivityType()).thenReturn("humanTask");
            when(ce.getParentId()).thenReturn("ci-1");
            when(ce.isRequired()).thenReturn(false);
            when(ce.isAvailable()).thenReturn(false);
            when(ce.isActive()).thenReturn(true);
            when(ce.isEnabled()).thenReturn(false);
            when(ce.isDisabled()).thenReturn(false);
            when(ce.isTerminated()).thenReturn(false);
            when(ce.getTenantId()).thenReturn("t1");
            when(query.listPage(anyInt(), anyInt())).thenReturn(List.of(ce));

            List<CaseExecutionResultDto> result = tools.queryCaseExecutions(empty(CaseExecutionQueryDto.class), null);

            assertEquals(1, result.size());
            CaseExecutionResultDto r = result.getFirst();
            assertEquals("exec-1", r.id());
            assertEquals("ci-1", r.caseInstanceId());
            assertEquals("caseDef:1", r.caseDefinitionId());
            assertEquals("humanTask1", r.activityId());
            assertEquals("Review Loan Application", r.activityName());
            assertEquals("humanTask", r.activityType());
            assertEquals("ci-1", r.parentId());
            assertFalse(r.required());
            assertFalse(r.available());
            assertTrue(r.active());
            assertFalse(r.enabled());
            assertFalse(r.disabled());
            assertFalse(r.terminated());
            assertEquals("t1", r.tenantId());
        }
    }
}
