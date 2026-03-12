package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.CaseDefinitionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.DecisionDefinitionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.DecisionRequirementsDefinitionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.DeploymentResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.ProcessDefinitionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseDefinitionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.DecisionDefinitionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.DecisionRequirementsDefinitionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.DeploymentQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.ProcessDefinitionQueryDto;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.CaseDefinition;
import org.finos.fluxnova.bpm.engine.repository.CaseDefinitionQuery;
import org.finos.fluxnova.bpm.engine.repository.DecisionDefinition;
import org.finos.fluxnova.bpm.engine.repository.DecisionDefinitionQuery;
import org.finos.fluxnova.bpm.engine.repository.DecisionRequirementsDefinition;
import org.finos.fluxnova.bpm.engine.repository.DecisionRequirementsDefinitionQuery;
import org.finos.fluxnova.bpm.engine.repository.Deployment;
import org.finos.fluxnova.bpm.engine.repository.DeploymentQuery;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinitionQuery;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T empty(Class<T> type) {
        try {
            return MAPPER.readValue("{}", type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private RepositoryService repositoryService;

    private RepositoryQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new RepositoryQueryMcpTools(repositoryService, 200);
    }

    // ========================================================================
    // Process Definition Query Tests
    // ========================================================================

    @Nested
    class QueryProcessDefinitions {

        @Mock(answer = Answers.RETURNS_SELF)
        private ProcessDefinitionQuery query;

        @BeforeEach
        void setUp() {
            when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<ProcessDefinitionResultDto> result = tools.queryProcessDefinitions(empty(ProcessDefinitionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).processDefinitionId(any());
            verify(query, never()).processDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());
            Date deployedAfter = new Date();
            Date deployedAt = new Date();

            ProcessDefinitionQueryDto dto = new ProcessDefinitionQueryDto(
                    "def-1", List.of("def-1", "def-2"),
                    "billing", "bill%",
                    "Invoice Process", "Invoice%",
                    "deploy-1", deployedAfter, deployedAt,
                    "invoiceProcess", "invoice%",
                    2, true,
                    "invoice.bpmn", "%.bpmn",
                    "john",
                    true, true,
                    "inc-1", "failedJob", "Error", "%error%",
                    List.of("t1", "t2"), true, true,
                    "v1.0", "v1%", true,
                    true, true
            );

            tools.queryProcessDefinitions(dto, null);

            verify(query).processDefinitionId("def-1");
            verify(query).processDefinitionIdIn("def-1", "def-2");
            verify(query).processDefinitionCategory("billing");
            verify(query).processDefinitionCategoryLike("bill%");
            verify(query).processDefinitionName("Invoice Process");
            verify(query).processDefinitionNameLike("Invoice%");
            verify(query).deploymentId("deploy-1");
            verify(query).deployedAfter(deployedAfter);
            verify(query).deployedAt(deployedAt);
            verify(query).processDefinitionKey("invoiceProcess");
            verify(query).processDefinitionKeyLike("invoice%");
            verify(query).processDefinitionVersion(2);
            verify(query).latestVersion();
            verify(query).processDefinitionResourceName("invoice.bpmn");
            verify(query).processDefinitionResourceNameLike("%.bpmn");
            verify(query).startableByUser("john");
            verify(query).active();
            verify(query).suspended();
            verify(query).incidentId("inc-1");
            verify(query).incidentType("failedJob");
            verify(query).incidentMessage("Error");
            verify(query).incidentMessageLike("%error%");
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).includeProcessDefinitionsWithoutTenantId();
            verify(query).versionTag("v1.0");
            verify(query).versionTagLike("v1%");
            verify(query).withoutVersionTag();
            verify(query).startableInTasklist();
            verify(query).notStartableInTasklist();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            ProcessDefinitionQueryDto dto = new ProcessDefinitionQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null, null, null, null,
                    null, null,
                    null, false,
                    null, null, null,
                    false, null,
                    null, null, null, null,
                    Collections.emptyList(), null, null,
                    null, null, null,
                    null, null
            );

            tools.queryProcessDefinitions(dto, null);

            verify(query, never()).active();
            verify(query, never()).suspended();
            verify(query, never()).latestVersion();
            verify(query, never()).withoutTenantId();
            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).processDefinitionIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            ProcessDefinition pd = mock(ProcessDefinition.class);
            when(pd.getId()).thenReturn("def:1:abc");
            when(pd.getKey()).thenReturn("invoiceProcess");
            when(pd.getCategory()).thenReturn("billing");
            when(pd.getDescription()).thenReturn("Handles invoices");
            when(pd.getName()).thenReturn("Invoice Process");
            when(pd.getVersion()).thenReturn(3);
            when(pd.getResourceName()).thenReturn("invoice.bpmn");
            when(pd.getDeploymentId()).thenReturn("deploy-1");
            when(pd.getDiagramResourceName()).thenReturn("invoice.png");
            when(pd.isSuspended()).thenReturn(true);
            when(pd.getTenantId()).thenReturn("t1");
            when(pd.getVersionTag()).thenReturn("v1.0");
            when(pd.getHistoryTimeToLive()).thenReturn(180);
            when(pd.isStartableInTasklist()).thenReturn(true);
            when(query.list()).thenReturn(List.of(pd));

            List<ProcessDefinitionResultDto> result = tools.queryProcessDefinitions(empty(ProcessDefinitionQueryDto.class), null);

            assertEquals(1, result.size());
            ProcessDefinitionResultDto r = result.getFirst();
            assertEquals("def:1:abc", r.id());
            assertEquals("invoiceProcess", r.key());
            assertEquals("billing", r.category());
            assertEquals("Handles invoices", r.description());
            assertEquals("Invoice Process", r.name());
            assertEquals(3, r.version());
            assertEquals("invoice.bpmn", r.resourceName());
            assertEquals("deploy-1", r.deploymentId());
            assertEquals("invoice.png", r.diagramResourceName());
            assertTrue(r.suspended());
            assertEquals("t1", r.tenantId());
            assertEquals("v1.0", r.versionTag());
            assertEquals(180, r.historyTimeToLive());
            assertTrue(r.startableInTasklist());
        }
    }

    // ========================================================================
    // Deployment Query Tests
    // ========================================================================

    @Nested
    class QueryDeployments {

        @Mock(answer = Answers.RETURNS_SELF)
        private DeploymentQuery query;

        @BeforeEach
        void setUp() {
            when(repositoryService.createDeploymentQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<DeploymentResultDto> result = tools.queryDeployments(empty(DeploymentQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).deploymentId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());
            Date after = new Date();
            Date before = new Date();

            DeploymentQueryDto dto = new DeploymentQueryDto(
                    "deploy-1", "my-deployment", "my-%",
                    "process-application", null,
                    after, before,
                    List.of("t1", "t2"), true, true
            );

            tools.queryDeployments(dto, null);

            verify(query).deploymentId("deploy-1");
            verify(query).deploymentName("my-deployment");
            verify(query).deploymentNameLike("my-%");
            verify(query).deploymentSource("process-application");
            verify(query).deploymentAfter(after);
            verify(query).deploymentBefore(before);
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).includeDeploymentsWithoutTenantId();
        }

        @Test
        void withoutSource_passesNullToDeploymentSource() {
            when(query.list()).thenReturn(Collections.emptyList());

            DeploymentQueryDto dto = new DeploymentQueryDto(
                    null, null, null, null, true,
                    null, null, null, null, null
            );

            tools.queryDeployments(dto, null);

            verify(query).deploymentSource(null);
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            DeploymentQueryDto dto = new DeploymentQueryDto(
                    null, null, null, null, null,
                    null, null, Collections.emptyList(), false, null
            );

            tools.queryDeployments(dto, null);

            verify(query, never()).withoutTenantId();
            verify(query, never()).deploymentSource(any());
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            Deployment dep = mock(Deployment.class);
            when(dep.getId()).thenReturn("deploy-1");
            when(dep.getName()).thenReturn("my-deployment");
            Date deployTime = new Date();
            when(dep.getDeploymentTime()).thenReturn(deployTime);
            when(dep.getSource()).thenReturn("process-application");
            when(dep.getTenantId()).thenReturn("t1");
            when(query.list()).thenReturn(List.of(dep));

            List<DeploymentResultDto> result = tools.queryDeployments(empty(DeploymentQueryDto.class), null);

            assertEquals(1, result.size());
            DeploymentResultDto r = result.getFirst();
            assertEquals("deploy-1", r.id());
            assertEquals("my-deployment", r.name());
            assertEquals(deployTime, r.deploymentTime());
            assertEquals("process-application", r.source());
            assertEquals("t1", r.tenantId());
        }
    }

    // ========================================================================
    // Case Definition Query Tests
    // ========================================================================

    @Nested
    class QueryCaseDefinitions {

        @Mock(answer = Answers.RETURNS_SELF)
        private CaseDefinitionQuery query;

        @BeforeEach
        void setUp() {
            when(repositoryService.createCaseDefinitionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<CaseDefinitionResultDto> result = tools.queryCaseDefinitions(empty(CaseDefinitionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).caseDefinitionId(any());
            verify(query, never()).caseDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            CaseDefinitionQueryDto dto = new CaseDefinitionQueryDto(
                    "case-1", List.of("case-1", "case-2"),
                    "insurance", "insur%",
                    "Insurance Case", "Insur%",
                    "insuranceCase", "insur%",
                    "deploy-1", 2, true,
                    "insurance.cmmn", "%.cmmn",
                    List.of("t1", "t2"), true, true
            );

            tools.queryCaseDefinitions(dto, null);

            verify(query).caseDefinitionId("case-1");
            verify(query).caseDefinitionIdIn("case-1", "case-2");
            verify(query).caseDefinitionCategory("insurance");
            verify(query).caseDefinitionCategoryLike("insur%");
            verify(query).caseDefinitionName("Insurance Case");
            verify(query).caseDefinitionNameLike("Insur%");
            verify(query).caseDefinitionKey("insuranceCase");
            verify(query).caseDefinitionKeyLike("insur%");
            verify(query).deploymentId("deploy-1");
            verify(query).caseDefinitionVersion(2);
            verify(query).latestVersion();
            verify(query).caseDefinitionResourceName("insurance.cmmn");
            verify(query).caseDefinitionResourceNameLike("%.cmmn");
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).includeCaseDefinitionsWithoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            CaseDefinitionQueryDto dto = new CaseDefinitionQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null, null, null, null,
                    null, false,
                    null, null,
                    Collections.emptyList(), null, null
            );

            tools.queryCaseDefinitions(dto, null);

            verify(query, never()).latestVersion();
            verify(query, never()).withoutTenantId();
            verify(query, never()).caseDefinitionIdIn(any(String[].class));
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            CaseDefinition cd = mock(CaseDefinition.class);
            when(cd.getId()).thenReturn("case:1:abc");
            when(cd.getKey()).thenReturn("insuranceCase");
            when(cd.getCategory()).thenReturn("insurance");
            when(cd.getName()).thenReturn("Insurance Case");
            when(cd.getVersion()).thenReturn(2);
            when(cd.getResourceName()).thenReturn("insurance.cmmn");
            when(cd.getDeploymentId()).thenReturn("deploy-1");
            when(cd.getDiagramResourceName()).thenReturn("insurance.png");
            when(cd.getTenantId()).thenReturn("t1");
            when(cd.getHistoryTimeToLive()).thenReturn(90);
            when(query.list()).thenReturn(List.of(cd));

            List<CaseDefinitionResultDto> result = tools.queryCaseDefinitions(empty(CaseDefinitionQueryDto.class), null);

            assertEquals(1, result.size());
            CaseDefinitionResultDto r = result.getFirst();
            assertEquals("case:1:abc", r.id());
            assertEquals("insuranceCase", r.key());
            assertEquals("insurance", r.category());
            assertEquals("Insurance Case", r.name());
            assertEquals(2, r.version());
            assertEquals("insurance.cmmn", r.resourceName());
            assertEquals("deploy-1", r.deploymentId());
            assertEquals("insurance.png", r.diagramResourceName());
            assertEquals("t1", r.tenantId());
            assertEquals(90, r.historyTimeToLive());
        }
    }

    // ========================================================================
    // Decision Definition Query Tests
    // ========================================================================

    @Nested
    class QueryDecisionDefinitions {

        @Mock(answer = Answers.RETURNS_SELF)
        private DecisionDefinitionQuery query;

        @BeforeEach
        void setUp() {
            when(repositoryService.createDecisionDefinitionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<DecisionDefinitionResultDto> result = tools.queryDecisionDefinitions(empty(DecisionDefinitionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).decisionDefinitionId(any());
            verify(query, never()).decisionDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());
            Date deployedAfter = new Date();
            Date deployedAt = new Date();

            DecisionDefinitionQueryDto dto = new DecisionDefinitionQueryDto(
                    "dec-1", List.of("dec-1", "dec-2"),
                    "finance", "fin%",
                    "Loan Decision", "Loan%",
                    "loanDecision", "loan%",
                    "deploy-1", deployedAfter, deployedAt,
                    3, true,
                    "loan.dmn", "%.dmn",
                    "drd-1", "loanDrd",
                    true,
                    List.of("t1", "t2"), true, true,
                    "v2.0", "v2%"
            );

            tools.queryDecisionDefinitions(dto, null);

            verify(query).decisionDefinitionId("dec-1");
            verify(query).decisionDefinitionIdIn("dec-1", "dec-2");
            verify(query).decisionDefinitionCategory("finance");
            verify(query).decisionDefinitionCategoryLike("fin%");
            verify(query).decisionDefinitionName("Loan Decision");
            verify(query).decisionDefinitionNameLike("Loan%");
            verify(query).decisionDefinitionKey("loanDecision");
            verify(query).decisionDefinitionKeyLike("loan%");
            verify(query).deploymentId("deploy-1");
            verify(query).deployedAfter(deployedAfter);
            verify(query).deployedAt(deployedAt);
            verify(query).decisionDefinitionVersion(3);
            verify(query).latestVersion();
            verify(query).decisionDefinitionResourceName("loan.dmn");
            verify(query).decisionDefinitionResourceNameLike("%.dmn");
            verify(query).decisionRequirementsDefinitionId("drd-1");
            verify(query).decisionRequirementsDefinitionKey("loanDrd");
            verify(query).withoutDecisionRequirementsDefinition();
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).includeDecisionDefinitionsWithoutTenantId();
            verify(query).versionTag("v2.0");
            verify(query).versionTagLike("v2%");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            DecisionDefinitionQueryDto dto = new DecisionDefinitionQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null, null, null, null, null, null,
                    null, false,
                    null, null, null, null,
                    false,
                    Collections.emptyList(), null, null,
                    null, null
            );

            tools.queryDecisionDefinitions(dto, null);

            verify(query, never()).latestVersion();
            verify(query, never()).withoutDecisionRequirementsDefinition();
            verify(query, never()).withoutTenantId();
            verify(query, never()).decisionDefinitionIdIn(any(String[].class));
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            DecisionDefinition dd = mock(DecisionDefinition.class);
            when(dd.getId()).thenReturn("dec:1:abc");
            when(dd.getKey()).thenReturn("loanDecision");
            when(dd.getCategory()).thenReturn("finance");
            when(dd.getName()).thenReturn("Loan Decision");
            when(dd.getVersion()).thenReturn(3);
            when(dd.getResourceName()).thenReturn("loan.dmn");
            when(dd.getDeploymentId()).thenReturn("deploy-1");
            when(dd.getDiagramResourceName()).thenReturn(null);
            when(dd.getTenantId()).thenReturn("t1");
            when(dd.getDecisionRequirementsDefinitionId()).thenReturn("drd-1");
            when(dd.getDecisionRequirementsDefinitionKey()).thenReturn("loanDrd");
            when(dd.getVersionTag()).thenReturn("v2.0");
            when(dd.getHistoryTimeToLive()).thenReturn(365);
            when(query.list()).thenReturn(List.of(dd));

            List<DecisionDefinitionResultDto> result = tools.queryDecisionDefinitions(empty(DecisionDefinitionQueryDto.class), null);

            assertEquals(1, result.size());
            DecisionDefinitionResultDto r = result.getFirst();
            assertEquals("dec:1:abc", r.id());
            assertEquals("loanDecision", r.key());
            assertEquals("finance", r.category());
            assertEquals("Loan Decision", r.name());
            assertEquals(3, r.version());
            assertEquals("loan.dmn", r.resourceName());
            assertEquals("deploy-1", r.deploymentId());
            assertEquals("t1", r.tenantId());
            assertEquals("drd-1", r.decisionRequirementsDefinitionId());
            assertEquals("loanDrd", r.decisionRequirementsDefinitionKey());
            assertEquals("v2.0", r.versionTag());
            assertEquals(365, r.historyTimeToLive());
        }
    }

    // ========================================================================
    // Decision Requirements Definition Query Tests
    // ========================================================================

    @Nested
    class QueryDecisionRequirementsDefinitions {

        @Mock(answer = Answers.RETURNS_SELF)
        private DecisionRequirementsDefinitionQuery query;

        @BeforeEach
        void setUp() {
            when(repositoryService.createDecisionRequirementsDefinitionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<DecisionRequirementsDefinitionResultDto> result = tools.queryDecisionRequirementsDefinitions(
                    empty(DecisionRequirementsDefinitionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).decisionRequirementsDefinitionId(any());
            verify(query, never()).decisionRequirementsDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            DecisionRequirementsDefinitionQueryDto dto = new DecisionRequirementsDefinitionQueryDto(
                    "drd-1", List.of("drd-1", "drd-2"),
                    "finance", "fin%",
                    "Loan DRD", "Loan%",
                    "loanDrd", "loan%",
                    "deploy-1", 2, true,
                    "loan.dmn", "%.dmn",
                    List.of("t1", "t2"), true, true
            );

            tools.queryDecisionRequirementsDefinitions(dto, null);

            verify(query).decisionRequirementsDefinitionId("drd-1");
            verify(query).decisionRequirementsDefinitionIdIn("drd-1", "drd-2");
            verify(query).decisionRequirementsDefinitionCategory("finance");
            verify(query).decisionRequirementsDefinitionCategoryLike("fin%");
            verify(query).decisionRequirementsDefinitionName("Loan DRD");
            verify(query).decisionRequirementsDefinitionNameLike("Loan%");
            verify(query).decisionRequirementsDefinitionKey("loanDrd");
            verify(query).decisionRequirementsDefinitionKeyLike("loan%");
            verify(query).deploymentId("deploy-1");
            verify(query).decisionRequirementsDefinitionVersion(2);
            verify(query).latestVersion();
            verify(query).decisionRequirementsDefinitionResourceName("loan.dmn");
            verify(query).decisionRequirementsDefinitionResourceNameLike("%.dmn");
            verify(query).tenantIdIn("t1", "t2");
            verify(query).withoutTenantId();
            verify(query).includeDecisionRequirementsDefinitionsWithoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            DecisionRequirementsDefinitionQueryDto dto = new DecisionRequirementsDefinitionQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null, null, null, null,
                    null, false,
                    null, null,
                    Collections.emptyList(), null, null
            );

            tools.queryDecisionRequirementsDefinitions(dto, null);

            verify(query, never()).latestVersion();
            verify(query, never()).withoutTenantId();
            verify(query, never()).decisionRequirementsDefinitionIdIn(any(String[].class));
            verify(query, never()).tenantIdIn(any(String[].class));
        }

        @Test
        void resultMapping() {
            DecisionRequirementsDefinition drd = mock(DecisionRequirementsDefinition.class);
            when(drd.getId()).thenReturn("drd:1:abc");
            when(drd.getKey()).thenReturn("loanDrd");
            when(drd.getCategory()).thenReturn("finance");
            when(drd.getName()).thenReturn("Loan DRD");
            when(drd.getVersion()).thenReturn(2);
            when(drd.getResourceName()).thenReturn("loan.dmn");
            when(drd.getDeploymentId()).thenReturn("deploy-1");
            when(drd.getDiagramResourceName()).thenReturn("loan.png");
            when(drd.getTenantId()).thenReturn("t1");
            when(drd.getHistoryTimeToLive()).thenReturn(180);
            when(query.list()).thenReturn(List.of(drd));

            List<DecisionRequirementsDefinitionResultDto> result = tools.queryDecisionRequirementsDefinitions(
                    empty(DecisionRequirementsDefinitionQueryDto.class), null);

            assertEquals(1, result.size());
            DecisionRequirementsDefinitionResultDto r = result.getFirst();
            assertEquals("drd:1:abc", r.id());
            assertEquals("loanDrd", r.key());
            assertEquals("finance", r.category());
            assertEquals("Loan DRD", r.name());
            assertEquals(2, r.version());
            assertEquals("loan.dmn", r.resourceName());
            assertEquals("deploy-1", r.deploymentId());
            assertEquals("loan.png", r.diagramResourceName());
            assertEquals("t1", r.tenantId());
            assertEquals(180, r.historyTimeToLive());
        }
    }
}
