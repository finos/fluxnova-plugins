package org.finos.fluxnova.bpm.engine.ai.agent.discovery;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AdHocSubProcessCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.*;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.ContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests that wire together real discovery components with a mocked
 * AgentConfigRegistry boundary. Verifies the full flow from BPMN XML to
 * resolved tool catalogues, context specs, and runtime context.
 */
@ExtendWith(MockitoExtension.class)
class DiscoveryIntegrationTest {

    private static final String PROC_DEF_ID = "creditCheck:1:abc";
    private static final String ELEMENT_ID = "creditCheckAgent";
    private static final String EXECUTION_ID = "exec-001";

    private static final String FULL_BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
                         xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
              <process id="creditCheck">
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                  systemPrompt="You are a credit analyst."/>
                    <agent:context>
                      <agent:variable name="customerId"/>
                      <agent:variable name="applicationAmount"/>
                    </agent:context>
                  </extensionElements>
                  <serviceTask id="creditScoreCheck" name="Credit Score Check">
                    <documentation>Retrieves the credit score for the customer.</documentation>
                    <extensionElements>
                      <camunda:inputOutput>
                        <camunda:inputParameter name="cid">${customerId}</camunda:inputParameter>
                        <camunda:outputParameter name="creditScore">${creditScore}</camunda:outputParameter>
                      </camunda:inputOutput>
                    </extensionElements>
                  </serviceTask>
                  <serviceTask id="riskAssessment" name="Risk Assessment">
                    <documentation>Assesses risk based on credit score.</documentation>
                    <extensionElements>
                      <camunda:inputOutput>
                        <camunda:inputParameter name="score">${creditScore}</camunda:inputParameter>
                        <camunda:inputParameter name="amount">${applicationAmount}</camunda:inputParameter>
                        <camunda:outputParameter name="riskLevel">${riskLevel}</camunda:outputParameter>
                      </camunda:inputOutput>
                    </extensionElements>
                  </serviceTask>
                  <serviceTask id="notifyApplicant" name="Notify Applicant">
                    <documentation>Sends notification to applicant.</documentation>
                  </serviceTask>
                </adHocSubProcess>
              </process>
            </definitions>
            """;

    private static final String BPMN_WITH_SEQ_FLOW = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
                         xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
              <process id="p">
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                  systemPrompt="You are a credit analyst."/>
                  </extensionElements>
                  <serviceTask id="taskA" name="Task A"/>
                  <serviceTask id="taskB" name="Task B"/>
                  <serviceTask id="taskC" name="Task C"/>
                  <sequenceFlow id="f1" sourceRef="taskA" targetRef="taskB"/>
                </adHocSubProcess>
              </process>
            </definitions>
            """;

