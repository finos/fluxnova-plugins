package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessValidationHelper;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdHocSubProcessCatalogueBuilder implements AgentToolCatalogueBuilder {

    private static final Namespace CAMUNDA_NS = new Namespace("http://camunda.org/schema/1.0/bpmn");

    private static final Pattern SIMPLE_EL =
            Pattern.compile("^\\s*\\$\\{\\s*([a-zA-Z_]\\w*)(?:\\.[a-zA-Z_]\\w*)*\\s*\\}\\s*$");

    @Override
    public AgentToolCatalogue build(Element scopeElement, String processDefinitionId) {
        Set<String> sequenceFlowTargets = collectSequenceFlowTargets(scopeElement);

        List<Element> activityElements =
                scopeElement.elements().stream().filter(this::isActivityElement).toList();

        List<String> errors = activityElements.stream()
                .filter(child -> child.attribute("id") == null || child.attribute("id").isBlank())
                .map(child -> "Activity element with missing id in scope '"
                        + scopeElement.attribute("id") + "'")
                .toList();

        if (!errors.isEmpty()) {
            throw new ProcessEngineException("Invalid tool configuration in scope '"
                    + scopeElement.attribute("id") + "' for process definition '"
                    + processDefinitionId + "': " + String.join("; ", errors));
        }

        List<AgentToolEntry> tools = activityElements.stream()
                .filter(child -> !sequenceFlowTargets.contains(child.attribute("id")))
                .map(child -> new AgentToolEntry(child.attribute("id"), child.attribute("name"),
                        extractDocumentation(child), extractReads(child), extractWrites(child)))
                .toList();

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
        String tagName = element.getTagName();
        // sequenceFlow is a connection element, not an activity
        if ("sequenceFlow".equals(tagName)) {
            return false;
        }
        // Raw <endEvent> maps to "noneEndEvent" internally; the XML tag name
        // "endEvent" is not in isNonStartableActivityType's list so must be handled explicitly
        if ("endEvent".equals(tagName)) {
            return false;
        }
        return !AdHocSubProcessValidationHelper.isNonStartableActivityType(tagName);
    }

    private String extractDocumentation(Element element) {
        Element doc = element.element("documentation");
        if (doc == null)
            return null;
        String text = doc.getText();
        return (text == null || text.isBlank()) ? null : text.strip();
    }

    private Set<String> extractReads(Element element) {
        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null)
            return Set.of();
        Element inputOutputElement = extensionElements.elementNS(CAMUNDA_NS, "inputOutput");
        if (inputOutputElement == null)
            return Set.of();

        return inputOutputElement.elementsNS(CAMUNDA_NS, "inputParameter").stream()
                .flatMap(inputParam -> walk(inputParam).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> extractWrites(Element element) {
        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null)
            return Set.of();
        Element inputOutputElement = extensionElements.elementNS(CAMUNDA_NS, "inputOutput");
        if (inputOutputElement == null)
            return Set.of();

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
        if (expression == null)
            return Optional.empty();
        Matcher matcher = SIMPLE_EL.matcher(expression);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Set<String> walk(Element node) {
        if (isFluxnovaScript(node))
            return Set.of();
        if (isFluxnovaList(node)) {
            return node.elementsNS(CAMUNDA_NS, "value").stream()
                    .flatMap(value -> walk(value).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (isFluxnovaMap(node)) {
            return node.elementsNS(CAMUNDA_NS, "entry").stream()
                    .flatMap(entry -> walk(entry).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return node.elements().stream()
                .filter(child -> isFluxnovaList(child) || isFluxnovaMap(child)
                        || isFluxnovaScript(child))
                .findFirst().map(AdHocSubProcessCatalogueBuilder::walk)
                .orElseGet(() -> scopeReadFor(node.getText()).map(Set::of).orElse(Set.of()));
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
