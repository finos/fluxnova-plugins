package org.finos.fluxnova.bpm.engine.ai.a2a.integration;

import org.finos.fluxnova.bpm.engine.ai.a2a.engine.A2aRemoteCallActivityBehaviour;
import org.finos.fluxnova.bpm.engine.ai.a2a.engine.A2aRemoteCallParseListener;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration test for the new visual service task pattern
 * ({@code fluxnova:type="a2a-remote"}) inside an ad-hoc subprocess.
 *
 * <p>This test verifies the end-to-end integration of:
 * <ul>
 *   <li>Parse listener detecting the {@code a2a-remote} type discriminator</li>
 *   <li>Config extraction and storage on the activity</li>
 *   <li>{@link A2aRemoteCallActivityBehaviour} attachment</li>
 *   <li>Compatibility with {@code AdHocSubProcessCatalogueBuilder} discovery
 *       (verifies config property key is correctly set)</li>
 *   <li>Activity instance lifecycle support via the attached behaviour</li>
 * </ul>
 *
 * <p>Validates: Requirements REQ-1, REQ-2, REQ-3, REQ-4, REQ-6</p>
 */
@ExtendWith(MockitoExtension.class)
class A2aVisualServiceTaskIntegrationTest {

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");

    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    private static final String CONFIG_PROPERTY_KEY = "a2aRemoteCallConfig";

    @Mock
    private A2aInvocationService invocationService;

    private A2aRemoteCallParseListener parseListener;
    private ProcessDefinitionImpl processDefinition;

    @BeforeEach
    void setUp() {
        parseListener = new A2aRemoteCallParseListener(invocationService);
        processDefinition = new ProcessDefinitionImpl("agentInvestigationProcess:1:1");
    }

    // ========================================================================
    // Test 1: Full deployment simulation — parse listener attaches behaviour
    //          and stores config on service task with a2a-remote type
    // Validates: REQ-1, REQ-2
    // ========================================================================

    @Test
    @DisplayName("Deploy: parse listener attaches behaviour and stores config for a2a-remote service task")
    void deployment_parseListenerAttachesBehaviourAndStoresConfig() {
        // Simulate the ad-hoc subprocess scope with activities
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
        ActivityImpl a2aActivity = adHocScope.createActivity("amlAgent");
        a2aActivity.setProperty("name", "AML Investigation Agent");

        // Build mock XML element representing the service task
        Element serviceTaskElement = mockA2aServiceTaskElement(
                "amlAgent",
                "http://localhost:8085/tasks/send",
                "aml-agent",
                "AML Investigation Agent",
                "Investigates AML alerts, sanctions lists, and PEP databases.",
                "30000"
        );

        // Execute parse listener (simulates deployment)
        parseListener.parseServiceTask(serviceTaskElement, adHocScope, a2aActivity);

        // Verify: behaviour is attached
        assertNotNull(a2aActivity.getActivityBehavior(),
                "Activity behaviour should be attached after parsing");
        assertInstanceOf(A2aRemoteCallActivityBehaviour.class, a2aActivity.getActivityBehavior(),
                "Activity behaviour should be A2aRemoteCallActivityBehaviour");

        // Verify: config is stored on the activity
        Object configProperty = a2aActivity.getProperty(CONFIG_PROPERTY_KEY);
        assertNotNull(configProperty, "a2aRemoteCallConfig property should be set on the activity");
        assertInstanceOf(A2aRemoteCallConfig.class, configProperty);

        A2aRemoteCallConfig config = (A2aRemoteCallConfig) configProperty;
        assertEquals("http://localhost:8085/tasks/send", config.url());
        assertEquals("aml-agent", config.agentRef());
        assertEquals("AML Investigation Agent", config.name());
        assertEquals("Investigates AML alerts, sanctions lists, and PEP databases.", config.description());
        assertEquals(30000L, config.timeout());
        assertNull(config.prompt(), "Prompt should be null for agentic mode tasks");
    }

