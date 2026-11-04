package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolType;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Catalogue Type Discrimination.
 *
 * <p>For any ad-hoc subprocess scope with a mix of activities (some with
 * {@code a2aRemoteCallConfig} property, some without): the builder produces a
 * catalogue where A2A activities have {@code REMOTE_AGENT} type and non-A2A
 * activities have {@code BPMN_ACTIVITY} type. Tool name is derived from config
 * name or activity name; description from config description or documentation.</p>
 *
 * <p><b>Validates: Requirements REQ-4</b></p>
 */
@Tag("Feature: a2a-visual-service-task, Property 4: Catalogue Type Discrimination")
class AdHocCatalogueTypeDiscriminationPropertyTest {

    private final AdHocSubProcessCatalogueBuilder builder = new AdHocSubProcessCatalogueBuilder();

    // ========================================================================
    // Property 4: Catalogue Type Discrimination
    //
    // For any ad-hoc subprocess scope containing a mix of standard BPMN
    // activities and A2A service tasks (those with a2aRemoteCallConfig property
    // set), the builder SHALL produce an AgentToolCatalogue where each A2A
    // activity has type = REMOTE_AGENT and each non-A2A activity has
    // type = BPMN_ACTIVITY.
    // ========================================================================

    /**
     * Activities with an {@code a2aRemoteCallConfig} property MUST produce
     * entries with {@code type = REMOTE_AGENT}.
     *
     * <p><b>Validates: Requirements REQ-4</b></p>
     */
    @Property(tries = 100)
    void a2aActivities_haveRemoteAgentType(
            @ForAll("a2aActivitySpecs") List<A2aActivitySpec> a2aSpecs) {

        Assume.that(!a2aSpecs.isEmpty());

        ProcessDefinitionImpl procDef = new ProcessDefinitionImpl("proc:test");
        ActivityImpl scope = procDef.createActivity("adHocScope");

        for (int i = 0; i < a2aSpecs.size(); i++) {
            A2aActivitySpec spec = a2aSpecs.get(i);
            ActivityImpl activity = scope.createActivity("a2a_" + i);
            activity.setProperty("type", "serviceTask");
            activity.setProperty("name", spec.activityName());
            activity.setProperty("documentation", spec.activityDocumentation());
            activity.setProperty("a2aRemoteCallConfig",
                    new A2aRemoteCallConfig(
                            spec.configUrl(), null,
                            spec.configName(), spec.configDescription(),
                            spec.agentRef()));
        }

        AgentToolCatalogue catalogue = builder.build(scope);

        assertEquals(a2aSpecs.size(), catalogue.tools().size());
        for (AgentToolEntry entry : catalogue.tools()) {
            assertEquals(AgentToolType.REMOTE_AGENT, entry.type(),
                    "A2A activity '" + entry.elementId() + "' must have REMOTE_AGENT type");
        }
    }

    /**
     * Activities WITHOUT an {@code a2aRemoteCallConfig} property MUST produce
     * entries with {@code type = BPMN_ACTIVITY}.
     *
     * <p><b>Validates: Requirements REQ-4</b></p>
     */
    @Property(tries = 100)
    void nonA2aActivities_haveBpmnActivityType(
            @ForAll("bpmnActivitySpecs") List<BpmnActivitySpec> bpmnSpecs) {

        Assume.that(!bpmnSpecs.isEmpty());

        ProcessDefinitionImpl procDef = new ProcessDefinitionImpl("proc:test");
        ActivityImpl scope = procDef.createActivity("adHocScope");

        for (int i = 0; i < bpmnSpecs.size(); i++) {
            BpmnActivitySpec spec = bpmnSpecs.get(i);
            ActivityImpl activity = scope.createActivity("bpmn_" + i);
            activity.setProperty("type", "serviceTask");
            activity.setProperty("name", spec.name());
            activity.setProperty("documentation", spec.documentation());
            // No a2aRemoteCallConfig property set
        }

        AgentToolCatalogue catalogue = builder.build(scope);

        assertEquals(bpmnSpecs.size(), catalogue.tools().size());
        for (AgentToolEntry entry : catalogue.tools()) {
            assertEquals(AgentToolType.BPMN_ACTIVITY, entry.type(),
                    "Non-A2A activity '" + entry.elementId() + "' must have BPMN_ACTIVITY type");
        }
    }

