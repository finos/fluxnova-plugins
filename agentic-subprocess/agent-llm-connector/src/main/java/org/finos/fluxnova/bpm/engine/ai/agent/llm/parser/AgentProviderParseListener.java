package org.finos.fluxnova.bpm.engine.ai.agent.llm.parser;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigElementWalker;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants.AGENT_NS;

public class AgentProviderParseListener extends AbstractBpmnParseListener {

    private static final Logger LOG = LoggerFactory.getLogger(AgentProviderParseListener.class);

    private final AgentProviderRegistry registry;
    private final AgentConfigElementWalker walker;

    public AgentProviderParseListener(AgentProviderRegistry registry) {
        this(registry, new AgentConfigElementWalker());
    }

    AgentProviderParseListener(AgentProviderRegistry registry, AgentConfigElementWalker walker) {
        this.registry = registry;
        this.walker = walker;
    }

    @Override
    public void parseRootElement(Element rootElement, List<ProcessDefinitionEntity> processDefinitions) {
        List<String> errors = new ArrayList<>();
        Element firstOffending = null;

        // Attempt to resolve the provider registry. If the Spring context hasn't fully
        // initialised yet (e.g. ChatModel beans can't be instantiated because credentials
        // aren't ready), skip validation entirely and defer to runtime.
        boolean registryAvailable;
        try {
            registryAvailable = registry.has("__probe__") || true; // Force supplier evaluation
        } catch (Exception e) {
            // Provider registry is not yet available (e.g. ChatModel bean creation failed
            // because JWT token not yet fetched, or network issue during Spring AI startup).
            // Skip deploy-time validation — the provider will be validated at runtime when
            // the agent is actually invoked.
            LOG.debug("Provider registry not available during BPMN parse, deferring validation: {}",
                    e.getMessage());
            return;
        }

        for (Element process : rootElement.elements("process")) {
            for (Element element : walker.walk(process)) {
                Element ext = element.element("extensionElements");
                if (ext == null) {
                    continue;
                }
                Element config = ext.elementNS(AGENT_NS, "config");
                if (config == null) {
                    continue;
                }
                String provider = config.attribute("provider");
                if (provider == null || provider.isBlank()) {
                    continue;
                }
                try {
                    if (!registry.has(provider)) {
                        if (firstOffending == null) {
                            firstOffending = element;
                        }
                        String elementId = element.attribute("id");
                        errors.add("element '" + elementId + "' references unavailable provider '" + provider + "'");
                    }
                } catch (Exception e) {
                    // If registry.has() fails for a specific provider (e.g. bean instantiation
                    // failure), skip validation for that provider — it will fail at runtime
                    // with a clear error message when the agent actually tries to use it.
                    LOG.debug("Provider '{}' check failed during BPMN parse, deferring to runtime: {}",
                            provider, e.getMessage());
                }
            }
        }

        if (!errors.isEmpty()) {
            StringJoiner joiner = new StringJoiner("; ");
            errors.forEach(joiner::add);
            throw new BpmnParseException(
                    "Invalid provider reference(s) in agent:config: " + joiner,
                    firstOffending);
        }
    }
}