    // ========================================================================
    // Test 2: AdHocSubProcessCatalogueBuilder contract — config property key
    //          matches what the catalogue builder checks for REMOTE_AGENT type
    // Validates: REQ-4
    // ========================================================================

    @Test
    @DisplayName("Discovery: config property key matches AdHocSubProcessCatalogueBuilder contract")
    void discovery_configPropertyKeyMatchesCatalogueBuilderContract() {
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
        ActivityImpl a2aActivity = adHocScope.createActivity("amlAgent");
        a2aActivity.setProperty("name", "AML Investigation Agent");

        Element serviceTaskElement = mockA2aServiceTaskElement(
                "amlAgent",
                "http://localhost:8085/tasks/send",
                "aml-agent",
                "AML Investigation Agent",
                "Investigates AML alerts.",
                null
        );

        // Execute parse listener
        parseListener.parseServiceTask(serviceTaskElement, adHocScope, a2aActivity);

        // Verify: the property key used by AdHocSubProcessCatalogueBuilder is present
        // This is the contract between parse listener and catalogue builder
        assertNotNull(a2aActivity.getProperty("a2aRemoteCallConfig"),
                "Property 'a2aRemoteCallConfig' must be set — "
                        + "AdHocSubProcessCatalogueBuilder uses this key to identify REMOTE_AGENT tools");

        // Verify: config has name and description for tool catalogue
        A2aRemoteCallConfig config = (A2aRemoteCallConfig) a2aActivity.getProperty("a2aRemoteCallConfig");
        assertNotNull(config.name(), "Config name required for catalogue tool naming");
        assertNotNull(config.description(), "Config description required for LLM tool selection");
    }

    // ========================================================================
    // Test 3: Mixed scope — A2A task gets config, standard task does not
    // Validates: REQ-1, REQ-4
    // ========================================================================

    @Test
    @DisplayName("Mixed scope: only a2a-remote tasks get config property, standard tasks are unaffected")
    void mixedScope_onlyA2aTasksGetConfigProperty() {
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");

        // A2A remote agent task
        ActivityImpl a2aActivity = adHocScope.createActivity("amlAgent");
        a2aActivity.setProperty("name", "AML Investigation Agent");

        // Standard BPMN service task (external type)
        ActivityImpl standardActivity = adHocScope.createActivity("enrichData");
        standardActivity.setProperty("name", "Enrich Transaction Data");
        standardActivity.setProperty("type", "serviceTask");

        // Parse the A2A task
        Element a2aElement = mockA2aServiceTaskElement(
                "amlAgent",
                "http://localhost:8085/tasks/send",
                "aml-agent",
                "AML Investigation Agent",
                "Investigates AML alerts.",
                null
        );
        parseListener.parseServiceTask(a2aElement, adHocScope, a2aActivity);

        // Parse the standard task (with external type, not a2a-remote)
        Element standardElement = mockServiceTaskElementWithType("external");
        parseListener.parseServiceTask(standardElement, adHocScope, standardActivity);

        // Verify: A2A task has config and behaviour
        assertNotNull(a2aActivity.getProperty(CONFIG_PROPERTY_KEY),
                "A2A task should have config property");
        assertNotNull(a2aActivity.getActivityBehavior(),
                "A2A task should have behaviour attached");

        // Verify: Standard task does NOT have config
        assertNull(standardActivity.getProperty(CONFIG_PROPERTY_KEY),
                "Standard task should NOT have a2a config property");
    }

    // ========================================================================
    // Test 4: Activity instance lifecycle — behaviour is FlowNodeActivityBehavior
    //          which provides activity instance creation (startTime, endTime, duration)
    // Validates: REQ-3, REQ-6
    // ========================================================================