    /**
     * In a mixed scope, the type discrimination is correct for EACH activity:
     * A2A activities get REMOTE_AGENT, non-A2A activities get BPMN_ACTIVITY.
     *
     * <p><b>Validates: Requirements REQ-4</b></p>
     */
    @Property(tries = 100)
    void mixedScope_correctTypeDiscrimination(
            @ForAll("a2aActivitySpecs") List<A2aActivitySpec> a2aSpecs,
            @ForAll("bpmnActivitySpecs") List<BpmnActivitySpec> bpmnSpecs) {

        Assume.that(!a2aSpecs.isEmpty() || !bpmnSpecs.isEmpty());

        ProcessDefinitionImpl procDef = new ProcessDefinitionImpl("proc:test");
        ActivityImpl scope = procDef.createActivity("adHocScope");

        // Add A2A activities
        for (int i = 0; i < a2aSpecs.size(); i++) {
            A2aActivitySpec spec = a2aSpecs.get(i);
            ActivityImpl activity = scope.createActivity("a2a_" + i);
            activity.setProperty("type", "serviceTask");
            activity.setProperty("name", spec.activityName());
            activity.setProperty("documentation", spec.activityDocumentation());
            activity.setProperty("a2aRemoteCallConfig",
                    new A2aRemoteCallConfig(
                            spec.configUrl(), null,
                            spec.configName(), spec.configDescription(),
                            spec.agentRef()));
        }

        // Add non-A2A activities
        for (int i = 0; i < bpmnSpecs.size(); i++) {
            BpmnActivitySpec spec = bpmnSpecs.get(i);
            ActivityImpl activity = scope.createActivity("bpmn_" + i);
            activity.setProperty("type", "serviceTask");
            activity.setProperty("name", spec.name());
            activity.setProperty("documentation", spec.documentation());
        }

        AgentToolCatalogue catalogue = builder.build(scope);

        assertEquals(a2aSpecs.size() + bpmnSpecs.size(), catalogue.tools().size());

        for (AgentToolEntry entry : catalogue.tools()) {
            if (entry.elementId().startsWith("a2a_")) {
                assertEquals(AgentToolType.REMOTE_AGENT, entry.type(),
                        "A2A entry '" + entry.elementId() + "' must be REMOTE_AGENT");
            } else {
                assertEquals(AgentToolType.BPMN_ACTIVITY, entry.type(),
                        "BPMN entry '" + entry.elementId() + "' must be BPMN_ACTIVITY");
            }
        }
    }

    /**
     * For A2A activities: tool name is derived from config name (primary) or
     * activity name (fallback); description from config description (primary) or
     * activity documentation (fallback).
     *
     * <p><b>Validates: Requirements REQ-4</b></p>
     */
    @Property(tries = 100)
    void a2aActivities_nameAndDescriptionDerivedFromConfig(
            @ForAll("a2aActivitySpecs") List<A2aActivitySpec> a2aSpecs) {

        Assume.that(!a2aSpecs.isEmpty());

        ProcessDefinitionImpl procDef = new ProcessDefinitionImpl("proc:test");
        ActivityImpl scope = procDef.createActivity("adHocScope");

        for (int i = 0; i < a2aSpecs.size(); i++) {
            A2aActivitySpec spec = a2aSpecs.get(i);
            ActivityImpl activity = scope.createActivity("a2a_" + i);
            activity.setProperty("type", "serviceTask");
            activity.setProperty("name", spec.activityName());
            activity.setProperty("documentation", spec.activityDocumentation());
            activity.setProperty("a2aRemoteCallConfig",
                    new A2aRemoteCallConfig(
                            spec.configUrl(), null,
                            spec.configName(), spec.configDescription(),
                            spec.agentRef()));
        }

        AgentToolCatalogue catalogue = builder.build(scope);

        for (int i = 0; i < a2aSpecs.size(); i++) {
            A2aActivitySpec spec = a2aSpecs.get(i);
            AgentToolEntry entry = catalogue.tools().get(i);

            // Name: config name if non-blank, else activity name
            String expectedName = (spec.configName() != null && !spec.configName().isBlank())
                    ? spec.configName()
                    : spec.activityName();
            assertEquals(expectedName, entry.name(),
                    "Tool name should come from config name (primary) or activity name (fallback)");

            // Description: config description if non-blank, else activity documentation (stripped)
            String expectedDesc;
            if (spec.configDescription() != null && !spec.configDescription().isBlank()) {
                expectedDesc = spec.configDescription();
            } else {
                String doc = spec.activityDocumentation();
                expectedDesc = (doc == null || doc.isBlank()) ? null : doc.strip();
            }
            assertEquals(expectedDesc, entry.description(),
                    "Tool description should come from config description (primary) or documentation (fallback)");
        }
    }