    private static final String BPMN_NO_CONTEXT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
              <process id="p">
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                  systemPrompt="You are a credit analyst."/>
                  </extensionElements>
                  <serviceTask id="taskA" name="Task A"/>
                </adHocSubProcess>
              </process>
            </definitions>
            """;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private AgentConfigRegistry agentConfigRegistry;

    private AgentToolCatalogueRegistry catalogueRegistry;
    private AgentContextSpecRegistry contextSpecRegistry;
    private ContextResolver contextResolver;

    @BeforeEach
    void setUp() {
        catalogueRegistry = new AgentToolCatalogueRegistry(
                repositoryService, agentConfigRegistry, new AdHocSubProcessCatalogueBuilder());
        contextSpecRegistry = new AgentContextSpecRegistry(
                repositoryService, agentConfigRegistry, new AgentContextSpecExtractor());
        contextResolver = new ContextResolver(runtimeService);
    }

    private AgentConfig config() {
        return new AgentConfig(PROC_DEF_ID, ELEMENT_ID, "anthropic", "claude-sonnet-4-6",
                "You are a credit analyst.", ELEMENT_ID);
    }

    private ByteArrayInputStream bpmnStream(String bpmn) {
        return new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- Full end-to-end flow ----------

    @Nested
    class EndToEndFlow {

        @Test
        void fullDiscoveryFlow_catalogueAndContextSpec_resolvedCorrectly() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessModel(PROC_DEF_ID))
                    .thenReturn(bpmnStream(FULL_BPMN))
                    .thenReturn(bpmnStream(FULL_BPMN));

            // Resolve catalogue
            Optional<AgentToolCatalogue> catalogue = catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);
            assertTrue(catalogue.isPresent());
            assertEquals(3, catalogue.get().tools().size());

            // Verify tool details
            AgentToolEntry credit = catalogue.get().findById("creditScoreCheck").orElseThrow();
            assertEquals("Credit Score Check", credit.name());
            assertEquals("Retrieves the credit score for the customer.", credit.description());
            assertEquals(Set.of("customerId"), credit.reads());
            assertEquals(Set.of("creditScore"), credit.writes());

            AgentToolEntry risk = catalogue.get().findById("riskAssessment").orElseThrow();
            assertEquals(Set.of("creditScore", "applicationAmount"), risk.reads());
            assertEquals(Set.of("riskLevel"), risk.writes());

            AgentToolEntry notify = catalogue.get().findById("notifyApplicant").orElseThrow();
            assertTrue(notify.reads().isEmpty());
            assertTrue(notify.writes().isEmpty());

            // Resolve context spec
            Optional<AgentContextSpec> spec = contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);
            assertTrue(spec.isPresent());
            assertEquals(2, spec.get().declaredVariables().size());
        }

        @Test
        void fullFlow_contextResolverUsesSpecToFilterVariables() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream(FULL_BPMN));

            // Resolve spec
            AgentContextSpec spec = contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID).orElseThrow();

            // Set up runtime variables
            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("customerId", "C-001");
            vars.put("applicationAmount", 50000);
            vars.put("creditScore", 720);
            vars.put("internalFlag", true);
            vars.put("_agentState", "RUNNING");
            when(runtimeService.getVariables(EXECUTION_ID)).thenReturn(vars);

            // Resolve context
            ResolvedContext resolved = contextResolver.resolve(EXECUTION_ID, spec);

            // Only declared variables, minus _agent prefixed
            assertEquals(2, resolved.variables().size());
            assertEquals("C-001", resolved.variables().get("customerId"));
            assertEquals(50000, resolved.variables().get("applicationAmount"));
            assertFalse(resolved.variables().containsKey("creditScore"));
            assertFalse(resolved.variables().containsKey("internalFlag"));
            assertFalse(resolved.variables().containsKey("_agentState"));
        }

        @Test
        void fullFlow_noContextDeclaration_exposesAllNonAgentVars() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream(BPMN_NO_CONTEXT));

            AgentContextSpec spec = contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID).orElseThrow();
            assertTrue(spec.declaredVariables().isEmpty());

            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("customerId", "C-001");
            vars.put("creditScore", 720);
            vars.put("_agentState", "RUNNING");
            when(runtimeService.getVariables(EXECUTION_ID)).thenReturn(vars);

            ResolvedContext resolved = contextResolver.resolve(EXECUTION_ID, spec);

            assertEquals(2, resolved.variables().size());
            assertTrue(resolved.variables().containsKey("customerId"));
            assertTrue(resolved.variables().containsKey("creditScore"));
            assertFalse(resolved.variables().containsKey("_agentState"));
        }
    }

    // ---------- Sequence flow filtering integration ----------

    @Nested
    class SequenceFlowFiltering {

        @Test
        void sequenceFlowTargetsExcluded_fromCatalogue() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream(BPMN_WITH_SEQ_FLOW));

            AgentToolCatalogue catalogue = catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID).orElseThrow();

            assertEquals(2, catalogue.tools().size());
            assertTrue(catalogue.findById("taskA").isPresent());
            assertTrue(catalogue.findById("taskC").isPresent());
            assertTrue(catalogue.findById("taskB").isEmpty());
        }
    }

    // ---------- Lazy scan caching integration ----------

    @Nested
    class LazyScanCaching {

        @Test
        void bothRegistries_cacheIndependently() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessModel(PROC_DEF_ID))
                    .thenReturn(bpmnStream(FULL_BPMN))
                    .thenReturn(bpmnStream(FULL_BPMN));

            // First resolve on each triggers scan
            catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);
            contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);

            // Second resolve on each should not trigger additional scans
            catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);
            contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);

            // 2 calls total: one per registry
            verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
        }

        @Test
        void unregisterAll_onBothRegistries_allowsRescanning() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessModel(PROC_DEF_ID))
                    .thenReturn(bpmnStream(FULL_BPMN))
                    .thenReturn(bpmnStream(FULL_BPMN))
                    .thenReturn(bpmnStream(FULL_BPMN))
                    .thenReturn(bpmnStream(FULL_BPMN));

            catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);
            contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);

            catalogueRegistry.unregisterAll();
            contextSpecRegistry.unregisterAll();

            catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);
            contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID);

            verify(repositoryService, times(4)).getProcessModel(PROC_DEF_ID);
        }
    }

    // ---------- Non-agentic subprocess integration ----------

    @Nested
    class NonAgenticSubprocess {

        @Test
        void resolve_nonAgenticSubprocess_returnsEmptyForBoth() {
            when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());

            assertTrue(catalogueRegistry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
            assertTrue(contextSpecRegistry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
        }
    }

    // ---------- Multiple agents in same definition ----------

    @Nested
    class MultipleAgents {

        private static final String BPMN_TWO_AGENTS = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
                             xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                  <process id="p">
                    <adHocSubProcess id="agentA">
                      <extensionElements>
                        <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                      systemPrompt="Agent A"/>
                        <agent:context>
                          <agent:variable name="varA"/>
                        </agent:context>
                      </extensionElements>
                      <serviceTask id="toolA1" name="Tool A1"/>
                    </adHocSubProcess>
                    <adHocSubProcess id="agentB">
                      <extensionElements>
                        <agent:config provider="openai" model="gpt-4o"
                                      systemPrompt="Agent B"/>
                      </extensionElements>
                      <serviceTask id="toolB1" name="Tool B1"/>
                      <serviceTask id="toolB2" name="Tool B2"/>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        @Test
        void twoAgents_resolvedIndependently() {
            AgentConfig configA = new AgentConfig(PROC_DEF_ID, "agentA", "anthropic",
                    "claude-sonnet-4-6", "Agent A", "agentA");
            AgentConfig configB = new AgentConfig(PROC_DEF_ID, "agentB", "openai",
                    "gpt-4o", "Agent B", "agentB");

            when(agentConfigRegistry.resolve(PROC_DEF_ID, "agentA")).thenReturn(Optional.of(configA));
            when(agentConfigRegistry.resolve(PROC_DEF_ID, "agentB")).thenReturn(Optional.of(configB));
            when(repositoryService.getProcessModel(PROC_DEF_ID))
                    .thenReturn(bpmnStream(BPMN_TWO_AGENTS))
                    .thenReturn(bpmnStream(BPMN_TWO_AGENTS))
                    .thenReturn(bpmnStream(BPMN_TWO_AGENTS))
                    .thenReturn(bpmnStream(BPMN_TWO_AGENTS));

            // Agent A
            AgentToolCatalogue catA = catalogueRegistry.resolve(PROC_DEF_ID, "agentA").orElseThrow();
            assertEquals(1, catA.tools().size());
            assertEquals("toolA1", catA.tools().get(0).elementId());

            AgentContextSpec specA = contextSpecRegistry.resolve(PROC_DEF_ID, "agentA").orElseThrow();
            assertEquals(1, specA.declaredVariables().size());
            assertEquals("varA", specA.declaredVariables().get(0).name());

            // Agent B
            AgentToolCatalogue catB = catalogueRegistry.resolve(PROC_DEF_ID, "agentB").orElseThrow();
            assertEquals(2, catB.tools().size());

            AgentContextSpec specB = contextSpecRegistry.resolve(PROC_DEF_ID, "agentB").orElseThrow();
            assertTrue(specB.declaredVariables().isEmpty());
        }
    }
}