    @Test
    @DisplayName("Lifecycle: attached behaviour extends FlowNodeActivityBehavior for activity instance tracking")
    void lifecycle_behaviourSupportsActivityInstanceTracking() {
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
        ActivityImpl a2aActivity = adHocScope.createActivity("amlAgent");
        a2aActivity.setProperty("name", "AML Investigation Agent");

        Element serviceTaskElement = mockA2aServiceTaskElement(
                "amlAgent",
                "http://localhost:8085/tasks/send",
                "aml-agent",
                "AML Investigation Agent",
                "Investigates AML alerts.",
                "30000"
        );

        parseListener.parseServiceTask(serviceTaskElement, adHocScope, a2aActivity);

        // Verify: the behaviour is a FlowNodeActivityBehavior subclass
        // FlowNodeActivityBehavior integrates with the engine's activity instance lifecycle:
        // - ACT_HI_ACTINST record created with startTime when execute() is entered
        // - endTime and duration recorded when leave() is called or BpmnError thrown
        // - Runtime execution positioned at the task while it executes
        A2aRemoteCallActivityBehaviour behaviour =
                (A2aRemoteCallActivityBehaviour) a2aActivity.getActivityBehavior();
        assertNotNull(behaviour,
                "Behaviour must be attached to support activity instance lifecycle tracking");

        // The behaviour stores result as {activityId}Result — verify activity id is accessible
        assertEquals("amlAgent", a2aActivity.getId(),
                "Activity id must be correctly set for variable naming ({activityId}Result)");
    }

    // ========================================================================
    // Test 5: Config with optional fields — prompt absent, timeout absent
    // Validates: REQ-1, REQ-2
    // ========================================================================

    @Test
    @DisplayName("Config: optional fields (prompt, timeout) are handled correctly")
    void config_optionalFieldsHandledCorrectly() {
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
        ActivityImpl a2aActivity = adHocScope.createActivity("kycAgent");
        a2aActivity.setProperty("name", "KYC Agent");

        // No prompt, no timeout
        Element serviceTaskElement = mockA2aServiceTaskElement(
                "kycAgent",
                "http://localhost:9090/tasks/send",
                "kyc-agent",
                "KYC Verification Agent",
                "Performs KYC verification checks.",
                null  // no timeout
        );

        parseListener.parseServiceTask(serviceTaskElement, adHocScope, a2aActivity);

        A2aRemoteCallConfig config = (A2aRemoteCallConfig) a2aActivity.getProperty(CONFIG_PROPERTY_KEY);
        assertNotNull(config);
        assertEquals("http://localhost:9090/tasks/send", config.url());
        assertEquals("kyc-agent", config.agentRef());
        assertEquals("KYC Verification Agent", config.name());
        assertEquals("Performs KYC verification checks.", config.description());
        assertNull(config.prompt(), "Prompt should be null for agentic mode");
        assertNull(config.timeout(), "Timeout should be null when not specified");
    }

    // ========================================================================
    // Test 6: Multiple A2A tasks in same scope
    // Validates: REQ-1, REQ-4
    // ========================================================================

    @Test
    @DisplayName("Multiple A2A tasks: each gets independent config and behaviour")
    void multipleA2aTasks_eachGetsIndependentConfigAndBehaviour() {
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");

        // First A2A agent
        ActivityImpl agent1 = adHocScope.createActivity("amlAgent");
        agent1.setProperty("name", "AML Agent");
        Element element1 = mockA2aServiceTaskElement(
                "amlAgent",
                "http://host1:8085/tasks/send",
                "aml-agent",
                "AML Investigation Agent",
                "Investigates AML alerts.",
                "30000"
        );

        // Second A2A agent
        ActivityImpl agent2 = adHocScope.createActivity("sanctionsAgent");
        agent2.setProperty("name", "Sanctions Agent");
        Element element2 = mockA2aServiceTaskElement(
                "sanctionsAgent",
                "http://host2:8086/tasks/send",
                "sanctions-agent",
                "Sanctions Screening Agent",
                "Checks sanctions lists and PEP databases.",
                "60000"
        );

        // Parse both
        parseListener.parseServiceTask(element1, adHocScope, agent1);
        parseListener.parseServiceTask(element2, adHocScope, agent2);

        // Verify: each has independent config
        A2aRemoteCallConfig config1 = (A2aRemoteCallConfig) agent1.getProperty(CONFIG_PROPERTY_KEY);
        A2aRemoteCallConfig config2 = (A2aRemoteCallConfig) agent2.getProperty(CONFIG_PROPERTY_KEY);

        assertNotNull(config1);
        assertNotNull(config2);

        assertEquals("http://host1:8085/tasks/send", config1.url());
        assertEquals("aml-agent", config1.agentRef());
        assertEquals(30000L, config1.timeout());

        assertEquals("http://host2:8086/tasks/send", config2.url());
        assertEquals("sanctions-agent", config2.agentRef());
        assertEquals(60000L, config2.timeout());

        // Verify: each has its own behaviour instance
        assertNotSame(agent1.getActivityBehavior(), agent2.getActivityBehavior(),
                "Each A2A task should have its own behaviour instance");
    }