    /**
     * For non-A2A activities: tool name comes from activity name property;
     * description from activity documentation.
     *
     * <p><b>Validates: Requirements REQ-4</b></p>
     */
    @Property(tries = 100)
    void nonA2aActivities_nameAndDescriptionFromActivityProperties(
            @ForAll("bpmnActivitySpecs") List<BpmnActivitySpec> bpmnSpecs) {

        Assume.that(!bpmnSpecs.isEmpty());

        ProcessDefinitionImpl procDef = new ProcessDefinitionImpl("proc:test");
        ActivityImpl scope = procDef.createActivity("adHocScope");

        for (int i = 0; i < bpmnSpecs.size(); i++) {
            BpmnActivitySpec spec = bpmnSpecs.get(i);
            ActivityImpl activity = scope.createActivity("bpmn_" + i);
            activity.setProperty("type", "serviceTask");
            activity.setProperty("name", spec.name());
            activity.setProperty("documentation", spec.documentation());
        }

        AgentToolCatalogue catalogue = builder.build(scope);

        for (int i = 0; i < bpmnSpecs.size(); i++) {
            BpmnActivitySpec spec = bpmnSpecs.get(i);
            AgentToolEntry entry = catalogue.tools().get(i);

            assertEquals(spec.name(), entry.name(),
                    "Non-A2A tool name should come from activity name property");

            String expectedDesc = (spec.documentation() == null || spec.documentation().isBlank())
                    ? null
                    : spec.documentation().strip();
            assertEquals(expectedDesc, entry.description(),
                    "Non-A2A tool description should come from activity documentation");
        }
    }

    // ========================================================================
    // Generators / Providers
    // ========================================================================

    @Provide
    Arbitrary<List<A2aActivitySpec>> a2aActivitySpecs() {
        return a2aActivitySpec().list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<BpmnActivitySpec>> bpmnActivitySpecs() {
        return bpmnActivitySpec().list().ofMinSize(1).ofMaxSize(5);
    }

    private Arbitrary<A2aActivitySpec> a2aActivitySpec() {
        Arbitrary<String> configNames = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.of("", " ", null)
        );
        Arbitrary<String> configDescriptions = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.of("", " ", null)
        );
        Arbitrary<String> activityNames = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.just(null)
        );
        Arbitrary<String> activityDocs = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.of("", " ", null)
        );
        Arbitrary<String> urls = nonBlankStrings();
        Arbitrary<String> agentRefs = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.just(null)
        );

        return Combinators.combine(configNames, configDescriptions, activityNames, activityDocs, urls, agentRefs)
                .as(A2aActivitySpec::new);
    }

    private Arbitrary<BpmnActivitySpec> bpmnActivitySpec() {
        Arbitrary<String> names = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.just(null)
        );
        Arbitrary<String> docs = Arbitraries.oneOf(
                nonBlankStrings(),
                Arbitraries.of("", " ", null)
        );

        return Combinators.combine(names, docs).as(BpmnActivitySpec::new);
    }

    private Arbitrary<String> nonBlankStrings() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(50)
                .alpha()
                .numeric()
                .withChars(' ', '-', '_', '.')
                .filter(s -> !s.isBlank());
    }

    // ========================================================================
    // Value Objects for Test Data
    // ========================================================================

    record A2aActivitySpec(
            String configName,
            String configDescription,
            String activityName,
            String activityDocumentation,
            String configUrl,
            String agentRef
    ) {}

    record BpmnActivitySpec(
            String name,
            String documentation
    ) {}
}
