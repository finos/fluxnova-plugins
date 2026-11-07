package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolParameter;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessValidationHelper;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.IoMapping;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.IoParameter;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ListValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.MapValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.finos.fluxnova.bpm.engine.impl.el.ElValueProvider;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.scripting.ScriptValueProvider;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdHocSubProcessCatalogueBuilder implements AgentToolCatalogueBuilder {

    private static final Pattern SIMPLE_EL =
            Pattern.compile("^\\s*\\$\\{\\s*([a-zA-Z_]\\w*)(?:\\.[a-zA-Z_]\\w*)*\\s*\\}\\s*$");

    private static final Pattern AGENT_ARG_CALL = Pattern.compile(
            "agent:arg\\s*\\(\\s*(['\"])((?:\\\\.|(?!\\1).)*)\\1\\s*,\\s*(['\"])((?:\\\\.|(?!\\3).)*)\\3\\s*\\)");

    @Override
    public AgentToolCatalogue build(ActivityImpl scope) {
        List<AgentToolEntry> tools = scope.getActivities().stream()
                .filter(a -> AdHocSubProcessValidationHelper
                        .isStartableActivityInAdHocScope(scope, a))
                .map(a -> new AgentToolEntry(
                        a.getId(),
                        (String) a.getProperty("name"),
                        extractDocumentation(a),
                        extractReads(a),
                        extractWrites(a),
                        extractParameters(a)))
                .toList();

        return new AgentToolCatalogue(
                scope.getProcessDefinition().getId(),
                scope.getId(),
                tools);
    }

    private String extractDocumentation(ActivityImpl activity) {
        String doc = (String) activity.getProperty("documentation");
        return (doc == null || doc.isBlank()) ? null : doc.strip();
    }

    private Set<String> extractReads(ActivityImpl activity) {
        IoMapping mapping = activity.getIoMapping();
        if (mapping == null) return Set.of();
        return mapping.getInputParameters().stream()
                .flatMap(p -> extractReadsFromValueProvider(p.getValueProvider()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> extractWrites(ActivityImpl activity) {
        IoMapping mapping = activity.getIoMapping();
        if (mapping == null) return Set.of();
        return mapping.getOutputParameters().stream()
                .map(IoParameter::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<AgentToolParameter> extractParameters(ActivityImpl activity) {
        IoMapping mapping = activity.getIoMapping();
        if (mapping == null) return List.of();
        Map<String, AgentToolParameter> byName = new LinkedHashMap<>();
        for (IoParameter inputParam : mapping.getInputParameters()) {
            for (AgentToolParameter param : extractParametersFromValueProvider(inputParam.getValueProvider())) {
                byName.putIfAbsent(param.name(), param);
            }
        }
        return List.copyOf(byName.values());
    }

    static Optional<String> scopeReadFor(String expression) {
        if (expression == null)
            return Optional.empty();
        Matcher matcher = SIMPLE_EL.matcher(expression);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static List<AgentToolParameter> agentArgsFor(String expression) {
        if (expression == null) {
            return List.of();
        }
        List<AgentToolParameter> result = new ArrayList<>();
        Matcher matcher = AGENT_ARG_CALL.matcher(expression);
        while (matcher.find()) {
            String name = unescapeLiteral(matcher.group(2)).trim();
            String description = unescapeLiteral(matcher.group(4)).trim();
            if (!name.isEmpty()) {
                result.add(new AgentToolParameter(name, description));
            }
        }
        return result;
    }

    private static String unescapeLiteral(String raw) {
        if (raw == null) return "";
        return raw.replace("\\'", "'").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    static Set<String> extractReadsFromValueProvider(ParameterValueProvider provider) {
        if (provider instanceof ScriptValueProvider) {
            return Set.of();
        }
        if (provider instanceof ListValueProvider listValueProvider) {
            return listValueProvider.getProviderList().stream()
                    .flatMap(p -> extractReadsFromValueProvider(p).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (provider instanceof MapValueProvider mapValueProvider) {
            return mapValueProvider.getProviderMap().values().stream()
                    .flatMap(p -> extractReadsFromValueProvider(p).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (provider instanceof ElValueProvider elValueProvider) {
            return scopeReadFor(elValueProvider.getExpression().getExpressionText())
                    .map(Set::of).orElse(Set.of());
        }
        return Set.of();
    }

    static List<AgentToolParameter> extractParametersFromValueProvider(ParameterValueProvider provider) {
        if (provider instanceof ScriptValueProvider) {
            return List.of();
        }
        if (provider instanceof ListValueProvider listValueProvider) {
            return listValueProvider.getProviderList().stream()
                    .flatMap(p -> extractParametersFromValueProvider(p).stream())
                    .toList();
        }
        if (provider instanceof MapValueProvider mapValueProvider) {
            return mapValueProvider.getProviderMap().values().stream()
                    .flatMap(p -> extractParametersFromValueProvider(p).stream())
                    .toList();
        }
        if (provider instanceof ElValueProvider elValueProvider) {
            return agentArgsFor(elValueProvider.getExpression().getExpressionText());
        }
        return List.of();
    }
}