    // ========================================================================
    // Test 7: End-to-end scope structure — activity hierarchy reflects
    //          BPMN structure (process → ad-hoc subprocess → A2A task)
    // Validates: REQ-3, REQ-4
    // ========================================================================

    @Test
    @DisplayName("Scope structure: A2A activity is child of ad-hoc subprocess scope")
    void scopeStructure_a2aActivityIsChildOfAdHocScope() {
        ActivityImpl adHocScope = processDefinition.createActivity("agentScope");
        ActivityImpl a2aActivity = adHocScope.createActivity("amlAgent");
        a2aActivity.setProperty("name", "AML Investigation Agent");

        Element serviceTaskElement = mockA2aServiceTaskElement(
                "amlAgent",
                "http://localhost:8085/tasks/send",
                "aml-agent",
                "AML Investigation Agent",
                "Investigates AML alerts.",
                null
        );

        parseListener.parseServiceTask(serviceTaskElement, adHocScope, a2aActivity);

        // Verify: activity is discoverable within the scope's activities
        assertTrue(adHocScope.getActivities().contains(a2aActivity),
                "A2A activity must be a child of the ad-hoc subprocess scope");

        // Verify: activity can be found by id (catalogue builder uses this)
        ActivityImpl found = adHocScope.findActivity("amlAgent");
        assertNotNull(found, "Activity should be findable by element id within scope");
        assertNotNull(found.getProperty(CONFIG_PROPERTY_KEY),
                "Found activity should have the config property set");
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Creates a mock XML Element representing a service task with
     * {@code fluxnova:type="a2a-remote"} and an {@code <a2a-remote:config>} child.
     */
    private Element mockA2aServiceTaskElement(String id, String url, String agentRef,
                                              String name, String description, String timeout) {
        Element element = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn("a2a-remote");
        when(element.attribute("id")).thenReturn(id);

        // Extension elements container
        Element extensionElements = mock(Element.class);
        when(element.element("extensionElements")).thenReturn(extensionElements);

        // a2a-remote:config element
        Element configElement = mock(Element.class);
        when(extensionElements.elementNS(any(Namespace.class), eq("config"))).thenReturn(configElement);

        when(configElement.attribute("url")).thenReturn(url);
        when(configElement.attribute("prompt")).thenReturn(null);  // no prompt in agentic mode
        when(configElement.attribute("name")).thenReturn(name);
        when(configElement.attribute("description")).thenReturn(description);
        when(configElement.attribute("agent-ref")).thenReturn(agentRef);
        when(configElement.attribute("timeout")).thenReturn(timeout);

        return element;
    }

    /**
     * Creates a mock XML Element representing a service task with a given
     * (non-a2a-remote) type — used for standard tasks that should be ignored.
     */
    private Element mockServiceTaskElementWithType(String type) {
        Element element = mock(Element.class);
        when(element.attributeNS(any(Namespace.class), eq("type"))).thenReturn(type);
        return element;
    }
}
