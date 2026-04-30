package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ActivityTypes;
import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdHocSubProcessCatalogueBuilder implements AgentToolCatalogueBuilder {

    private static final Namespace CAMUNDA_NS = new Namespace("http://camunda.org/schema/1.0/bpmn");

    private static final Set<String> ACTIVITY_TAG_NAMES = Set.of(
            ActivityTypes.TASK,
            ActivityTypes.TASK_SERVICE,
            ActivityTypes.TASK_SEND_TASK,
            ActivityTypes.TASK_RECEIVE_TASK,
            ActivityTypes.TASK_USER_TASK,
            ActivityTypes.TASK_MANUAL_TASK,
            ActivityTypes.TASK_BUSINESS_RULE,
            ActivityTypes.TASK_SCRIPT,
            ActivityTypes.SUB_PROCESS,
            ActivityTypes.CALL_ACTIVITY,
            ActivityTypes.SUB_PROCESS_AD_HOC
    );

    private static final Pattern SIMPLE_EL = Pattern.compile(
            "^\\s*\\$\\{\\s*([a-zA-Z_]\\w*)(?:\\.[a-zA-Z_]\\w*)*\\s*\\}\\s*$");

    @Override
    public AgentToolCatalogue build(Element scopeElement, String processDefinitionId) {
        List<AgentToolEntry> tools = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> sequenceFlowTargets = collectSequenceFlowTargets(scopeElement);

        for (Element child : scopeElement.elements()) {
            if (!isActivityElement(child)) continue;
            String id = child.attribute("id");
            if (id == null || id.isBlank()) {
                errors.add("Activity element with missing id in scope '" + scopeElement.attribute("id") + "'");
                continue;
            }
            if (sequenceFlowTargets.contains(id)) continue;

            String name = child.attribute("name");
            String description = extractDocumentation(child);
            Set<String> reads = extractReads(child);
            Set<String> writes = extractWrites(child);

            tools.add(new AgentToolEntry(id, name, description, reads, writes));
        }

        if (!errors.isEmpty()) {
            throw new ProcessEngineException(
                    "Invalid tool configuration in scope '" + scopeElement.attribute("id")
                            + "' for process definition '" + processDefinitionId + "': "
                            + String.join("; ", errors));
        }

        return new AgentToolCatalogue(processDefinitionId, scopeElement.attribute("id"), tools);
    }

    private Set<String> collectSequenceFlowTargets(Element scopeElement) {
        Set<String> targets = new HashSet<>();
        for (Element sequenceFlow : scopeElement.elements("sequenceFlow")) {
            String targetRef = sequenceFlow.attribute("targetRef");
            if (targetRef != null) {
                targets.add(targetRef);
            }
        }
        return targets;
    }

    private boolean isActivityElement(Element element) {
        return ACTIVITY_TAG_NAMES.contains(element.getTagName());
    }

    private String extractDocumentation(Element element) {
        Element doc = element.element("documentation");
        if (doc == null) return null;
        String text = doc.getText();
        return (text == null || text.isBlank()) ? null : text.strip();
    }

    private Set<String> extractReads(Element element) {
        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null) return Set.of();
        Element inputOutputElement = extensionElements.elementNS(CAMUNDA_NS, "inputOutput");
        if (inputOutputElement == null) return Set.of();

        Set<String> reads = new LinkedHashSet<>();
        for (Element inputParam : inputOutputElement.elementsNS(CAMUNDA_NS, "inputParameter")) {
            walk(inputParam, reads);
        }
        return reads;
    }

    private Set<String> extractWrites(Element element) {
        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null) return Set.of();
        Element inputOutputElement = extensionElements.elementNS(CAMUNDA_NS, "inputOutput");
        if (inputOutputElement == null) return Set.of();

        Set<String> writes = new LinkedHashSet<>();
        for (Element outputParam : inputOutputElement.elementsNS(CAMUNDA_NS, "outputParameter")) {
            String name = outputParam.attribute("name");
            if (name != null) {
                writes.add(name);
            }
        }
        return writes;
    }

    static Optional<String> scopeReadFor(String expression) {
        if (expression == null) return Optional.empty();
        Matcher matcher = SIMPLE_EL.matcher(expression);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static void walk(Element node, Set<String> out) {
        if (isFluxnovaScript(node)) return;
        if (isFluxnovaList(node)) {
            for (Element value : node.elementsNS(CAMUNDA_NS, "value")) {
                walk(value, out);
            }
            return;
        }
        if (isFluxnovaMap(node)) {
            for (Element entry : node.elementsNS(CAMUNDA_NS, "entry")) {
                walk(entry, out);
            }
            return;
        }
        // Check for composite children (list, map, script) before falling through to text
        for (Element child : node.elements()) {
            if (isFluxnovaList(child) || isFluxnovaMap(child) || isFluxnovaScript(child)) {
                walk(child, out);
                return;
            }
        }
        scopeReadFor(node.getText()).ifPresent(out::add);
    }

    private static boolean isFluxnovaScript(Element node) {
        return "script".equals(node.getTagName())
                && CAMUNDA_NS.getNamespaceUri().equals(node.getUri());
    }

    private static boolean isFluxnovaList(Element node) {
        return "list".equals(node.getTagName())
                && CAMUNDA_NS.getNamespaceUri().equals(node.getUri());
    }

    private static boolean isFluxnovaMap(Element node) {
        return "map".equals(node.getTagName())
                && CAMUNDA_NS.getNamespaceUri().equals(node.getUri());
    }
}
