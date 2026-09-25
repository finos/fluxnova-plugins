package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.a2a.discovery.AgentCardCache;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentCard;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentSkill;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolType;
import org.finos.fluxnova.bpm.engine.impl.bpmn.behavior.AdHocSubProcessValidationHelper;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.IoMapping;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.IoParameter;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ListValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.MapValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.finos.fluxnova.bpm.engine.impl.el.ElValueProvider;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.scripting.ScriptValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdHocSubProcessCatalogueBuilder implements AgentToolCatalogueBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(AdHocSubProcessCatalogueBuilder.class);

    private static final Pattern SIMPLE_EL =
            Pattern.compile("^\\s*\\$\\{\\s*([a-zA-Z_]\\w*)(?:\\.[a-zA-Z_]\\w*)*\\s*\\}\\s*$");

    private static final String A2A_CONFIG_PROPERTY_KEY = "a2aRemoteCallConfig";

    private final AgentCardCache agentCardCache;

    public AdHocSubProcessCatalogueBuilder(AgentCardCache agentCardCache) {
        this.agentCardCache = agentCardCache;
    }

    /** Backward-compatible constructor (no agent card enrichment). */
    public AdHocSubProcessCatalogueBuilder() {
        this(null);
    }

    @Override
    public AgentToolCatalogue build(ActivityImpl scope) {
        List<AgentToolEntry> tools = scope.getActivities().stream()
                .filter(a -> AdHocSubProcessValidationHelper
                        .isStartableActivityInAdHocScope(scope, a))
                .map(this::buildToolEntry)
                .toList();

        return new AgentToolCatalogue(
                scope.getProcessDefinition().getId(),
                scope.getId(),
                tools);
    }

    private AgentToolEntry buildToolEntry(ActivityImpl activity) {
        A2aRemoteCallConfig a2aConfig = (A2aRemoteCallConfig) activity.getProperty(A2A_CONFIG_PROPERTY_KEY);

        if (a2aConfig != null) {
            String name = resolveA2aName(a2aConfig, activity);
            String description = resolveA2aDescription(a2aConfig, activity);
            return new AgentToolEntry(
                    activity.getId(),
                    name,
                    description,
                    extractReads(activity),
                    extractWrites(activity),
                    AgentToolType.REMOTE_AGENT);
        }

        return new AgentToolEntry(
                activity.getId(),
                (String) activity.getProperty("name"),
                extractDocumentation(activity),
                extractReads(activity),
                extractWrites(activity),
                AgentToolType.BPMN_ACTIVITY);
    }

    private String resolveA2aName(A2aRemoteCallConfig config, ActivityImpl activity) {
        String configName = config.name();
        if (configName != null && !configName.isBlank()) {
            return configName;
        }
        // Fall back to agent card name if available
        Optional<AgentCard> card = fetchAgentCard(config);
        if (card.isPresent() && card.get().name() != null && !card.get().name().isBlank()) {
            return card.get().name();
        }
        return (String) activity.getProperty("name");
    }

    private String resolveA2aDescription(A2aRemoteCallConfig config, ActivityImpl activity) {
        Optional<AgentCard> card = fetchAgentCard(config);

        // Resolve the base description: config wins, then agent card, then BPMN documentation.
        String baseDescription = config.description();
        if (baseDescription == null || baseDescription.isBlank()) {
            if (card.isPresent() && card.get().description() != null
                    && !card.get().description().isBlank()) {
                baseDescription = card.get().description();
            } else {
                baseDescription = extractDocumentation(activity);
            }
        }

        // Append the agent card's advertised skills so the LLM can make capability-based
        // agent-selection decisions. Skills supplement whatever base description was chosen.
        String skillsText = card.map(c -> formatSkills(c.skills())).orElse(null);
        if (skillsText == null || skillsText.isBlank()) {
            return baseDescription;
        }
        if (baseDescription == null || baseDescription.isBlank()) {
            return skillsText;
        }
        return baseDescription.trim() + "\n" + skillsText;
    }

    /**
     * Formats a remote agent's advertised skills into a compact, human-readable block
     * suitable for inclusion in the tool description presented to the LLM.
     *
     * <p>Returns {@code null} if there are no usable skills.
     */
    private String formatSkills(List<AgentSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Skills:");
        for (AgentSkill skill : skills) {
            String name = skill.name();
            String description = skill.description();
            boolean hasName = name != null && !name.isBlank();
            boolean hasDescription = description != null && !description.isBlank();
            if (!hasName && !hasDescription) {
                continue;
            }
            sb.append("\n  - ");
            if (hasName) {
                sb.append(name.trim());
                if (hasDescription) {
                    sb.append(": ").append(description.trim());
                }
            } else {
                sb.append(description.trim());
            }
        }
        // If every skill was blank, the header is meaningless.
        return sb.indexOf("\n") == -1 ? null : sb.toString();
    }

    /**
     * Attempts to fetch the agent card for a remote A2A agent using the configured
     * base URL. Returns empty if no {@link AgentCardCache} is available or the URL
     * contains unresolved EL expressions.
     */
    private Optional<AgentCard> fetchAgentCard(A2aRemoteCallConfig config) {
        if (agentCardCache == null || config.url() == null) {
            return Optional.empty();
        }
        String url = config.url();
        // Skip EL expressions — they can only be resolved at runtime
        if (url.contains("${") || url.contains("#{")) {
            LOG.debug("Skipping agent card fetch for EL expression URL '{}'", url);
            return Optional.empty();
        }
        return agentCardCache.getOrFetch(url, config.agentRef());
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

    static Optional<String> scopeReadFor(String expression) {
        if (expression == null)
            return Optional.empty();
        Matcher matcher = SIMPLE_EL.matcher(expression);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
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
}
